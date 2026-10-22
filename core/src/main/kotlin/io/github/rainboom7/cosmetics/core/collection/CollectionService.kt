package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ExpiryStatus
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/** Business rules on top of [CollectionRepository]. ViewModels talk to this, not to the repository. */
class CollectionService(
    private val repository: CollectionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun observe(status: ItemStatus): Flow<List<CosmeticItem>> = repository.observeByStatus(status)

    fun observeAll(): Flow<List<CosmeticItem>> = repository.observeAll()

    suspend fun get(id: Long): CosmeticItem? = repository.get(id)

    fun observeItem(id: Long): Flow<CosmeticItem?> = repository.observeItem(id)

    fun observeSummary(): Flow<CollectionSummary> = repository.observeAll().map { summarize(it, today()) }

    /** Barcodes per list, for the "Уже в коллекции" hint in catalog results. */
    fun observeKnownBarcodes(): Flow<KnownBarcodes> = repository.observeAll().map { items ->
        KnownBarcodes(
            owned = items.filter { it.status == ItemStatus.OWNED }.mapNotNullTo(HashSet()) { it.barcode },
            wishlist = items.filter { it.status == ItemStatus.WISHLIST }.mapNotNullTo(HashSet()) { it.barcode },
        )
    }

    /**
     * Adds a catalog product. Products are matched by barcode: adding something that is already in
     * the target list returns the existing id, and adding a wished product as owned moves it from
     * the wishlist instead of duplicating it. [photoUris] are own photos, e.g. from photo search.
     */
    suspend fun addFromCatalog(
        product: CatalogProduct,
        status: ItemStatus,
        photoUris: List<String> = emptyList(),
    ): AddResult {
        val sameBarcode = product.barcode?.let { repository.findByBarcode(it) }.orEmpty()
        sameBarcode.firstOrNull { it.status == status }?.let { return AddResult.AlreadyThere(it.id) }
        if (status == ItemStatus.OWNED) {
            sameBarcode.firstOrNull { it.status == ItemStatus.WISHLIST }?.let { wished ->
                markBought(wished.id)
                return AddResult.MovedFromWishlist(wished.id)
            }
        }
        return AddResult.Added(repository.upsert(product.toItem(status, clock.millis(), photoUris)))
    }

    /**
     * Saves a manually entered or edited item, stamping the add time on new ones.
     * @throws IllegalArgumentException for a new item without any image: the design requires a photo.
     */
    suspend fun save(item: CosmeticItem): Long {
        if (item.id == 0L) {
            require(item.hasImage) { "a new item needs a photo" }
            return repository.upsert(item.copy(addedAtMillis = clock.millis()))
        }
        return repository.upsert(item)
    }

    /** Puts back an item exactly as it was, e.g. for Snackbar "Отменить". */
    suspend fun restore(item: CosmeticItem) {
        repository.upsert(item)
    }

    /**
     * "Куплено": the wishlist item moves to the collection as a fresh, unopened item.
     * Returns the item as it was before, for undo.
     */
    suspend fun markBought(id: Long): CosmeticItem? {
        val item = repository.get(id) ?: return null
        if (item.status == ItemStatus.OWNED) return null
        repository.upsert(
            item.copy(
                status = ItemStatus.OWNED,
                usage = UsageState.NEW,
                remainingPercent = 100,
                openedOn = null,
                wantBadly = false,
                rebuy = false,
                shelfStatus = null,
                finishedOn = null,
                addedAtMillis = clock.millis(),
            ),
        )
        return item
    }

    /**
     * "Купить снова": puts a copy of an owned item on the wishlist, tagged [CosmeticItem.rebuy].
     * Returns the wishlist item's id; an existing wishlist entry with the same barcode is reused.
     */
    suspend fun buyAgain(id: Long): Long? {
        val item = repository.get(id) ?: return null
        item.barcode?.let { code ->
            repository.findByBarcode(code).firstOrNull { it.status == ItemStatus.WISHLIST }?.let { return it.id }
        }
        return repository.upsert(
            item.copy(
                id = 0,
                status = ItemStatus.WISHLIST,
                usage = UsageState.NEW,
                remainingPercent = 100,
                openedOn = null,
                notes = "",
                rebuy = true,
                shelfStatus = null,
                favorite = false,
                finishedOn = null,
                addedAtMillis = clock.millis(),
            ),
        )
    }

    /**
     * Changes the usage state; opening an item for the first time starts its PAO countdown on
     * [openedOn] (today by default). Finishing stamps [CosmeticItem.finishedOn] and drops a
     * "running low" mark, which no longer means anything for an empty jar.
     */
    suspend fun setUsage(id: Long, usage: UsageState, openedOn: LocalDate = today()) {
        val item = repository.get(id) ?: return
        val opened = when {
            usage == UsageState.NEW -> null
            item.openedOn == null -> openedOn
            else -> item.openedOn
        }
        val finished = UsageState.FINISHED
        repository.upsert(
            item.copy(
                usage = usage,
                openedOn = opened,
                remainingPercent = if (usage == finished) 0 else item.remainingPercent.coerceAtLeast(1),
                finishedOn = if (usage == finished) item.finishedOn ?: today() else null,
                shelfStatus = if (usage == finished && item.shelfStatus == ShelfStatus.RUNNING_LOW) null else item.shelfStatus,
            ),
        )
    }

    /** Sets or clears (null) the user's status mark; tapping the selected chip again clears it. */
    suspend fun setShelfStatus(id: Long, shelfStatus: ShelfStatus?) {
        val item = repository.get(id) ?: return
        repository.upsert(item.copy(shelfStatus = shelfStatus))
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        val item = repository.get(id) ?: return
        repository.upsert(item.copy(favorite = favorite))
    }

    suspend fun setRemaining(id: Long, percent: Int) {
        val item = repository.get(id) ?: return
        repository.upsert(item.copy(remainingPercent = percent.coerceIn(0, 100)))
    }

    suspend fun addPhoto(id: Long, uri: String) {
        val item = repository.get(id) ?: return
        if (uri in item.photoUris) return
        repository.upsert(item.copy(photoUris = item.photoUris + uri))
    }

    suspend fun setWantBadly(id: Long, wantBadly: Boolean) {
        val item = repository.get(id) ?: return
        repository.upsert(item.copy(wantBadly = wantBadly))
    }

    /** Deletes an item, returning it for undo. */
    suspend fun delete(id: Long): CosmeticItem? {
        val item = repository.get(id) ?: return null
        repository.delete(id)
        return item
    }

    fun today(): LocalDate = LocalDate.now(clock)

    sealed interface AddResult {
        val id: Long

        data class Added(override val id: Long) : AddResult
        data class AlreadyThere(override val id: Long) : AddResult
        data class MovedFromWishlist(override val id: Long) : AddResult
    }

    companion object {
        fun summarize(items: List<CosmeticItem>, today: LocalDate): CollectionSummary {
            val owned = items.filter { it.status == ItemStatus.OWNED && it.usage != UsageState.FINISHED }
            val expiry = owned.groupingBy { it.expiryStatus(today) }.eachCount()
            return CollectionSummary(
                ownedCount = items.count { it.status == ItemStatus.OWNED },
                wishlistCount = items.count { it.status == ItemStatus.WISHLIST },
                expiringSoonCount = expiry[ExpiryStatus.SOON] ?: 0,
                expiredCount = expiry[ExpiryStatus.EXPIRED] ?: 0,
                shelfCounts = owned.mapNotNull { it.effectiveShelfStatus() }.groupingBy { it }.eachCount(),
                favoriteCount = owned.count { it.favorite },
                finishedThisYear = items.count { it.status == ItemStatus.OWNED && it.finishedOn?.year == today.year },
            )
        }
    }
}

data class CollectionSummary(
    val ownedCount: Int,
    val wishlistCount: Int,
    val expiringSoonCount: Int,
    val expiredCount: Int,
    /** Items in use per status chip, counting the automatic "Заканчивается". */
    val shelfCounts: Map<ShelfStatus, Int> = emptyMap(),
    val favoriteCount: Int = 0,
    /** "Пустые баночки": items used up this calendar year. */
    val finishedThisYear: Int = 0,
) {
    companion object {
        val EMPTY = CollectionSummary(0, 0, 0, 0)
    }
}

data class KnownBarcodes(val owned: Set<String> = emptySet(), val wishlist: Set<String> = emptySet())

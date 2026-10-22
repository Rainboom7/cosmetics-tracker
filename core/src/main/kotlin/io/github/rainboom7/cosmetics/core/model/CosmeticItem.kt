package io.github.rainboom7.cosmetics.core.model

import java.time.LocalDate

/** Which list an item is on. */
enum class ItemStatus {
    /** The user has it ("Коллекция"). */
    OWNED,

    /** The user wants it ("Желаемое"). */
    WISHLIST,
}

/** Lifecycle of an owned item ("Мой экземпляр" segmented button). */
enum class UsageState { NEW, OPENED, FINISHED }

/**
 * How the user feels about an owned item, picked by hand on the "Статус" chips: plenty in stock,
 * running low, trying it out, not going to use it, giving it away. Unlike [UsageState] it does not
 * drive the PAO countdown; it is for sorting the shelf and deciding what to buy or declutter.
 */
enum class ShelfStatus { STOCKED, RUNNING_LOW, TRYING, NOT_USING, GIVE_AWAY }

/**
 * The one status the user picks for an owned item ("Состояние" chips): the lifecycle
 * ([UsageState]) and the shelf mark ([ShelfStatus]) folded into a single choice, so the card shows
 * one question instead of two overlapping ones. See [CosmeticItem.condition] and
 * `CollectionService.setCondition` for the mapping.
 */
enum class Condition { NEW, OPENED, TRYING, STOCKED, RUNNING_LOW, NOT_USING, GIVE_AWAY, FINISHED }

/**
 * A product in the user's collection or wishlist. One table holds both lists; [status] tells
 * which. Owned-only fields ([usage], [remainingPercent], [openedOn]) are ignored for wishlist
 * items, wishlist-only fields ([wantBadly], [rebuy], [price], [link]) for owned ones.
 * [catalogSource] null means the item was entered manually.
 *
 * Images: [catalogImageUrl] comes from the catalog named by [catalogSource] and must be shown
 * with attribution; [photoUris] are the user's own photos (local `file://`/`content://` URIs).
 * A manually added item must have at least one own photo, see [hasImage].
 *
 * [openedOn] + [periodAfterOpeningMonths] (the "PAO" jar symbol, e.g. 12M) give [expiresOn].
 * [shelfStatus] and [favorite] are the user's own marks on owned items; [finishedOn] is stamped when
 * an item is used up, for the "Пустые баночки" counter.
 */
data class CosmeticItem(
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val category: Category = Category.OTHER,
    val status: ItemStatus = ItemStatus.OWNED,
    val barcode: String? = null,
    val shade: String? = null,
    val quantity: String? = null,
    val ingredients: String? = null,
    val catalogImageUrl: String? = null,
    val catalogSource: String? = null,
    val photoUris: List<String> = emptyList(),
    val notes: String = "",
    val usage: UsageState = UsageState.NEW,
    val remainingPercent: Int = 100,
    val openedOn: LocalDate? = null,
    val periodAfterOpeningMonths: Int? = null,
    val wantBadly: Boolean = false,
    val rebuy: Boolean = false,
    val price: String? = null,
    val link: String? = null,
    val addedAtMillis: Long = 0,
    val shelfStatus: ShelfStatus? = null,
    val favorite: Boolean = false,
    val finishedOn: LocalDate? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(remainingPercent in 0..100) { "remainingPercent must be in 0..100, was $remainingPercent" }
        require(periodAfterOpeningMonths == null || periodAfterOpeningMonths > 0) {
            "periodAfterOpeningMonths must be positive, was $periodAfterOpeningMonths"
        }
    }

    /** All images for the pager: the catalog photo first, then the user's own. */
    val images: List<String> get() = listOfNotNull(catalogImageUrl) + photoUris

    /** The image for list cards: the user's own photo of their item wins over the catalog one. */
    val coverImage: String? get() = photoUris.firstOrNull() ?: catalogImageUrl

    val hasImage: Boolean get() = coverImage != null

    /** The day the product should be thrown away, or null when unknown. */
    fun expiresOn(): LocalDate? {
        val opened = openedOn ?: return null
        val months = periodAfterOpeningMonths ?: return null
        return opened.plusMonths(months.toLong())
    }

    fun expiryStatus(today: LocalDate, soonDays: Long = EXPIRY_SOON_DAYS): ExpiryStatus {
        val expires = expiresOn() ?: return ExpiryStatus.UNKNOWN
        return when {
            today.isAfter(expires) -> ExpiryStatus.EXPIRED
            !today.plusDays(soonDays).isBefore(expires) -> ExpiryStatus.SOON
            else -> ExpiryStatus.OK
        }
    }

    /**
     * The status shown on the card: the user's choice, or "Заканчивается" worked out from the
     * remaining slider when the user has not picked one. Null for wishlist and finished items.
     */
    fun effectiveShelfStatus(): ShelfStatus? = when {
        status != ItemStatus.OWNED || usage == UsageState.FINISHED -> null
        shelfStatus != null -> shelfStatus
        usage == UsageState.OPENED && remainingPercent <= RUNNING_LOW_PERCENT -> ShelfStatus.RUNNING_LOW
        else -> null
    }

    /**
     * The single status picked for an owned item, see [Condition]. Null for wishlist items. Uses the
     * hand-set mark only, so the chips follow taps; the slider's automatic "Заканчивается" shows as
     * the card badge and in the collection filter instead.
     */
    fun condition(): Condition? {
        if (status != ItemStatus.OWNED) return null
        if (usage == UsageState.FINISHED) return Condition.FINISHED
        return when (shelfStatus) {
            ShelfStatus.STOCKED -> Condition.STOCKED
            ShelfStatus.RUNNING_LOW -> Condition.RUNNING_LOW
            ShelfStatus.TRYING -> Condition.TRYING
            ShelfStatus.NOT_USING -> Condition.NOT_USING
            ShelfStatus.GIVE_AWAY -> Condition.GIVE_AWAY
            null -> if (usage == UsageState.NEW) Condition.NEW else Condition.OPENED
        }
    }

    /** The warning badge on a collection card, if any. Expiry is more urgent than running low. */
    fun badge(today: LocalDate): ItemBadge? = when {
        status != ItemStatus.OWNED || usage == UsageState.FINISHED -> null
        expiryStatus(today) == ExpiryStatus.SOON || expiryStatus(today) == ExpiryStatus.EXPIRED -> ItemBadge.EXPIRING
        effectiveShelfStatus() == ShelfStatus.RUNNING_LOW -> ItemBadge.RUNNING_LOW
        else -> null
    }

    companion object {
        const val EXPIRY_SOON_DAYS = 30L
        const val RUNNING_LOW_PERCENT = 35

        /** PAO choices offered in the add form, months. */
        val PAO_OPTIONS = listOf(3, 6, 12, 18, 24, 36)
    }
}

enum class ExpiryStatus { UNKNOWN, OK, SOON, EXPIRED }

enum class ItemBadge { EXPIRING, RUNNING_LOW }

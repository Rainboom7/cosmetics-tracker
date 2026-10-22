package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Thread-safe in-memory [CollectionRepository] for tests, previews and fakes. */
class InMemoryCollectionRepository(initial: List<CosmeticItem> = emptyList()) : CollectionRepository {
    private val items = MutableStateFlow(initial.associateBy { it.id })
    private val lock = Any()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeAll(): Flow<List<CosmeticItem>> = items.map { it.values.sortedByDescending(CosmeticItem::addedAtMillis) }

    override fun observeByStatus(status: ItemStatus): Flow<List<CosmeticItem>> =
        observeAll().map { list -> list.filter { it.status == status } }

    override fun observeItem(id: Long): Flow<CosmeticItem?> = items.map { it[id] }

    override suspend fun get(id: Long): CosmeticItem? = items.value[id]

    override suspend fun findByBarcode(barcode: String): List<CosmeticItem> =
        items.value.values.filter { it.barcode == barcode }

    override suspend fun upsert(item: CosmeticItem): Long = synchronized(lock) {
        val stored = if (item.id == 0L) item.copy(id = nextId++) else item
        items.update { it + (stored.id to stored) }
        stored.id
    }

    override suspend fun delete(id: Long) {
        items.update { it - id }
    }

    override suspend fun replaceAll(items: List<CosmeticItem>) = synchronized(lock) {
        var id = 1L
        val stored = items.map { it.copy(id = id++) }
        nextId = id
        this.items.value = stored.associateBy { it.id }
    }
}

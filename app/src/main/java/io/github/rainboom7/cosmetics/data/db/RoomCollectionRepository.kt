package io.github.rainboom7.cosmetics.data.db

import io.github.rainboom7.cosmetics.core.collection.CollectionRepository
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room is the source of truth: the catalog is only consulted while adding items. */
class RoomCollectionRepository(private val dao: ItemDao) : CollectionRepository {
    override fun observeAll(): Flow<List<CosmeticItem>> = dao.observeAll().map { rows -> rows.map(ItemEntity::toModel) }

    override fun observeByStatus(status: ItemStatus): Flow<List<CosmeticItem>> =
        dao.observeByStatus(status.name).map { rows -> rows.map(ItemEntity::toModel) }

    override fun observeItem(id: Long): Flow<CosmeticItem?> = dao.observe(id).map { it?.toModel() }

    override suspend fun get(id: Long): CosmeticItem? = dao.get(id)?.toModel()

    override suspend fun findByBarcode(barcode: String): List<CosmeticItem> = dao.findByBarcode(barcode).map(ItemEntity::toModel)

    override suspend fun upsert(item: CosmeticItem): Long {
        val rowId = dao.upsert(ItemEntity.from(item))
        return if (item.id != 0L) item.id else rowId
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun replaceAll(items: List<CosmeticItem>) =
        dao.replaceAll(items.map { ItemEntity.from(it.copy(id = 0)) })

    suspend fun getAll(): List<CosmeticItem> = dao.getAll().map(ItemEntity::toModel)
}

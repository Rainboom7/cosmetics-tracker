package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import kotlinx.coroutines.flow.Flow

/** Persistent storage of the user's items. The Android app implements it with Room. */
interface CollectionRepository {
    fun observeAll(): Flow<List<CosmeticItem>>

    fun observeByStatus(status: ItemStatus): Flow<List<CosmeticItem>>

    fun observeItem(id: Long): Flow<CosmeticItem?>

    suspend fun get(id: Long): CosmeticItem?

    suspend fun findByBarcode(barcode: String): List<CosmeticItem>

    /** Inserts when [CosmeticItem.id] is 0, updates otherwise. Returns the item's id. */
    suspend fun upsert(item: CosmeticItem): Long

    suspend fun delete(id: Long)

    /** Replaces the whole collection, used by backup import. Returns the new ids, in order. */
    suspend fun replaceAll(items: List<CosmeticItem>): List<Long>
}

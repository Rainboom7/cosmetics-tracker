package io.github.rainboom7.cosmetics.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ItemDao {
    @Query("SELECT * FROM items ORDER BY addedAtMillis DESC")
    abstract fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE status = :status ORDER BY addedAtMillis DESC")
    abstract fun observeByStatus(status: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    abstract fun observe(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id")
    abstract suspend fun get(id: Long): ItemEntity?

    @Query("SELECT * FROM items")
    abstract suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE barcode = :barcode")
    abstract suspend fun findByBarcode(barcode: String): List<ItemEntity>

    /** Returns the row id; for an update Room returns -1, callers use the entity id then. */
    @Upsert
    abstract suspend fun upsert(item: ItemEntity): Long

    @Query("DELETE FROM items WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM items")
    abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<ItemEntity>): List<Long>

    /** Deleting the items also deletes their usage marks (foreign key cascade). */
    @Transaction
    open suspend fun replaceAll(items: List<ItemEntity>): List<Long> {
        deleteAll()
        return insertAll(items)
    }
}

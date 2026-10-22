package io.github.rainboom7.cosmetics.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.rainboom7.cosmetics.core.usage.UsageLogRepository
import io.github.rainboom7.cosmetics.core.usage.UsageMark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Room row for [UsageMark]: one per item per day, [day] as an epoch day. Deleted with its item. */
@Entity(
    tableName = "usage_log",
    primaryKeys = ["itemId", "day"],
    foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("day")],
)
data class UsageEntity(val itemId: Long, val day: Long) {
    fun toModel() = UsageMark(itemId, LocalDate.ofEpochDay(day))

    companion object {
        fun from(mark: UsageMark) = UsageEntity(mark.itemId, mark.date.toEpochDay())
    }
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_log ORDER BY day DESC")
    fun observeAll(): Flow<List<UsageEntity>>

    @Query("SELECT day FROM usage_log WHERE itemId = :itemId ORDER BY day DESC")
    fun observeDays(itemId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<UsageEntity>)

    @Query("DELETE FROM usage_log WHERE itemId = :itemId AND day = :day")
    suspend fun delete(itemId: Long, day: Long)
}

class RoomUsageLogRepository(private val dao: UsageDao) : UsageLogRepository {
    override fun observeAll(): Flow<List<UsageMark>> = dao.observeAll().map { rows -> rows.map(UsageEntity::toModel) }

    override fun observeForItem(itemId: Long): Flow<List<LocalDate>> =
        dao.observeDays(itemId).map { days -> days.map(LocalDate::ofEpochDay) }

    override suspend fun add(mark: UsageMark) = dao.insert(listOf(UsageEntity.from(mark)))

    override suspend fun remove(mark: UsageMark) = dao.delete(mark.itemId, mark.date.toEpochDay())

    override suspend fun addAll(marks: List<UsageMark>) = dao.insert(marks.map(UsageEntity::from))
}

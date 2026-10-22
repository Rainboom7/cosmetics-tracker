package io.github.rainboom7.cosmetics.core.usage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** "Used [itemId] on [date]": one mark per item per day, the unit of the usage calendar. */
data class UsageMark(val itemId: Long, val date: LocalDate)

/**
 * Persistent usage calendar. The Android app implements it with Room, where deleting an item
 * deletes its marks too. A few marks a day for years is small enough to observe whole.
 */
interface UsageLogRepository {
    fun observeAll(): Flow<List<UsageMark>>

    /** The days [itemId] was used, newest first. */
    fun observeForItem(itemId: Long): Flow<List<LocalDate>>

    /** Adding a mark that already exists does nothing. */
    suspend fun add(mark: UsageMark)

    suspend fun remove(mark: UsageMark)

    suspend fun addAll(marks: List<UsageMark>)
}

/** Thread-safe in-memory [UsageLogRepository] for tests and fakes. */
class InMemoryUsageLogRepository(initial: List<UsageMark> = emptyList()) : UsageLogRepository {
    private val marks = MutableStateFlow(initial.toSet())

    override fun observeAll(): Flow<List<UsageMark>> = marks.map { it.sortedByDescending(UsageMark::date) }

    override fun observeForItem(itemId: Long): Flow<List<LocalDate>> =
        marks.map { set -> set.filter { it.itemId == itemId }.map { it.date }.sortedDescending() }

    override suspend fun add(mark: UsageMark) = marks.update { it + mark }

    override suspend fun remove(mark: UsageMark) = marks.update { it - mark }

    override suspend fun addAll(marks: List<UsageMark>) = this.marks.update { it + marks }
}

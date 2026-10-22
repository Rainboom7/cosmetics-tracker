package io.github.rainboom7.cosmetics.core.usage

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class UsageCalendarTest {
    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
    private val repo = InMemoryCollectionRepository()
    private val log = InMemoryUsageLogRepository()
    private val service = CollectionService(repo, clock)
    private val calendar = UsageCalendar(log, service)

    private fun day(d: Int) = LocalDate.of(2026, 10, d)

    @Test
    fun `toggling today marks and unmarks`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "Крем", usage = UsageState.OPENED, openedOn = day(1)))

        assertTrue(calendar.toggleToday(id))
        assertEquals(ItemUsage(lastUsed = today, usedToday = true, last30Days = 1, total = 1), calendar.observeItem(id).first())

        assertFalse(calendar.toggleToday(id))
        assertEquals(ItemUsage(), calendar.observeItem(id).first())
    }

    @Test
    fun `using a new item opens it on that day`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "Сыворотка", periodAfterOpeningMonths = 6))

        calendar.setUsed(id, day(10), used = true)

        val item = repo.get(id)!!
        assertEquals(UsageState.OPENED, item.usage)
        assertEquals(day(10), item.openedOn)
    }

    @Test
    fun `future days cannot be marked`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "Крем"))
        calendar.setUsed(id, day(23), used = true)
        assertEquals(emptyList<UsageMark>(), log.observeAll().first())
        assertEquals(UsageState.NEW, repo.get(id)!!.usage)
    }

    @Test
    fun `month view groups items by day and ranks the most used`() = runTest {
        val cream = repo.upsert(CosmeticItem(name = "Крем", usage = UsageState.OPENED, openedOn = day(1)))
        val spf = repo.upsert(CosmeticItem(name = "SPF", usage = UsageState.OPENED, openedOn = day(1)))
        val wish = repo.upsert(CosmeticItem(name = "Хочу", status = ItemStatus.WISHLIST))
        listOf(day(20), day(21), day(22)).forEach { calendar.setUsed(cream, it, true) }
        calendar.setUsed(spf, day(21), true)
        log.add(UsageMark(wish, day(21))) // not owned: ignored
        log.add(UsageMark(cream, LocalDate.of(2026, 9, 30))) // other month

        val month = calendar.observeMonth(YearMonth.of(2026, 10)).first()

        assertEquals(setOf(day(20), day(21), day(22)), month.days.keys)
        assertEquals(listOf("SPF", "Крем"), month.days.getValue(day(21)).map { it.name })
        assertEquals(3, month.activeDays)
        assertEquals(listOf("Крем" to 3, "SPF" to 1), month.mostUsed.map { it.first.name to it.second })
        assertEquals(3, month.streak)
    }

    @Test
    fun `streak counts back from yesterday when today is not marked yet`() {
        assertEquals(2, UsageCalendar.streak(setOf(day(20), day(21)), today))
        assertEquals(0, UsageCalendar.streak(setOf(day(19), day(20)), today))
        assertEquals(1, UsageCalendar.streak(setOf(day(22), day(20)), today))
    }

    @Test
    fun `forgotten items are opened ones unused for a month, never shelved ones`() {
        val old = CosmeticItem(id = 1, name = "old", usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 8, 1))
        val recent = CosmeticItem(id = 2, name = "recent", usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 8, 1))
        val shelved = old.copy(id = 3, name = "shelved", shelfStatus = ShelfStatus.NOT_USING)
        val unopened = CosmeticItem(id = 4, name = "new")
        val usedLongAgo = CosmeticItem(id = 5, name = "usedLongAgo", usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 1, 1))
        val owned = listOf(old, recent, shelved, unopened, usedLongAgo)
        val marks = listOf(UsageMark(2, day(15)), UsageMark(5, LocalDate.of(2026, 9, 22)))

        val forgotten = UsageCalendar.forgotten(marks, owned, today)

        assertEquals(listOf("old", "usedLongAgo"), forgotten.map { it.name })
        assertEquals(emptyList<CosmeticItem>(), UsageCalendar.forgotten(emptyList(), owned, today))
    }

    @Test
    fun `last 30 days include today and 29 days back`() {
        val usage = UsageCalendar.itemUsage(listOf(today, today.minusDays(29), today.minusDays(30)), today)
        assertEquals(2, usage.last30Days)
        assertEquals(3, usage.total)
    }
}

package io.github.rainboom7.cosmetics.ui

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.usage.InMemoryUsageLogRepository
import io.github.rainboom7.cosmetics.core.usage.UsageCalendar
import io.github.rainboom7.cosmetics.core.usage.UsageMark
import io.github.rainboom7.cosmetics.testing.MainDispatcherRule
import io.github.rainboom7.cosmetics.ui.calendar.CalendarViewModel
import io.github.rainboom7.cosmetics.ui.calendar.monthWeeks
import io.github.rainboom7.cosmetics.ui.collection.CollectionViewModel
import io.github.rainboom7.cosmetics.ui.detail.DetailState
import io.github.rainboom7.cosmetics.ui.detail.DetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class CalendarAndDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
    private val repo = InMemoryCollectionRepository(
        listOf(
            CosmeticItem(id = 1, name = "Крем", usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 9, 1)),
            CosmeticItem(id = 2, name = "Сыворотка", favorite = true),
            CosmeticItem(id = 3, name = "Тушь", usage = UsageState.FINISHED, remainingPercent = 0, finishedOn = LocalDate.of(2026, 2, 1)),
        ),
    )
    private val log = InMemoryUsageLogRepository(listOf(UsageMark(1, LocalDate.of(2026, 10, 21))))
    private val service = CollectionService(repo, clock)
    private val calendar = UsageCalendar(log, service)

    @Test
    fun `calendar marks the selected day and moves between months`() = runTest(main.dispatcher) {
        val vm = CalendarViewModel(calendar, service)
        backgroundScope.launch { vm.state.collect {} }

        val initial = vm.state.value
        assertEquals(YearMonth.of(2026, 10), initial.month)
        assertEquals(today, initial.selected)
        assertEquals(listOf("Сыворотка", "Крем"), initial.markable.map { it.name }) // favorite first, finished hidden
        assertEquals(1, initial.empties)
        assertEquals(1, initial.usage.streak)
        assertFalse(initial.canGoForward)

        vm.setUsed(2, used = true)
        assertEquals(listOf("Сыворотка"), vm.state.value.selectedItems.map { it.name })
        assertEquals(2, vm.state.value.usage.streak)
        assertEquals(UsageState.OPENED, repo.get(2)!!.usage) // using a new item opens it

        vm.select(LocalDate.of(2026, 10, 21))
        assertEquals(listOf("Крем"), vm.state.value.selectedItems.map { it.name })
        vm.select(LocalDate.of(2026, 10, 30)) // future: ignored
        assertEquals(LocalDate.of(2026, 10, 21), vm.state.value.selected)

        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        assertEquals(LocalDate.of(2026, 9, 30), vm.state.value.selected)
        assertTrue(vm.state.value.canGoForward)
        vm.nextMonth()
        vm.nextMonth() // can't go past the current month
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
    }

    @Test
    fun `detail toggles status, favorite and today's use`() = runTest(main.dispatcher) {
        val vm = DetailViewModel(1, service, photos = null, calendar = calendar)
        backgroundScope.launch { vm.state.collect {} }

        vm.toggleShelf(ShelfStatus.TRYING)
        assertEquals(ShelfStatus.TRYING, repo.get(1)!!.shelfStatus)
        vm.toggleShelf(ShelfStatus.TRYING)
        assertNull(repo.get(1)!!.shelfStatus)

        vm.toggleFavorite()
        assertTrue(repo.get(1)!!.favorite)

        vm.toggleUsedToday()
        val loaded = vm.state.value as DetailState.Loaded
        assertTrue(loaded.usage.usedToday)
        assertEquals(2, loaded.usage.last30Days)
        assertEquals(listOf(today, LocalDate.of(2026, 10, 21)), log.observeForItem(1).first())
    }

    @Test
    fun `collection status chips filter and toggle off`() = runTest(main.dispatcher) {
        repo.upsert(repo.get(1)!!.copy(shelfStatus = ShelfStatus.NOT_USING))
        val vm = CollectionViewModel(service)
        backgroundScope.launch { vm.state.collect {} }

        assertEquals(mapOf(ShelfStatus.NOT_USING to 1), vm.state.value.shelfCounts)
        assertEquals(1, vm.state.value.favoriteCount)

        vm.toggleShelf(ShelfStatus.NOT_USING)
        assertEquals(listOf(1L), vm.state.value.items.map { it.id })
        vm.toggleShelf(ShelfStatus.NOT_USING)
        assertEquals(3, vm.state.value.items.size)

        vm.toggleFavoritesOnly()
        assertEquals(listOf(2L), vm.state.value.items.map { it.id })
    }

    @Test
    fun `month grid starts on Monday and pads to full weeks`() {
        val weeks = monthWeeks(YearMonth.of(2026, 10)) // 1 October 2026 is a Thursday
        assertEquals(5, weeks.size)
        assertEquals(listOf(null, null, null), weeks.first().take(3))
        assertEquals(LocalDate.of(2026, 10, 1), weeks.first()[3])
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(31, weeks.flatten().count { it != null })
    }
}

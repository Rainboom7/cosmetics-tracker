package io.github.rainboom7.cosmetics.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.usage.MonthUsage
import io.github.rainboom7.cosmetics.core.usage.UsageCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = today,
    val usage: MonthUsage = MonthUsage(YearMonth.from(today)),
    /** Items that can be marked: owned and not used up. Favorites first. */
    val markable: List<CosmeticItem> = emptyList(),
    /** "Пустые баночки" this year. */
    val empties: Int = 0,
) {
    val month: YearMonth get() = usage.month

    val selectedItems: List<CosmeticItem> get() = usage.days[selected].orEmpty()

    val canGoForward: Boolean get() = month < YearMonth.from(today)
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val calendar: UsageCalendar,
    private val service: CollectionService,
) : ViewModel() {
    private val today = service.today()
    private val month = MutableStateFlow(YearMonth.from(today))
    private val selected = MutableStateFlow(today)

    val state: StateFlow<CalendarUiState> = combine(
        month.flatMapLatest { calendar.observeMonth(it) },
        selected,
        service.observe(ItemStatus.OWNED),
        service.observeSummary(),
    ) { usage, day, owned, summary ->
        CalendarUiState(
            loading = false,
            today = today,
            selected = day,
            usage = usage,
            markable = owned
                .filter { it.usage != UsageState.FINISHED }
                .sortedWith(compareByDescending<CosmeticItem> { it.favorite }.thenBy { it.name.lowercase() }),
            empties = summary.finishedThisYear,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(today = today))

    fun previousMonth() = showMonth(month.value.minusMonths(1))

    fun nextMonth() {
        if (month.value < YearMonth.from(today)) showMonth(month.value.plusMonths(1))
    }

    /** Days after today can't be picked: there is nothing to mark there yet. */
    fun select(date: LocalDate) {
        if (!date.isAfter(today)) selected.value = date
    }

    /** Marks or unmarks [itemId] on the selected day. */
    fun setUsed(itemId: Long, used: Boolean) {
        val day = selected.value
        viewModelScope.launch { calendar.setUsed(itemId, day, used) }
    }

    private fun showMonth(target: YearMonth) {
        month.value = target
        // Past months open on their last day, the current one on today.
        selected.value = if (target == YearMonth.from(today)) today else target.atEndOfMonth()
    }
}

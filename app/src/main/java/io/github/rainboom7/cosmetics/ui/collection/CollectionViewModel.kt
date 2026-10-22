package io.github.rainboom7.cosmetics.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.collection.CollectionQuery
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.applyQuery
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.usage.UsageCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CollectionUiState(
    val loading: Boolean = true,
    val query: CollectionQuery = CollectionQuery(),
    /** Items after the filter. */
    val items: List<CosmeticItem> = emptyList(),
    /** Size of the whole collection, for "Все · N" and the empty state. */
    val totalCount: Int = 0,
    /** "У N средств срок после вскрытия истекает в этом месяце"; the banner hides at 0. */
    val expiringSoon: Int = 0,
    val today: LocalDate = LocalDate.now(),
    /** Counts for the status chips; a chip is shown only when its count is above 0 or it is selected. */
    val shelfCounts: Map<ShelfStatus, Int> = emptyMap(),
    val favoriteCount: Int = 0,
    /** Owned items per category, for the counters on the category bubbles. */
    val categoryCounts: Map<Category, Int> = emptyMap(),
    /** Items marked as used today: their card button shows "✓ Сегодня". */
    val usedToday: Set<Long> = emptySet(),
) {
    /** For the "заканчивается" banner, counting the automatic mark too. */
    val runningLow: Int get() = shelfCounts[ShelfStatus.RUNNING_LOW] ?: 0
}

class CollectionViewModel(
    private val service: CollectionService,
    private val usage: UsageCalendar? = null,
) : ViewModel() {
    private val query = MutableStateFlow(CollectionQuery())

    private val usedToday: Flow<Set<Long>> = usage
        ?.observeMonth(YearMonth.from(service.today()))
        ?.map { month -> month.days[service.today()].orEmpty().mapTo(HashSet()) { it.id } }
        ?: flowOf(emptySet())

    val state: StateFlow<CollectionUiState> = combine(
        service.observe(ItemStatus.OWNED),
        service.observeSummary(),
        query,
        usedToday,
    ) { owned, summary, q, today ->
        CollectionUiState(
            loading = false,
            query = q,
            items = owned.applyQuery(q),
            totalCount = owned.size,
            expiringSoon = summary.expiringSoonCount,
            today = service.today(),
            shelfCounts = summary.shelfCounts,
            favoriteCount = summary.favoriteCount,
            categoryCounts = owned.groupingBy { it.category }.eachCount(),
            usedToday = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())

    fun setText(text: String) = query.update { it.copy(text = text) }

    /** Category bubble tap; tapping the selected category again goes back to "Все". */
    fun setCategory(category: Category?) = query.update { it.copy(category = if (it.category == category) null else category) }

    /** Status chip tap: selects [shelf], or clears it when it is already selected. */
    fun toggleShelf(shelf: ShelfStatus) = query.update { it.copy(shelf = if (it.shelf == shelf) null else shelf) }

    fun toggleFavoritesOnly() = query.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    /** Heart on a card. */
    fun toggleFavorite(id: Long) {
        viewModelScope.launch { service.get(id)?.let { service.setFavorite(id, !it.favorite) } }
    }

    /** "Сегодня" button on a card: flips today's usage mark, like the button on the item screen. */
    fun toggleUsedToday(id: Long) {
        viewModelScope.launch { usage?.toggleToday(id) }
    }

    companion object {
        /** Category bubbles on the collection screen, in order; null is "Все". */
        val FILTER_CATEGORIES: List<Category?> = listOf(
            null, Category.SKINCARE, Category.MAKEUP, Category.HAIR, Category.BODY, Category.FRAGRANCE, Category.NAILS, Category.OTHER,
        )
    }
}

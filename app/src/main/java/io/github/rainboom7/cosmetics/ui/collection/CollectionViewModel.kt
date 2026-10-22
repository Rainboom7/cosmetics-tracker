package io.github.rainboom7.cosmetics.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.collection.CollectionQuery
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.applyQuery
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

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
)

class CollectionViewModel(private val service: CollectionService) : ViewModel() {
    private val query = MutableStateFlow(CollectionQuery())

    val state: StateFlow<CollectionUiState> = combine(
        service.observe(ItemStatus.OWNED),
        service.observeSummary(),
        query,
    ) { owned, summary, q ->
        CollectionUiState(
            loading = false,
            query = q,
            items = owned.applyQuery(q),
            totalCount = owned.size,
            expiringSoon = summary.expiringSoonCount,
            today = service.today(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())

    fun setText(text: String) = query.update { it.copy(text = text) }

    fun setCategory(category: Category?) = query.update { it.copy(category = category) }

    companion object {
        /** Filter chips on the collection screen, in order; null is "Все". */
        val FILTER_CATEGORIES: List<Category?> = listOf(null, Category.SKINCARE, Category.MAKEUP, Category.HAIR, Category.BODY)
    }
}

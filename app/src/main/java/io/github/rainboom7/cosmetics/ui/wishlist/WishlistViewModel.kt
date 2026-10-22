package io.github.rainboom7.cosmetics.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.DuplicateFinder
import io.github.rainboom7.cosmetics.core.collection.WishlistFilter
import io.github.rainboom7.cosmetics.core.collection.applyWishlistFilter
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WishRow(val item: CosmeticItem, val alreadyOwned: Boolean)

data class WishlistUiState(
    val loading: Boolean = true,
    val filter: WishlistFilter = WishlistFilter.ALL,
    val rows: List<WishRow> = emptyList(),
    val totalCount: Int = 0,
)

class WishlistViewModel(private val service: CollectionService) : ViewModel() {
    private val filter = MutableStateFlow(WishlistFilter.ALL)
    private val boughtEvents = Channel<CosmeticItem>(Channel.BUFFERED)

    /** Emits the item as it was before "Куплено", for the Snackbar "Отменить". */
    val bought: Flow<CosmeticItem> = boughtEvents.receiveAsFlow()

    val state: StateFlow<WishlistUiState> = combine(
        service.observe(ItemStatus.WISHLIST),
        service.observe(ItemStatus.OWNED),
        filter,
    ) { wishes, owned, f ->
        WishlistUiState(
            loading = false,
            filter = f,
            rows = wishes.applyWishlistFilter(f).map { wish ->
                WishRow(wish, alreadyOwned = owned.any { DuplicateFinder.isSimilar(wish, it) })
            },
            totalCount = wishes.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WishlistUiState())

    fun setFilter(value: WishlistFilter) {
        filter.value = value
    }

    fun markBought(id: Long) {
        viewModelScope.launch {
            service.markBought(id)?.let { boughtEvents.send(it) }
        }
    }

    /** Heart on a wish card: "Хочу сильно". */
    fun toggleWantBadly(id: Long) {
        viewModelScope.launch { service.get(id)?.let { service.setWantBadly(id, !it.wantBadly) } }
    }

    fun undo(before: CosmeticItem) {
        viewModelScope.launch { service.restore(before) }
    }
}

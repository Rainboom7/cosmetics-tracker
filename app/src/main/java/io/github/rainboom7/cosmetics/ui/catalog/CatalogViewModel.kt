package io.github.rainboom7.cosmetics.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.catalog.CatalogException
import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.KnownBarcodes
import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /** All loaded results; [visible] applies the filter chips. */
    val results: List<CatalogProduct> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val error: Boolean = false,
    val category: Category? = null,
    val brand: String? = null,
    val withPhotoOnly: Boolean = false,
) {
    val visible: List<CatalogProduct>
        get() = results.filter { p ->
            (category == null || p.category == category) &&
                (brand == null || p.brand == brand) &&
                (!withPhotoOnly || p.imageUrl != null)
        }

    val brands: List<String> get() = results.mapNotNull { it.brand }.distinct().sorted()

    /** Searched and got nothing: show "Нет нужного средства в базе?". */
    val nothingFound: Boolean get() = query.isNotBlank() && !loading && !error && page > 0 && visible.isEmpty()
}

enum class CatalogMessage { ADDED_COLLECTION, ADDED_WISHLIST, ALREADY_THERE, MOVED_FROM_WISHLIST }

@OptIn(FlowPreview::class)
class CatalogViewModel(
    private val catalog: ProductCatalog,
    private val service: CollectionService,
    initialQuery: String = "",
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogUiState(query = initialQuery))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    val known: StateFlow<KnownBarcodes> =
        service.observeKnownBarcodes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnownBarcodes())

    private val messages = Channel<CatalogMessage>(Channel.BUFFERED)
    val events: Flow<CatalogMessage> = messages.receiveAsFlow()

    private val queryInput = MutableStateFlow(initialQuery)
    private var searchJob: Job? = null

    /** Typing back and forth must not repeat requests against the rate limit. */
    private val firstPages = object : LinkedHashMap<String, CatalogPage>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CatalogPage>) = size > 20
    }

    init {
        if (initialQuery.isNotBlank()) search(initialQuery)
        viewModelScope.launch {
            queryInput.drop(1).debounce(DEBOUNCE_MS).distinctUntilChanged().collect { typed ->
                // Open Beauty Facts allows about 10 searches a minute: don't spend them on one or two letters.
                if (typed.isBlank() || typed.trim().length >= MIN_TYPED_QUERY) search(typed)
            }
        }
    }

    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text) }
        queryInput.value = text
    }

    fun setCategory(category: Category?) = _state.update { it.copy(category = category) }

    fun setBrand(brand: String?) = _state.update { it.copy(brand = brand) }

    fun toggleWithPhoto() = _state.update { it.copy(withPhotoOnly = !it.withPhotoOnly) }

    fun retry() {
        firstPages.remove(_state.value.query.trim())
        search(_state.value.query)
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.loading || current.loadingMore) return
        _state.update { it.copy(loadingMore = true) }
        searchJob = viewModelScope.launch {
            try {
                val next = catalog.search(current.query, current.page + 1)
                _state.update {
                    it.copy(
                        loadingMore = false,
                        results = (it.results + next.products).distinctBy { p -> p.barcode ?: p.name },
                        page = next.page,
                        hasMore = next.hasMore,
                    )
                }
            } catch (e: CatalogException) {
                _state.update { it.copy(loadingMore = false, error = true) }
            }
        }
    }

    fun add(product: CatalogProduct, status: ItemStatus) {
        viewModelScope.launch {
            val message = when (service.addFromCatalog(product, status)) {
                is CollectionService.AddResult.AlreadyThere -> CatalogMessage.ALREADY_THERE
                is CollectionService.AddResult.MovedFromWishlist -> CatalogMessage.MOVED_FROM_WISHLIST
                is CollectionService.AddResult.Added ->
                    if (status == ItemStatus.OWNED) CatalogMessage.ADDED_COLLECTION else CatalogMessage.ADDED_WISHLIST
            }
            messages.send(message)
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(loading = false, results = emptyList(), page = 0, hasMore = false, error = false) }
            return
        }
        _state.update { it.copy(loading = true, error = false) }
        searchJob = viewModelScope.launch {
            try {
                val page = firstPages[query.trim()] ?: catalog.search(query, 1).also { firstPages[query.trim()] = it }
                _state.update {
                    it.copy(loading = false, results = page.products, page = 1, hasMore = page.hasMore, brand = null)
                }
            } catch (e: CatalogException) {
                _state.update { it.copy(loading = false, error = true, results = emptyList(), page = 0, hasMore = false) }
            }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 800L
        const val MIN_TYPED_QUERY = 3
    }
}

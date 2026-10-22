package io.github.rainboom7.cosmetics.ui.catalogcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.catalog.CatalogException
import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.KnownBarcodes
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.ui.catalog.CatalogMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Hands the tapped search result to the product card, so the route needs no arguments. */
class CatalogSelection {
    @Volatile
    var product: CatalogProduct? = null
}

data class CatalogCardUiState(
    val product: CatalogProduct?,
    /** Fetching ingredients and details by barcode; the card already shows what search had. */
    val loadingDetails: Boolean = false,
)

class CatalogCardViewModel(
    initial: CatalogProduct?,
    private val catalog: ProductCatalog,
    private val service: CollectionService,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogCardUiState(initial))
    val state: StateFlow<CatalogCardUiState> = _state.asStateFlow()

    val known: StateFlow<KnownBarcodes> =
        service.observeKnownBarcodes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnownBarcodes())

    private val messages = Channel<CatalogMessage>(Channel.BUFFERED)
    val events: Flow<CatalogMessage> = messages.receiveAsFlow()

    init {
        val barcode = initial?.barcode
        if (barcode != null) {
            _state.update { it.copy(loadingDetails = true) }
            viewModelScope.launch {
                val full = try {
                    catalog.findByBarcode(barcode)
                } catch (e: CatalogException) {
                    null // Offline or rate limited: the card keeps what the search result had.
                }
                _state.update { it.copy(product = it.product?.let { shown -> enrich(shown, full) }, loadingDetails = false) }
            }
        }
    }

    fun add(status: ItemStatus) {
        val product = _state.value.product ?: return
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

    companion object {
        /** The lookup wins where it knows more; nothing the search result showed is lost. */
        fun enrich(shown: CatalogProduct, full: CatalogProduct?): CatalogProduct {
            if (full == null) return shown
            return shown.copy(
                imageUrl = shown.imageUrl ?: full.imageUrl,
                quantity = shown.quantity ?: full.quantity,
                ingredients = full.ingredients ?: shown.ingredients,
                details = full.details ?: shown.details,
            )
        }
    }
}

package io.github.rainboom7.cosmetics.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.catalog.CatalogException
import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.model.Barcodes
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.photo.FormPrefiller
import io.github.rainboom7.cosmetics.core.photo.PhotoMatch
import io.github.rainboom7.cosmetics.core.photo.PhotoRecognition
import io.github.rainboom7.cosmetics.core.photo.PhotoSearch
import io.github.rainboom7.cosmetics.data.photo.PhotoRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanMode { BARCODE, PHOTO }

/** The "Это оно?" sheet. */
data class Candidates(
    val matches: List<PhotoMatch>,
    /** The package photo, attached to the item as the user's own photo. */
    val photoUri: String?,
    val recognition: PhotoRecognition,
    val barcode: String?,
)

data class ScanUiState(
    val mode: ScanMode = ScanMode.BARCODE,
    val busy: Boolean = false,
    val candidates: Candidates? = null,
    val failed: Boolean = false,
)

sealed interface ScanEvent {
    /** Go to the add form; the draft is in [AddFlowStore]. */
    data object OpenForm : ScanEvent

    data class Added(val status: ItemStatus, val result: CollectionService.AddResult) : ScanEvent
}

class ScanViewModel(
    private val target: ItemStatus,
    initialMode: ScanMode,
    private val catalog: ProductCatalog,
    private val photoSearch: PhotoSearch,
    private val recognizer: PhotoRecognizer,
    private val service: CollectionService,
    private val addFlow: AddFlowStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanUiState(mode = initialMode))
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val eventChannel = Channel<ScanEvent>(Channel.BUFFERED)
    val events: Flow<ScanEvent> = eventChannel.receiveAsFlow()

    fun setMode(mode: ScanMode) = _state.update { it.copy(mode = mode, failed = false) }

    /** Called by the live analyzer for every frame with a barcode; repeats are ignored while busy. */
    fun onBarcode(raw: String) {
        val code = Barcodes.normalize(raw) ?: return
        val current = _state.value
        if (current.busy || current.candidates != null || current.mode != ScanMode.BARCODE) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val product = catalog.findByBarcode(code)
                if (product != null) {
                    _state.update {
                        it.copy(busy = false, candidates = Candidates(listOf(PhotoMatch(product, 100)), null, PhotoRecognition(listOf(code)), code))
                    }
                } else {
                    openForm(AddDraft(DraftReason.BARCODE_NOT_FOUND, target, barcode = code))
                }
            } catch (e: CatalogException) {
                openForm(AddDraft(DraftReason.OFFLINE, target, barcode = code))
            }
        }
    }

    /** A package photo from the camera or the gallery, already stored in app storage. */
    fun onPhoto(uri: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            val photo = uri.toString()
            val recognition = try {
                recognizer.recognize(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // ML Kit failures (corrupt image, model not ready) leave the user with the manual form.
                PhotoRecognition()
            }
            val barcode = recognition.barcodes.firstNotNullOfOrNull(Barcodes::normalize)
            try {
                val result = photoSearch.search(recognition)
                if (result.matches.isEmpty()) {
                    openForm(draftFrom(DraftReason.NO_MATCH, photo, recognition, barcode))
                } else {
                    _state.update {
                        it.copy(busy = false, candidates = Candidates(result.matches.take(MAX_CANDIDATES), photo, recognition, barcode))
                    }
                }
            } catch (e: CatalogException) {
                openForm(draftFrom(DraftReason.OFFLINE, photo, recognition, barcode))
            }
        }
    }

    fun onCaptureFailed() = _state.update { it.copy(busy = false, failed = true) }

    fun pick(match: PhotoMatch, status: ItemStatus) {
        val candidates = _state.value.candidates ?: return
        viewModelScope.launch {
            val result = service.addFromCatalog(match.product, status, listOfNotNull(candidates.photoUri))
            _state.update { it.copy(candidates = null) }
            eventChannel.send(ScanEvent.Added(status, result))
        }
    }

    /** "Ничего не подходит — заполнить по фото". */
    fun nothingFits() {
        val candidates = _state.value.candidates ?: return
        viewModelScope.launch {
            openForm(draftFrom(DraftReason.NO_MATCH, candidates.photoUri, candidates.recognition, candidates.barcode))
        }
    }

    fun dismissCandidates() = _state.update { it.copy(candidates = null) }

    fun enterManually() {
        viewModelScope.launch { openForm(AddDraft(DraftReason.MANUAL, target)) }
    }

    private fun draftFrom(reason: DraftReason, photo: String?, recognition: PhotoRecognition, barcode: String?) =
        AddDraft(reason, target, photoUri = photo, barcode = barcode, prefill = FormPrefiller.prefill(recognition))

    private suspend fun openForm(draft: AddDraft) {
        addFlow.put(draft)
        _state.update { it.copy(busy = false, candidates = null) }
        eventChannel.send(ScanEvent.OpenForm)
    }

    companion object {
        const val MAX_CANDIDATES = 5
    }
}

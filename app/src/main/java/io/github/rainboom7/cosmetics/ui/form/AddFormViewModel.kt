package io.github.rainboom7.cosmetics.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.DuplicateFinder
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.ui.scan.AddDraft
import io.github.rainboom7.cosmetics.ui.scan.AddFlowStore
import io.github.rainboom7.cosmetics.ui.scan.DraftReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the user can type on the "Нет в базе" / manual form. */
data class FormFields(
    val photoUri: String? = null,
    val brand: String = "",
    val name: String = "",
    val category: Category = Category.OTHER,
    val shade: String = "",
    val periodAfterOpeningMonths: Int? = null,
    val openedOn: LocalDate? = null,
    val notes: String = "",
    val wantBadly: Boolean = false,
    val price: String = "",
    val link: String = "",
)

data class FormUiState(
    val fields: FormFields = FormFields(),
    val reason: DraftReason = DraftReason.MANUAL,
    val target: ItemStatus = ItemStatus.OWNED,
    /** Non-null when editing an existing item: one "Сохранить" button instead of two targets. */
    val editing: CosmeticItem? = null,
    val barcode: String? = null,
    val similar: List<CosmeticItem> = emptyList(),
    val saving: Boolean = false,
) {
    /** The design requires a picture: the user's photo, or the catalog one when editing such an item. */
    val hasPicture: Boolean get() = fields.photoUri != null || editing?.hasImage == true

    val canSave: Boolean get() = fields.name.isNotBlank() && hasPicture && !saving
}

class AddFormViewModel(
    private val service: CollectionService,
    addFlow: AddFlowStore,
    target: ItemStatus,
    private val editId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(FormUiState(target = target))

    private val savedChannel = Channel<Long>(Channel.BUFFERED)

    /** Emits the saved item's id; the screen navigates away. */
    val saved: Flow<Long> = savedChannel.receiveAsFlow()

    val state: StateFlow<FormUiState> = combine(_state, service.observeAll()) { form, all ->
        form.copy(similar = DuplicateFinder.findSimilar(form.toCandidate(), all.filter { it.id != form.editing?.id }).take(3))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _state.value)

    init {
        if (editId != null) {
            viewModelScope.launch {
                service.get(editId)?.let { item -> _state.update { it.copy(editing = item, target = item.status, barcode = item.barcode, fields = item.toFields()) } }
            }
        } else {
            addFlow.take()?.let { draft -> _state.update { it.fromDraft(draft) } }
        }
    }

    fun update(transform: (FormFields) -> FormFields) = _state.update { it.copy(fields = transform(it.fields)) }

    fun save(status: ItemStatus) {
        val form = _state.value
        if (!form.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val item = form.toItem(status)
            val id = service.save(item)
            savedChannel.send(id)
        }
    }

    private fun FormUiState.toCandidate(): CosmeticItem = CosmeticItem(
        id = editing?.id ?: 0,
        name = fields.name.ifBlank { "-" },
        brand = fields.brand.trim().ifEmpty { null },
        barcode = barcode,
    )

    private fun FormUiState.toItem(status: ItemStatus): CosmeticItem {
        val f = fields
        val base = editing ?: CosmeticItem(name = f.name.trim(), barcode = barcode)
        val newPhoto = f.photoUri
        val photos = when {
            newPhoto == null -> base.photoUris
            base.photoUris.firstOrNull() == newPhoto -> base.photoUris
            else -> listOf(newPhoto) + (base.photoUris - newPhoto)
        }
        return base.copy(
            name = f.name.trim(),
            brand = f.brand.trim().ifEmpty { null },
            category = f.category,
            status = status,
            shade = f.shade.trim().ifEmpty { null },
            periodAfterOpeningMonths = f.periodAfterOpeningMonths,
            openedOn = if (status == ItemStatus.OWNED) f.openedOn else null,
            usage = when {
                status != ItemStatus.OWNED -> UsageState.NEW
                editing != null && editing.usage == UsageState.FINISHED -> UsageState.FINISHED
                f.openedOn != null -> UsageState.OPENED
                else -> UsageState.NEW
            },
            photoUris = photos,
            notes = f.notes.trim(),
            wantBadly = status == ItemStatus.WISHLIST && f.wantBadly,
            price = f.price.trim().ifEmpty { null },
            link = f.link.trim().ifEmpty { null },
        )
    }

    private fun FormUiState.fromDraft(draft: AddDraft) = copy(
        reason = draft.reason,
        target = draft.target,
        barcode = draft.barcode,
        fields = fields.copy(
            photoUri = draft.photoUri,
            brand = draft.prefill.brand.orEmpty(),
            name = draft.prefill.name.orEmpty(),
            category = draft.prefill.category,
            periodAfterOpeningMonths = draft.prefill.periodAfterOpeningMonths,
            openedOn = if (draft.target == ItemStatus.OWNED) service.today() else null,
        ),
    )

    private fun CosmeticItem.toFields() = FormFields(
        photoUri = photoUris.firstOrNull(),
        brand = brand.orEmpty(),
        name = name,
        category = category,
        shade = shade.orEmpty(),
        periodAfterOpeningMonths = periodAfterOpeningMonths,
        openedOn = openedOn,
        notes = notes,
        wantBadly = wantBadly,
        price = price.orEmpty(),
        link = link.orEmpty(),
    )
}

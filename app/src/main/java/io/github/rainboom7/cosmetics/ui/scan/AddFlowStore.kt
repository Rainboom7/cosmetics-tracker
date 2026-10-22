package io.github.rainboom7.cosmetics.ui.scan

import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.photo.FormPrefill

/** Why the user lands on the add form; the form shows a matching info line. */
enum class DraftReason {
    /** "Ввести вручную". */
    MANUAL,

    /** The barcode is not in Open Beauty Facts: normal for many Russian products, not an error. */
    BARCODE_NOT_FOUND,

    /** Photo search found nothing, or the user said "Ничего не подходит". */
    NO_MATCH,

    /** The catalog could not be reached. */
    OFFLINE,
}

/** What the scanner hands to the add form: the package photo, the barcode and OCR guesses. */
data class AddDraft(
    val reason: DraftReason,
    val target: ItemStatus = ItemStatus.OWNED,
    val photoUri: String? = null,
    val barcode: String? = null,
    val prefill: FormPrefill = FormPrefill(),
)

/**
 * In-memory hand-off between the scan screen and the form, so navigation arguments stay simple
 * ids. Lost on process death, which only means the form opens empty.
 */
class AddFlowStore {
    @Volatile
    private var draft: AddDraft? = null

    fun put(value: AddDraft) {
        draft = value
    }

    /** Returns the pending draft and clears it. */
    fun take(): AddDraft? = draft.also { draft = null }
}

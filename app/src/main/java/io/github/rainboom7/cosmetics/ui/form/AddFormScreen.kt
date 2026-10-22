package io.github.rainboom7.cosmetics.ui.form

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.CosmeticsApp
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.ui.components.FullDate
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel
import io.github.rainboom7.cosmetics.ui.scan.DraftReason
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun AddFormRoute(target: ItemStatus, editId: Long?, onClose: () -> Unit, onSaved: (Long) -> Unit, onOpenItem: (Long) -> Unit) {
    val viewModel = appViewModel(key = "form-$editId") { AddFormViewModel(it.collection, it.addFlow, target, editId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val photos = remember { (context.applicationContext as CosmeticsApp).container.photoStorage }
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onSaved(it) } }

    var pendingCameraFile by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCameraFile
        if (ok && path != null) viewModel.update { it.copy(photoUri = Uri.fromFile(java.io.File(path)).toString()) }
        pendingCameraFile = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { val stored = photos.importFrom(uri); viewModel.update { it.copy(photoUri = stored) } }
    }
    AddFormScreen(
        state = state,
        onChange = viewModel::update,
        onSave = viewModel::save,
        onClose = onClose,
        onTakePhoto = {
            val file = photos.newPhotoFile()
            pendingCameraFile = file.absolutePath
            takePicture.launch(photos.contentUriFor(file))
        },
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onOpenItem = onOpenItem,
    )
}

@Composable
fun AddFormScreen(
    state: FormUiState,
    onChange: ((FormFields) -> FormFields) -> Unit,
    onSave: (ItemStatus) -> Unit,
    onClose: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenItem: (Long) -> Unit,
) {
    val f = state.fields
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.editing != null) R.string.edit_title else R.string.add_title)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close)) } },
                actions = {
                    if (state.editing != null) {
                        TextButton(onClick = { onSave(state.target) }, enabled = state.canSave, modifier = Modifier.testTag("save")) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.editing == null) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { onSave(ItemStatus.WISHLIST) }, enabled = state.canSave, modifier = Modifier.weight(1f).testTag("save_wishlist")) {
                        Text(stringResource(R.string.action_to_wishlist))
                    }
                    Button(onClick = { onSave(ItemStatus.OWNED) }, enabled = state.canSave, modifier = Modifier.weight(1f).testTag("save_collection")) {
                        Text(stringResource(R.string.action_to_collection))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReasonBanner(state.reason, state.barcode)
            PhotoSlot(state, onTakePhoto, onPickPhoto)
            OutlinedTextField(f.brand, { v -> onChange { it.copy(brand = v) } }, label = { Text(stringResource(R.string.field_brand)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("field_brand"))
            OutlinedTextField(f.name, { v -> onChange { it.copy(name = v) } }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("field_name"))
            CategoryField(f.category) { c -> onChange { it.copy(category = c) } }
            OutlinedTextField(f.shade, { v -> onChange { it.copy(shade = v) } }, label = { Text(stringResource(R.string.field_shade)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            PaoField(f.periodAfterOpeningMonths) { m -> onChange { it.copy(periodAfterOpeningMonths = m) } }
            val owned = (state.editing?.status ?: state.target) == ItemStatus.OWNED
            if (owned || state.editing == null) OpenedField(f) { d -> onChange { it.copy(openedOn = d) } }
            if (!owned || state.editing == null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onChange { it.copy(wantBadly = !it.wantBadly) } }) {
                    Checkbox(checked = f.wantBadly, onCheckedChange = { v -> onChange { it.copy(wantBadly = v) } })
                    Text(stringResource(R.string.want_badly))
                }
                OutlinedTextField(f.price, { v -> onChange { it.copy(price = v) } }, label = { Text(stringResource(R.string.field_price)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.link, { v -> onChange { it.copy(link = v) } }, label = { Text(stringResource(R.string.field_link)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(f.notes, { v -> onChange { it.copy(notes = v) } }, label = { Text(stringResource(R.string.field_notes)) }, modifier = Modifier.fillMaxWidth())
            state.similar.firstOrNull()?.let { SimilarBanner(it, onOpenItem) }
            if (!state.hasPicture || f.name.isBlank()) {
                Text(stringResource(R.string.form_requirements), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReasonBanner(reason: DraftReason, barcode: String?) {
    val text = when (reason) {
        DraftReason.MANUAL -> return
        DraftReason.BARCODE_NOT_FOUND -> stringResource(R.string.reason_not_found)
        DraftReason.NO_MATCH -> stringResource(R.string.reason_no_match)
        DraftReason.OFFLINE -> stringResource(R.string.reason_offline)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("reason_banner")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            barcode?.let { Text(stringResource(R.string.barcode_value, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun PhotoSlot(state: FormUiState, onTakePhoto: () -> Unit, onPickPhoto: () -> Unit) {
    val picture = state.fields.photoUri ?: state.editing?.coverImage
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(104.dp).clip(MaterialTheme.shapes.large)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.large)
                .clickable(onClick = onTakePhoto)
                .testTag("photo_slot"),
            contentAlignment = Alignment.Center,
        ) {
            if (picture != null) {
                ItemImage(picture, state.fields.category, null, Modifier.fillMaxSize())
            } else {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = stringResource(R.string.action_take_photo), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column {
            Text(stringResource(R.string.photo_required), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onTakePhoto) { Text(stringResource(if (picture == null) R.string.action_take_photo else R.string.action_retake)) }
            TextButton(onClick = onPickPhoto) { Text(stringResource(R.string.from_gallery)) }
        }
    }
}

@Composable
private fun CategoryField(value: Category, onSelect: (Category) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = categoryLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_category)) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent layer over the read-only field opens the menu.
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Category.entries.forEach { c ->
                DropdownMenuItem(text = { Text(categoryLabel(c)) }, onClick = { open = false; onSelect(c) })
            }
        }
    }
}

@Composable
private fun PaoField(value: Int?, onSelect: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.field_pao), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CosmeticItem.PAO_OPTIONS.forEach { months ->
                FilterChip(
                    selected = value == months,
                    onClick = { onSelect(if (value == months) null else months) },
                    label = { Text(stringResource(R.string.pao_short, months)) },
                )
            }
        }
    }
}

@Composable
private fun OpenedField(fields: FormFields, onSelect: (java.time.LocalDate?) -> Unit) {
    var pick by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { pick = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.opened_date), modifier = Modifier.weight(1f))
        Text(fields.openedOn?.format(FullDate) ?: stringResource(R.string.not_opened), color = MaterialTheme.colorScheme.primary)
        if (fields.openedOn != null) {
            IconButton(onClick = { onSelect(null) }) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.not_opened)) }
        }
    }
    if (pick) {
        val initial = fields.openedOn?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pick = false },
            confirmButton = {
                TextButton(onClick = {
                    pick = false
                    pickerState.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pick = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun SimilarBanner(item: CosmeticItem, onOpenItem: (Long) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("similar_banner")) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemImage(item.coverImage, item.category, item.name, Modifier.size(48.dp).clip(MaterialTheme.shapes.medium), iconSize = 20.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.similar_owned), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(listOfNotNull(item.brand, item.name).joinToString(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            TextButton(onClick = { onOpenItem(item.id) }) { Text(stringResource(R.string.action_open)) }
        }
    }
}

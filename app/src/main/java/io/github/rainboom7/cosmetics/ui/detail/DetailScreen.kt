package io.github.rainboom7.cosmetics.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ExpiryStatus
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.usage.ItemUsage
import io.github.rainboom7.cosmetics.ui.components.DayMonthWords
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.FullDate
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel
import io.github.rainboom7.cosmetics.ui.components.shelfLabel
import io.github.rainboom7.cosmetics.ui.components.usageLabel
import io.github.rainboom7.cosmetics.ui.theme.Spacing
import io.github.rainboom7.cosmetics.ui.theme.tone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun DetailRoute(id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val viewModel = appViewModel(key = "detail-$id") { DetailViewModel(id, it.collection, it.photoStorage, it.usage) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.AddedToWishlist -> Toast.makeText(context, R.string.added_to_wishlist, Toast.LENGTH_SHORT).show()
                DetailEvent.MovedToCollection -> Toast.makeText(context, R.string.wishlist_moved, Toast.LENGTH_SHORT).show()
                DetailEvent.Deleted -> onBack()
            }
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.addPhoto(uri)
    }
    when (val s = state) {
        DetailState.Loading -> Box(Modifier.fillMaxSize())
        DetailState.Missing -> EmptyState(stringResource(R.string.item_missing), "", actionLabel = stringResource(R.string.action_back), onAction = onBack)
        is DetailState.Loaded -> DetailScreen(
            item = s.item,
            today = s.today,
            usage = s.usage,
            onBack = onBack,
            onEdit = { onEdit(id) },
            onDelete = viewModel::delete,
            onToggleWantBadly = viewModel::toggleWantBadly,
            onUsage = viewModel::setUsage,
            onRemaining = viewModel::setRemaining,
            onOpenedOn = viewModel::setOpenedOn,
            onAddPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onBuyAgain = viewModel::buyAgain,
            onBought = viewModel::markBought,
            onShelf = viewModel::toggleShelf,
            onToggleFavorite = viewModel::toggleFavorite,
            onToggleUsedToday = viewModel::toggleUsedToday,
        )
    }
}

@Composable
fun DetailScreen(
    item: CosmeticItem,
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleWantBadly: () -> Unit,
    onUsage: (UsageState) -> Unit,
    onRemaining: (Int) -> Unit,
    onOpenedOn: (LocalDate) -> Unit,
    onAddPhoto: () -> Unit,
    onBuyAgain: () -> Unit,
    onBought: () -> Unit,
    usage: ItemUsage = ItemUsage(),
    onShelf: (ShelfStatus) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onToggleUsedToday: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box {
                PhotoPager(item)
                TopButtons(item, onBack, onEdit, onDelete, onToggleWantBadly, onToggleFavorite)
            }
            Column(Modifier.padding(horizontal = Spacing.screen, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(Spacing.gap)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(item.name, style = MaterialTheme.typography.headlineSmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftTag(categoryLabel(item.category))
                    item.quantity?.let { SoftTag(it) }
                    item.shade?.let { SoftTag(it) }
                    item.periodAfterOpeningMonths?.let { SoftTag(stringResource(R.string.pao_months, it)) }
                }
                if (item.status == ItemStatus.OWNED) {
                    MyCopy(item, today, onUsage, onRemaining, onOpenedOn)
                    if (item.usage != UsageState.FINISHED) StatusSection(item, onShelf)
                    UsageSection(usage, enabled = item.usage != UsageState.FINISHED, onToggleUsedToday)
                } else {
                    WishInfo(item)
                }
                if (item.notes.isNotBlank() && item.status == ItemStatus.OWNED) {
                    Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                }
                item.ingredients?.let { Ingredients(it) }
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.screen, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onAddPhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_add_photo), modifier = Modifier.padding(start = 8.dp))
            }
            if (item.status == ItemStatus.OWNED) {
                Button(onClick = onBuyAgain, modifier = Modifier.weight(1f).testTag("buy_again")) { Text(stringResource(R.string.action_buy_again)) }
            } else {
                Button(onClick = onBought, modifier = Modifier.weight(1f).testTag("bought")) { Text(stringResource(R.string.action_bought)) }
            }
        }
    }
}

@Composable
private fun PhotoPager(item: CosmeticItem) {
    val images = item.images.ifEmpty { listOf(null) }
    val pager = rememberPagerState { images.size }
    Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
        HorizontalPager(pager, modifier = Modifier.fillMaxSize()) { page ->
            val model = images[page]
            ItemImage(
                model = model,
                category = item.category,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                iconSize = 72.dp,
                attribution = model != null && model == item.catalogImageUrl,
            )
        }
        if (images.size > 1) {
            Row(Modifier.align(Alignment.BottomCenter).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(images.size) { index ->
                    val color = if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun TopButtons(
    item: CosmeticItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleWantBadly: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
        RoundButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
        Box(Modifier.weight(1f))
        if (item.status == ItemStatus.WISHLIST) {
            RoundButton(onToggleWantBadly) {
                Icon(
                    if (item.wantBadly) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.want_badly),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            RoundButton(onToggleFavorite) {
                Icon(
                    if (item.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(if (item.favorite) R.string.action_unfavorite else R.string.action_favorite),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("favorite_button"),
                )
            }
        }
        Box {
            RoundButton({ menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menu = false; confirmDelete = true })
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(item.name) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun RoundButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), modifier = Modifier.padding(4.dp)) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun MyCopy(
    item: CosmeticItem,
    today: LocalDate,
    onUsage: (UsageState) -> Unit,
    onRemaining: (Int) -> Unit,
    onOpenedOn: (LocalDate) -> Unit,
) {
    var pickDate by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.my_copy), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                UsageState.entries.forEachIndexed { index, usage ->
                    SegmentedButton(
                        selected = item.usage == usage,
                        onClick = { onUsage(usage) },
                        shape = SegmentedButtonDefaults.itemShape(index, UsageState.entries.size),
                        label = { Text(usageLabel(usage)) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().clickable { pickDate = true }.padding(vertical = 4.dp)) {
                Text(stringResource(R.string.opened_date), modifier = Modifier.weight(1f))
                Text(item.openedOn?.format(FullDate) ?: stringResource(R.string.not_set), color = MaterialTheme.colorScheme.primary)
            }
            item.expiresOn()?.let { expires ->
                val urgent = item.expiryStatus(today).let { it == ExpiryStatus.SOON || it == ExpiryStatus.EXPIRED }
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.good_until), modifier = Modifier.weight(1f))
                    Text(
                        expires.format(FullDate),
                        color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("good_until"),
                    )
                }
            }
            if (item.usage == UsageState.OPENED) {
                var remaining by remember(item.remainingPercent) { mutableFloatStateOf(item.remainingPercent.toFloat()) }
                Text(stringResource(R.string.remaining_percent, remaining.toInt()))
                Slider(
                    value = remaining,
                    onValueChange = { remaining = it },
                    onValueChangeFinished = { onRemaining(remaining.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                )
            }
        }
    }
    if (pickDate) {
        val initial = (item.openedOn ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickDate = false
                    pickerState.selectedDateMillis?.let { onOpenedOn(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(pickerState)
        }
    }
}

@Composable
private fun WishInfo(item: CosmeticItem) {
    val uriHandler = LocalUriHandler.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.wantBadly) Text(stringResource(R.string.want_badly), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            item.price?.let { Text(stringResource(R.string.price_value, it)) }
            item.link?.let { link ->
                Text(
                    link,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { runCatching { uriHandler.openUri(link) } },
                )
            }
            if (item.notes.isNotBlank()) Text(item.notes)
            if (item.price == null && item.link == null && item.notes.isBlank() && !item.wantBadly) {
                Text(stringResource(R.string.wish_no_details), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Ingredients(text: String) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ingredients), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(open) { Text(text, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SoftTag(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** "Статус" chips: pastel per status; the automatic "Заканчивается" shows as selected but unset. */
@Composable
private fun StatusSection(item: CosmeticItem, onShelf: (ShelfStatus) -> Unit) {
    val effective = item.effectiveShelfStatus()
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.shelf_title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("shelf_chips")) {
                ShelfStatus.entries.forEach { shelf ->
                    val tone = shelf.tone()
                    FilterChip(
                        selected = effective == shelf,
                        onClick = { onShelf(shelf) },
                        label = { Text(shelfLabel(shelf)) },
                        shape = CircleShape,
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = tone.tone,
                            selectedLabelColor = tone.ink,
                        ),
                    )
                }
            }
            if (item.shelfStatus != null) {
                Text(stringResource(R.string.shelf_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "Использование": mark today, last use and how often in the last 30 days. */
@Composable
private fun UsageSection(usage: ItemUsage, enabled: Boolean, onToggleUsedToday: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.usage_section), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                usage.lastUsed?.let { stringResource(R.string.usage_last, it.format(DayMonthWords)) } ?: stringResource(R.string.usage_never),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (usage.last30Days > 0) {
                Text(
                    pluralStringResource(R.plurals.usage_last30, usage.last30Days, usage.last30Days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled) {
                if (usage.usedToday) {
                    Button(
                        onClick = onToggleUsedToday,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("used_today"),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.usage_marked_today), modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    OutlinedButton(onClick = onToggleUsedToday, modifier = Modifier.fillMaxWidth().testTag("used_today")) {
                        Text(stringResource(R.string.usage_mark_today))
                    }
                }
            }
        }
    }
}

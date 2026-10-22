package io.github.rainboom7.cosmetics.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.ui.components.DayMonthWords
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.Russian
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.monthTitle
import io.github.rainboom7.cosmetics.ui.theme.Candy
import io.github.rainboom7.cosmetics.ui.theme.Spacing
import io.github.rainboom7.cosmetics.ui.theme.tone
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun CalendarRoute(onOpenItem: (Long) -> Unit) {
    val viewModel = appViewModel { CalendarViewModel(it.usage, it.collection) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSelect = viewModel::select,
        onSetUsed = viewModel::setUsed,
        onOpenItem = onOpenItem,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onSetUsed: (Long, Boolean) -> Unit,
    onOpenItem: (Long) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.gap),
        modifier = Modifier.fillMaxSize().testTag("calendar"),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.tab_calendar), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.calendar_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { MonthCard(state, onPreviousMonth, onNextMonth, onSelect) }
        item { StatsRow(state) }
        item {
            DayCard(state, onAdd = { picking = true }, onRemove = { onSetUsed(it, false) }, onOpenItem = onOpenItem)
        }
        if (state.usage.mostUsed.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.calendar_most_used)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.usage.mostUsed, key = { "top-" + it.first.id }) { (item, days) ->
                        TopItemCard(item, days, onClick = { onOpenItem(item.id) })
                    }
                }
            }
        }
        if (state.usage.forgotten.isNotEmpty()) {
            item { ForgottenCard(state.usage.forgotten, onOpenItem) }
        }
    }
    if (picking) {
        PickSheet(state, onSetUsed, onDismiss = { picking = false })
    }
}

/** Month grid on a blush → peach → lilac gradient; days glow pinker the more was used. */
@Composable
private fun MonthCard(state: CalendarUiState, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onSelect: (LocalDate) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Candy.header)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArrowButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.calendar_prev), enabled = true, onPreviousMonth)
                Text(
                    monthTitle(state.month),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Candy.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).testTag("month_title"),
                )
                ArrowButton(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.calendar_next), enabled = state.canGoForward, onNextMonth)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.values().forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.SHORT_STANDALONE, Russian).replaceFirstChar { it.titlecase(Russian) },
                        style = MaterialTheme.typography.labelMedium,
                        color = Candy.ink.copy(alpha = if (day.value >= 6) 0.9f else 0.6f),
                        fontWeight = if (day.value >= 6) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            monthWeeks(state.month).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) DayCell(date, state, onSelect)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.7f), contentColor = Candy.ink, modifier = Modifier.size(40.dp).alpha(if (enabled) 1f else 0.4f)) {
        IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = description) }
    }
}

@Composable
private fun DayCell(date: LocalDate, state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val selected = date == state.selected
    val isToday = date == state.today
    val future = date.isAfter(state.today)
    val used = state.usage.days[date].orEmpty().size
    val colors = MaterialTheme.colorScheme
    val background = when {
        selected -> colors.primary
        used > 0 -> Candy.heat[(used - 1).coerceAtMost(Candy.heat.lastIndex)]
        else -> Color.White.copy(alpha = 0.55f)
    }
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (isToday && !selected) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clickable(enabled = !future) { onSelect(date) }
            .alpha(if (future) 0.35f else 1f)
            .testTag("day_${date.dayOfMonth}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (used > 0 || selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onPrimary else Candy.ink,
        )
        if (used > 0 && !selected) {
            // A tiny heart marks days with care, on top of the pink heat.
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = colors.primary.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(7.dp),
            )
        }
    }
}

/** Streak, days with care this month, empties this year: big numbers on candy gradients. */
@Composable
private fun StatsRow(state: CalendarUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        StatTile(
            number = state.usage.streak,
            label = pluralStringResource(R.plurals.calendar_streak_label, state.usage.streak),
            icon = Icons.Outlined.Whatshot,
            brush = Candy.streak,
            modifier = Modifier.weight(1f).testTag("streak"),
        )
        StatTile(
            number = state.usage.activeDays,
            label = pluralStringResource(R.plurals.calendar_active_label, state.usage.activeDays),
            icon = Icons.Outlined.EventAvailable,
            brush = Candy.days,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            number = state.empties,
            label = pluralStringResource(R.plurals.calendar_empties_label, state.empties),
            icon = Icons.Outlined.Spa,
            brush = Candy.empties,
            modifier = Modifier.weight(1f).testTag("empties"),
        )
    }
}

@Composable
private fun StatTile(number: Int, label: String, icon: ImageVector, brush: Brush, modifier: Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .heightIn(min = 104.dp)
            .clip(MaterialTheme.shapes.large)
            .background(brush)
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Candy.ink.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopEnd).size(20.dp))
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(number.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = Candy.ink)
            Text(label, style = MaterialTheme.typography.labelMedium, color = Candy.ink.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun DayCard(state: CalendarUiState, onAdd: () -> Unit, onRemove: (Long) -> Unit, onOpenItem: (Long) -> Unit) {
    val items = state.selectedItems
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.selected.format(DayMonthWords), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("selected_day"))
                if (state.selected == state.today) {
                    Surface(shape = CircleShape, color = Candy.blush, contentColor = Candy.ink) {
                        Text(stringResource(R.string.calendar_today), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (items.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.calendar_day_count, items.size, items.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (items.isEmpty()) {
                Text(stringResource(R.string.calendar_day_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.forEach { item ->
                val tone = item.category.tone()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(tone.tone.copy(alpha = 0.55f))
                        .clickable { onOpenItem(item.id) }
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                        .testTag("day_item"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ItemImage(item.coverImage, item.category, contentDescription = null, modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small), iconSize = 20.dp)
                    ItemTitle(item, Modifier.weight(1f))
                    IconButton(onClick = { onRemove(item.id) }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_delete), tint = tone.ink)
                    }
                }
            }
            Button(
                onClick = onAdd,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().testTag("mark_usage"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.calendar_mark))
            }
        }
    }
}

@Composable
private fun TopItemCard(item: CosmeticItem, days: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .width(128.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            ItemImage(item.coverImage, item.category, contentDescription = null, modifier = Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.medium), iconSize = 28.dp)
            Surface(shape = CircleShape, color = Candy.blush, contentColor = Candy.ink, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                Text(
                    pluralStringResource(R.plurals.calendar_times, days, days),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(item.name, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun ForgottenCard(items: List<CosmeticItem>, onOpenItem: (Long) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(Candy.peach, Candy.butter)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.calendar_forgotten_title), style = MaterialTheme.typography.titleMedium, color = Candy.ink)
        Text(stringResource(R.string.calendar_forgotten_body), style = MaterialTheme.typography.bodySmall, color = Candy.ink.copy(alpha = 0.75f))
        items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.White.copy(alpha = 0.6f))
                    .clickable { onOpenItem(item.id) }
                    .padding(8.dp)
                    .testTag("forgotten_item"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ItemImage(item.coverImage, item.category, contentDescription = null, modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small), iconSize = 18.dp)
                ItemTitle(item, Modifier.weight(1f), ink = Candy.ink)
            }
        }
    }
}

@Composable
private fun PickSheet(state: CalendarUiState, onSetUsed: (Long, Boolean) -> Unit, onDismiss: () -> Unit) {
    val marked = state.selectedItems.mapTo(HashSet()) { it.id }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.calendar_pick_title, state.selected.format(DayMonthWords)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            if (state.markable.isEmpty()) {
                Text(
                    stringResource(R.string.calendar_nothing_owned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(state.markable, key = { it.id }) { item ->
                    val checked = item.id in marked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (checked) Candy.blush else Color.Transparent)
                            .clickable { onSetUsed(item.id, !checked) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("pick_item"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ItemImage(item.coverImage, item.category, contentDescription = null, modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small), iconSize = 20.dp)
                        ItemTitle(item, Modifier.weight(1f))
                        Checkbox(checked = checked, onCheckedChange = { onSetUsed(item.id, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ItemTitle(item: CosmeticItem, modifier: Modifier = Modifier, ink: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        item.brand?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (item.favorite) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            }
        }
    }
}

/** The month as weeks Monday..Sunday; days outside the month are null. */
fun monthWeeks(month: YearMonth): List<List<LocalDate?>> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val days: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    return days.chunked(7).map { week -> week + List(7 - week.size) { null } }
}

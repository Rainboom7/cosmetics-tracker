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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import io.github.rainboom7.cosmetics.ui.theme.Spacing
import io.github.rainboom7.cosmetics.ui.theme.dot
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
            items(state.usage.mostUsed, key = { "top-" + it.first.id }) { (item, days) ->
                ItemRow(item, onClick = { onOpenItem(item.id) }) {
                    Text(pluralStringResource(R.plurals.calendar_times, days, days), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (state.usage.forgotten.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle(stringResource(R.string.calendar_forgotten_title))
                    Text(stringResource(R.string.calendar_forgotten_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.usage.forgotten, key = { "forgotten-" + it.id }) { item ->
                ItemRow(item, onClick = { onOpenItem(item.id) }, modifier = Modifier.testTag("forgotten_item"))
            }
        }
    }
    if (picking) {
        val marked = state.selectedItems.mapTo(HashSet()) { it.id }
        ModalBottomSheet(onDismissRequest = { picking = false }) {
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
                                .clickable { onSetUsed(item.id, !checked) }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
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
}

@Composable
private fun MonthCard(state: CalendarUiState, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onSelect: (LocalDate) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_prev))
                }
                Text(
                    monthTitle(state.month),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).testTag("month_title"),
                )
                IconButton(onClick = onNextMonth, enabled = state.canGoForward) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.values().forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.SHORT_STANDALONE, Russian).replaceFirstChar { it.titlecase(Russian) },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun DayCell(date: LocalDate, state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val selected = date == state.selected
    val isToday = date == state.today
    val future = date.isAfter(state.today)
    val items = state.usage.days[date].orEmpty()
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> colors.primary
                    items.isNotEmpty() -> colors.primaryContainer
                    else -> colors.surfaceContainerLowest
                },
            )
            .then(if (isToday && !selected) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier)
            .clickable(enabled = !future) { onSelect(date) }
            .alpha(if (future) 0.35f else 1f)
            .testTag("day_${date.dayOfMonth}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
        if (items.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                items.take(3).forEach { item ->
                    Box(Modifier.size(4.dp).background(if (selected) colors.onPrimary else item.category.dot, CircleShape))
                }
            }
        }
    }
}

/** Streak, active days, empties: three soft tiles. */
@Composable
private fun StatsRow(state: CalendarUiState) {
    // Equal heights: the tallest label (often "N пустых баночек…") sets the row height.
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        StatTile(
            pluralStringResource(R.plurals.calendar_streak, state.usage.streak, state.usage.streak),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Modifier.weight(1f),
        )
        StatTile(
            pluralStringResource(R.plurals.calendar_active_days, state.usage.activeDays, state.usage.activeDays),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Modifier.weight(1f),
        )
        StatTile(
            pluralStringResource(R.plurals.calendar_empties, state.empties, state.empties),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Modifier.weight(1f).testTag("empties"),
        )
    }
}

@Composable
private fun StatTile(text: String, container: androidx.compose.ui.graphics.Color, ink: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(color = container, contentColor = ink, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxHeight().heightIn(min = 76.dp)) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.CenterStart) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DayCard(state: CalendarUiState, onAdd: () -> Unit, onRemove: (Long) -> Unit, onOpenItem: (Long) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.selected.format(DayMonthWords), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("selected_day"))
            if (state.selectedItems.isEmpty()) {
                Text(stringResource(R.string.calendar_day_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.selectedItems.forEach { item ->
                ItemRow(item, onClick = { onOpenItem(item.id) }, modifier = Modifier.testTag("day_item")) {
                    IconButton(onClick = { onRemove(item.id) }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("mark_usage")) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.calendar_mark))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ItemRow(item: CosmeticItem, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemImage(item.coverImage, item.category, contentDescription = null, modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small), iconSize = 22.dp)
        ItemTitle(item, Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ItemTitle(item: CosmeticItem, modifier: Modifier = Modifier) {
    Column(modifier) {
        item.brand?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
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

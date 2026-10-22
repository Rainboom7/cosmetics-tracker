package io.github.rainboom7.cosmetics.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.Condition
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemBadge
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.ui.components.BadgeLabel
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.ConditionPill
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel
import io.github.rainboom7.cosmetics.ui.components.openedLine
import io.github.rainboom7.cosmetics.ui.components.shelfLabel
import io.github.rainboom7.cosmetics.ui.theme.Candy
import io.github.rainboom7.cosmetics.ui.theme.Spacing
import io.github.rainboom7.cosmetics.ui.theme.tone
import io.github.rainboom7.cosmetics.core.model.UsageState
import java.time.LocalDate

@Composable
fun CollectionRoute(
    onOpenItem: (Long) -> Unit,
    onAdd: () -> Unit,
    onPhotoSearch: () -> Unit,
    onAbout: () -> Unit,
) {
    val viewModel = appViewModel { CollectionViewModel(it.collection) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectionScreen(
        state = state,
        onTextChange = viewModel::setText,
        onCategory = viewModel::setCategory,
        onOpenItem = onOpenItem,
        onAdd = onAdd,
        onPhotoSearch = onPhotoSearch,
        onAbout = onAbout,
        onShelf = viewModel::toggleShelf,
        onFavoritesOnly = viewModel::toggleFavoritesOnly,
    )
}

@Composable
fun CollectionScreen(
    state: CollectionUiState,
    onTextChange: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onOpenItem: (Long) -> Unit,
    onAdd: () -> Unit,
    onPhotoSearch: () -> Unit,
    onAbout: () -> Unit,
    onShelf: (ShelfStatus) -> Unit = {},
    onFavoritesOnly: () -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = 20.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gap),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap),
            modifier = Modifier.fillMaxSize().padding(padding).testTag("collection_grid"),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tab_collection), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchField(
                    value = state.query.text,
                    onValueChange = onTextChange,
                    placeholder = stringResource(R.string.collection_search_hint),
                    trailing = {
                        IconButton(onClick = onPhotoSearch) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = stringResource(R.string.action_photo_search))
                        }
                    },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryBubbles(state, onCategory)
                    StatusChips(state, onShelf, onFavoritesOnly)
                }
            }
            if (state.expiringSoon > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = MaterialTheme.shapes.large) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).testTag("expiring_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.HourglassBottom, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                pluralStringResource(R.plurals.expiring_banner, state.expiringSoon, state.expiringSoon),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (!state.loading && state.totalCount == 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(R.string.collection_empty_title),
                        body = stringResource(R.string.collection_empty_body),
                        actionLabel = stringResource(R.string.action_add),
                        onAction = onAdd,
                    )
                }
            } else if (!state.loading && state.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(title = stringResource(R.string.nothing_found), body = stringResource(R.string.nothing_found_body))
                }
            }
            items(state.items, key = { it.id }) { item ->
                CollectionCard(item, state.today, onClick = { onOpenItem(item.id) })
            }
        }
    }
}

private fun categoryIcon(category: Category?): ImageVector = when (category) {
    null -> Icons.Outlined.AutoAwesome
    Category.SKINCARE -> Icons.Outlined.Spa
    Category.MAKEUP -> Icons.Outlined.Brush
    Category.HAIR -> Icons.Outlined.ContentCut
    Category.BODY -> Icons.Outlined.Bathtub
    Category.FRAGRANCE -> Icons.Outlined.LocalFlorist
    Category.NAILS -> Icons.Outlined.Palette
    Category.OTHER -> Icons.Outlined.MoreHoriz
}

/**
 * Categories as big round pastel bubbles with an icon, name and counter: easy to hit and to tell
 * apart at a glance. Tapping the selected bubble again returns to "Все"; empty categories fade.
 */
@Composable
private fun CategoryBubbles(state: CollectionUiState, onCategory: (Category?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("category_bubbles")) {
        items(CollectionViewModel.FILTER_CATEGORIES) { category ->
            val selected = state.query.category == category
            val count = if (category == null) state.totalCount else state.categoryCounts[category] ?: 0
            val (tone, ink) = if (category == null) Candy.blush to Candy.ink else category.tone().let { it.tone to it.ink }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .width(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onCategory(category) }
                    .padding(vertical = 4.dp)
                    .alpha(if (count == 0 && !selected) 0.5f else 1f)
                    .testTag("category_" + (category?.name ?: "ALL")),
            ) {
                Box(Modifier.size(64.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(tone)
                            .then(if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(categoryIcon(category), contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
                    }
                    if (count > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            shadowElevation = 1.dp,
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Text(count.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                }
                Text(
                    if (category == null) stringResource(R.string.filter_all) else categoryLabel(category),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * "♡ Любимое" and every status as soft pills with a colored dot and counter. Selected pills fill
 * with their pastel; tapping again clears. Statuses with no items stay visible but faded.
 */
@Composable
private fun StatusChips(state: CollectionUiState, onShelf: (ShelfStatus) -> Unit, onFavoritesOnly: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("status_chips")) {
        item {
            FilterPill(
                label = stringResource(R.string.filter_favorites),
                count = state.favoriteCount,
                selected = state.query.favoritesOnly,
                tone = Candy.blush,
                ink = Candy.ink,
                onClick = onFavoritesOnly,
                icon = if (state.query.favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            )
        }
        items(ShelfStatus.entries) { shelf ->
            val tone = shelf.tone()
            FilterPill(
                label = shelfLabel(shelf),
                count = state.shelfCounts[shelf] ?: 0,
                selected = state.query.shelf == shelf,
                tone = tone.tone,
                ink = tone.ink,
                onClick = { onShelf(shelf) },
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    tone: Color,
    ink: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) tone else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) ink else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(36.dp).alpha(if (count == 0 && !selected) 0.55f else 1f),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected) ink else MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            } else {
                Box(Modifier.size(8.dp).background(if (selected) ink else tone, CircleShape).border(1.dp, ink.copy(alpha = 0.35f), CircleShape))
            }
            Text(stringResource(R.string.shelf_count, label, count), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = { Icon(Icons.Outlined.Search, contentDescription = null) },
    trailing: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp).testTag("search_field"),
    )
}

@Composable
private fun CollectionCard(item: CosmeticItem, today: LocalDate, onClick: () -> Unit) {
    val badge = item.badge(today)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.testTag("item_card"),
    ) {
        // The photo sits inside the card with a margin, like a print on a mat: lighter than edge-to-edge.
        Box(Modifier.padding(8.dp)) {
            ItemImage(
                model = item.coverImage,
                category = item.category,
                contentDescription = item.name,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(MaterialTheme.shapes.medium),
            )
            badge?.let { BadgeLabel(it, Modifier.align(Alignment.TopStart).padding(8.dp)) }
            if (item.favorite) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.filter_favorites), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item.brand?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // New and opened are the default and need no pill; the badge already says "Заканчивается".
            item.condition()
                ?.takeUnless { it == Condition.NEW || it == Condition.OPENED }
                ?.takeUnless { it == Condition.RUNNING_LOW && badge == ItemBadge.RUNNING_LOW }
                ?.let { ConditionPill(it, Modifier.padding(top = 2.dp)) }
            if (item.usage == UsageState.OPENED) {
                Spacer(Modifier.size(2.dp))
                LinearProgressIndicator(
                    progress = { item.remainingPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
            }
            openedLine(item.openedOn, item.expiresOn())?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

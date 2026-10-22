package io.github.rainboom7.cosmetics.ui.collection

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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemBadge
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.ui.components.BadgeLabel
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.StatusPill
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel
import io.github.rainboom7.cosmetics.ui.components.openedLine
import io.github.rainboom7.cosmetics.ui.components.shelfLabel
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CollectionViewModel.FILTER_CATEGORIES) { category ->
                            FilterChip(
                                selected = state.query.category == category,
                                onClick = { onCategory(category) },
                                label = {
                                    Text(if (category == null) stringResource(R.string.filter_all_count, state.totalCount) else categoryLabel(category))
                                },
                                shape = CircleShape,
                                colors = softChipColors(),
                                border = null,
                            )
                        }
                    }
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

/** Unselected chips are soft filled pills without outlines; selected ones use the pastel primary. */
@Composable
fun softChipColors(
    selectedContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedLabel: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = selectedContainer,
    selectedLabelColor = selectedLabel,
    selectedLeadingIconColor = selectedLabel,
)

/** "♡ Любимое" and one chip per status that has items; hidden while nothing is marked. */
@Composable
private fun StatusChips(state: CollectionUiState, onShelf: (ShelfStatus) -> Unit, onFavoritesOnly: () -> Unit) {
    val shelves = ShelfStatus.entries.filter { (state.shelfCounts[it] ?: 0) > 0 || state.query.shelf == it }
    val showFavorites = state.favoriteCount > 0 || state.query.favoritesOnly
    if (shelves.isEmpty() && !showFavorites) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("status_chips")) {
        if (showFavorites) {
            item {
                FilterChip(
                    selected = state.query.favoritesOnly,
                    onClick = onFavoritesOnly,
                    label = { Text(stringResource(R.string.shelf_count, stringResource(R.string.filter_favorites), state.favoriteCount)) },
                    leadingIcon = {
                        Icon(
                            if (state.query.favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    shape = CircleShape,
                    colors = softChipColors(),
                    border = null,
                )
            }
        }
        items(shelves) { shelf ->
            val tone = shelf.tone()
            FilterChip(
                selected = state.query.shelf == shelf,
                onClick = { onShelf(shelf) },
                label = { Text(stringResource(R.string.shelf_count, shelfLabel(shelf), state.shelfCounts[shelf] ?: 0)) },
                shape = CircleShape,
                colors = softChipColors(selectedContainer = tone.tone, selectedLabel = tone.ink),
                border = null,
            )
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
            // The badge already says "Заканчивается"; don't repeat it as a pill.
            item.effectiveShelfStatus()
                ?.takeUnless { it == ShelfStatus.RUNNING_LOW && badge == ItemBadge.RUNNING_LOW }
                ?.let { StatusPill(it, Modifier.padding(top = 2.dp)) }
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

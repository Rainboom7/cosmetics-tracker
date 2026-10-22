package io.github.rainboom7.cosmetics.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.collection.WishlistFilter
import io.github.rainboom7.cosmetics.ui.collection.HeartButton
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.bleed
import io.github.rainboom7.cosmetics.ui.components.cloudShadow
import io.github.rainboom7.cosmetics.ui.components.cloudSky
import io.github.rainboom7.cosmetics.ui.theme.Candy
import io.github.rainboom7.cosmetics.ui.theme.Spacing

@Composable
fun WishlistRoute(onOpenItem: (Long) -> Unit, onOpenCatalog: () -> Unit) {
    val viewModel = appViewModel { WishlistViewModel(it.collection) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val movedText = stringResource(R.string.wishlist_moved)
    val undoText = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.bought.collect { before ->
            val result = snackbar.showSnackbar(movedText, actionLabel = undoText, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(before)
        }
    }
    WishlistScreen(state, snackbar, viewModel::setFilter, viewModel::markBought, onOpenItem, onOpenCatalog, viewModel::toggleWantBadly)
}

/** "Желаемое" as a shop shelf: cloud header, filter pills and a two-column grid with prices. */
@Composable
fun WishlistScreen(
    state: WishlistUiState,
    snackbar: SnackbarHostState,
    onFilter: (WishlistFilter) -> Unit,
    onBought: (Long) -> Unit,
    onOpenItem: (Long) -> Unit,
    onOpenCatalog: () -> Unit,
    onToggleWantBadly: (Long) -> Unit = {},
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCatalog,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_find_in_catalog))
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gap),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .bleed(Spacing.screen)
                        .cloudSky(MaterialTheme.colorScheme.background)
                        .padding(top = 16.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(Modifier.padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.tab_wishlist), style = MaterialTheme.typography.headlineMedium, color = Candy.ink)
                            Text(state.totalCount.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(stringResource(R.string.wishlist_subtitle), style = MaterialTheme.typography.bodyMedium, color = Candy.ink.copy(alpha = 0.7f))
                    }
                    FilterPills(state.filter, onFilter)
                }
            }
            if (!state.loading && state.rows.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(R.string.wishlist_empty_title),
                        body = stringResource(R.string.wishlist_empty_body),
                        actionLabel = stringResource(R.string.action_find_in_catalog),
                        onAction = onOpenCatalog,
                    )
                }
            }
            items(state.rows, key = { it.item.id }) { row ->
                WishCard(
                    row,
                    onClick = { onOpenItem(row.item.id) },
                    onBought = { onBought(row.item.id) },
                    onToggleWantBadly = { onToggleWantBadly(row.item.id) },
                )
            }
        }
    }
}

@Composable
private fun FilterPills(selected: WishlistFilter, onFilter: (WishlistFilter) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(WishlistFilter.entries) { filter ->
            val isSelected = selected == filter
            Surface(
                onClick = { onFilter(filter) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Candy.ink,
                modifier = Modifier.height(38.dp),
            ) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(
                            when (filter) {
                                WishlistFilter.ALL -> R.string.filter_all
                                WishlistFilter.WANT_BADLY -> R.string.want_badly
                                WishlistFilter.REBUY -> R.string.rebuy
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Shop card: photo with a heart ("Хочу сильно"), brand, name, price in bold and a pink "Купила". */
@Composable
private fun WishCard(row: WishRow, onClick: () -> Unit, onBought: () -> Unit, onToggleWantBadly: () -> Unit) {
    val item = row.item
    val shape = MaterialTheme.shapes.large
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.cloudShadow(shape, 3.dp).testTag("wish_row"),
    ) {
        Column {
            Box(Modifier.padding(6.dp)) {
                ItemImage(item.coverImage, item.category, item.name, Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium))
                if (item.rebuy) {
                    Surface(color = Candy.butter, contentColor = Candy.ink, shape = CircleShape, modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                        Text(stringResource(R.string.rebuy), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                HeartButton(item.wantBadly, onToggleWantBadly, Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.brand?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 40.dp))
                item.price?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (row.alreadyOwned) {
                    Text(stringResource(R.string.similar_owned), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                if (item.wantBadly) {
                    Surface(color = Candy.blush, contentColor = Candy.ink, shape = CircleShape) {
                        Text(stringResource(R.string.want_badly), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    onClick = onBought,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("bought_button"),
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.wishlist_bought_short), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

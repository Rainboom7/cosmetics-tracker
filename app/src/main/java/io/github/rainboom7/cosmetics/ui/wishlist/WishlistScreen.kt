package io.github.rainboom7.cosmetics.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.collection.WishlistFilter
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel

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
    WishlistScreen(state, snackbar, viewModel::setFilter, viewModel::markBought, onOpenItem, onOpenCatalog)
}

@Composable
fun WishlistScreen(
    state: WishlistUiState,
    snackbar: SnackbarHostState,
    onFilter: (WishlistFilter) -> Unit,
    onBought: (Long) -> Unit,
    onOpenItem: (Long) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCatalog, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_find_in_catalog))
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tab_wishlist), style = MaterialTheme.typography.headlineMedium)
                    Text(state.totalCount.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WishlistFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onFilter(filter) },
                            label = {
                                Text(
                                    stringResource(
                                        when (filter) {
                                            WishlistFilter.ALL -> R.string.filter_all
                                            WishlistFilter.WANT_BADLY -> R.string.want_badly
                                            WishlistFilter.REBUY -> R.string.rebuy
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            if (!state.loading && state.rows.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.wishlist_empty_title),
                        body = stringResource(R.string.wishlist_empty_body),
                        actionLabel = stringResource(R.string.action_find_in_catalog),
                        onAction = onOpenCatalog,
                    )
                }
            }
            items(state.rows, key = { it.item.id }) { row ->
                WishRowView(row, onClick = { onOpenItem(row.item.id) }, onBought = { onBought(row.item.id) })
            }
        }
    }
}

@Composable
private fun WishRowView(row: WishRow, onClick: () -> Unit, onBought: () -> Unit) {
    val item = row.item
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.testTag("wish_row")) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemImage(item.coverImage, item.category, item.name, Modifier.size(72.dp).clip(MaterialTheme.shapes.medium), iconSize = 28.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (row.alreadyOwned) {
                    Text(stringResource(R.string.similar_owned), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.wantBadly) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(stringResource(R.string.want_badly), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    val meta = listOfNotNull(item.price, item.notes.takeIf { it.isNotBlank() }).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            OutlinedIconButton(onClick = onBought, modifier = Modifier.testTag("bought_button")) {
                Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.action_bought))
            }
        }
    }
}

package io.github.rainboom7.cosmetics.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.collection.KnownBarcodes
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.ui.collection.SearchField
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.appContainer
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel

@Composable
fun CatalogRoute(
    initialQuery: String,
    onScanBarcode: () -> Unit,
    onPhotoSearch: () -> Unit,
    onAddManually: () -> Unit,
    onOpenProduct: () -> Unit,
) {
    val viewModel = appViewModel(key = "catalog") { CatalogViewModel(it.catalog, it.collection, initialQuery) }
    val selection = appContainer().catalogSelection
    val state by viewModel.state.collectAsStateWithLifecycle()
    val known by viewModel.known.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            val text = context.getString(
                when (message) {
                    CatalogMessage.ADDED_COLLECTION -> R.string.added_to_collection
                    CatalogMessage.ADDED_WISHLIST -> R.string.added_to_wishlist
                    CatalogMessage.ALREADY_THERE -> R.string.already_there
                    CatalogMessage.MOVED_FROM_WISHLIST -> R.string.moved_from_wishlist
                },
            )
            snackbar.showSnackbar(text)
        }
    }
    CatalogScreen(
        state = state,
        known = known,
        snackbar = snackbar,
        onQueryChange = viewModel::onQueryChange,
        onCategory = viewModel::setCategory,
        onBrand = viewModel::setBrand,
        onToggleWithPhoto = viewModel::toggleWithPhoto,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onAdd = viewModel::add,
        onScanBarcode = onScanBarcode,
        onPhotoSearch = onPhotoSearch,
        onAddManually = onAddManually,
        onOpen = { product ->
            selection.product = product
            onOpenProduct()
        },
    )
}

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    known: KnownBarcodes,
    snackbar: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onBrand: (String?) -> Unit,
    onToggleWithPhoto: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onAdd: (CatalogProduct, ItemStatus) -> Unit,
    onScanBarcode: () -> Unit,
    onPhotoSearch: () -> Unit,
    onAddManually: () -> Unit,
    onOpen: (CatalogProduct) -> Unit = {},
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= listState.layoutInfo.totalItemsCount - 3) onLoadMore() }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0)) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                SearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.catalog_search_hint),
                    trailing = {
                        Row {
                            IconButton(onClick = onScanBarcode) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = stringResource(R.string.mode_barcode))
                            }
                            IconButton(onClick = onPhotoSearch) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = stringResource(R.string.action_photo_search))
                            }
                        }
                    },
                )
            }
            if (state.results.isNotEmpty()) {
                item { Filters(state, onCategory, onBrand, onToggleWithPhoto) }
            }
            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.error -> item {
                    EmptyState(
                        title = stringResource(R.string.catalog_error_title),
                        body = stringResource(R.string.catalog_error_body),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = onRetry,
                    )
                }
                state.query.isBlank() -> item {
                    EmptyState(title = stringResource(R.string.catalog_intro_title), body = stringResource(R.string.catalog_intro_body))
                }
            }
            items(state.visible, key = { it.barcode ?: it.name }) { product ->
                ProductRow(
                    product = product,
                    owned = product.barcode != null && product.barcode in known.owned,
                    wished = product.barcode != null && product.barcode in known.wishlist,
                    onWish = { onAdd(product, ItemStatus.WISHLIST) },
                    onAdd = { onAdd(product, ItemStatus.OWNED) },
                    onOpen = { onOpen(product) },
                )
            }
            if (state.loadingMore) {
                item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            if (state.nothingFound || (!state.hasMore && state.visible.isNotEmpty())) {
                item { NotInCatalogCard(onAddManually) }
            }
        }
    }
}

@Composable
private fun Filters(
    state: CatalogUiState,
    onCategory: (Category?) -> Unit,
    onBrand: (String?) -> Unit,
    onToggleWithPhoto: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            DropdownChip(
                label = state.category?.let { categoryLabel(it) } ?: stringResource(R.string.filter_category),
                selected = state.category != null,
                options = listOf(null) + Category.entries,
                optionLabel = { it?.let { c -> categoryLabel(c) } ?: stringResource(R.string.filter_all) },
                onSelect = onCategory,
            )
        }
        item {
            DropdownChip(
                label = state.brand ?: stringResource(R.string.filter_brand),
                selected = state.brand != null,
                options = listOf(null) + state.brands,
                optionLabel = { it ?: stringResource(R.string.filter_all) },
                onSelect = onBrand,
            )
        }
        item {
            FilterChip(selected = state.withPhotoOnly, onClick = onToggleWithPhoto, label = { Text(stringResource(R.string.filter_with_photo)) })
        }
    }
}

@Composable
private fun <T> DropdownChip(
    label: String,
    selected: Boolean,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { open = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}

@Composable
private fun ProductRow(product: CatalogProduct, owned: Boolean, wished: Boolean, onWish: () -> Unit, onAdd: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onOpen).testTag("catalog_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemImage(product.imageUrl, product.category, product.name, Modifier.size(56.dp).clip(MaterialTheme.shapes.medium), iconSize = 24.dp)
        Column(Modifier.weight(1f)) {
            product.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            Text(product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (owned) Text(stringResource(R.string.already_in_collection), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            else if (wished) Text(stringResource(R.string.already_in_wishlist), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onWish) { Icon(Icons.Outlined.FavoriteBorder, contentDescription = stringResource(R.string.action_to_wishlist)) }
        FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_to_collection)) }
    }
}

@Composable
fun NotInCatalogCard(onAddManually: () -> Unit) {
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().testTag("not_in_catalog"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.not_in_catalog_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.not_in_catalog_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAddManually) { Text(stringResource(R.string.action_add_manually)) }
        }
    }
}

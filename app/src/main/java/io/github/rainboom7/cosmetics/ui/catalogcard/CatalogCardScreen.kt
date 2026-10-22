package io.github.rainboom7.cosmetics.ui.catalogcard

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.collection.KnownBarcodes
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.price.PriceLinks
import io.github.rainboom7.cosmetics.ui.catalog.CatalogMessage
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel

@Composable
fun CatalogCardRoute(onBack: () -> Unit) {
    val viewModel = appViewModel { CatalogCardViewModel(it.catalogSelection.product, it.catalog, it.collection) }
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
    CatalogCardScreen(state, known, snackbar, onBack, viewModel::add, onOpenShop = { openInBrowserTab(context, it) })
}

@Composable
fun CatalogCardScreen(
    state: CatalogCardUiState,
    known: KnownBarcodes,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: (ItemStatus) -> Unit,
    onOpenShop: (String) -> Unit = {},
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0)) { padding ->
        val product = state.product
        if (product == null) {
            // Lost on process death: the selection is kept in memory only.
            Box(Modifier.fillMaxSize().padding(padding).statusBarsPadding(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.catalog_card_missing),
                    body = "",
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).navigationBarsPadding(),
        ) {
            Box {
                ItemImage(
                    product.imageUrl,
                    product.category,
                    product.name,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    iconSize = 72.dp,
                    attribution = true,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.statusBarsPadding().padding(8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            }
            if (state.loadingDetails) {
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("card_loading"))
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Header(product)
                Actions(product, known, onAdd)
                FindPrice(product, onOpenShop)
                product.details?.genericName?.let { Section(stringResource(R.string.catalog_card_description)) { Text(it, style = MaterialTheme.typography.bodyLarge) } }
                Specs(product)
                product.ingredients?.let { Ingredients(it) }
                Source(product)
            }
        }
    }
}

@Composable
private fun Header(product: CatalogProduct) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        product.brand?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(product.name, style = MaterialTheme.typography.headlineSmall)
        val chips = listOfNotNull(categoryLabel(product.category), product.quantity)
        Text(chips.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Actions(product: CatalogProduct, known: KnownBarcodes, onAdd: (ItemStatus) -> Unit) {
    val owned = product.barcode != null && product.barcode in known.owned
    val wished = product.barcode != null && product.barcode in known.wishlist
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (owned || wished) {
            Text(
                stringResource(if (owned) R.string.already_in_collection else R.string.already_in_wishlist),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onAdd(ItemStatus.OWNED) }, modifier = Modifier.weight(1f).testTag("card_add")) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_to_collection), modifier = Modifier.padding(start = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { onAdd(ItemStatus.WISHLIST) }, modifier = Modifier.weight(1f).testTag("card_wish")) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_to_wishlist), modifier = Modifier.padding(start = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Shop search pages for this product; the app itself sends nothing to the shops. */
@Composable
private fun FindPrice(product: CatalogProduct, onOpenShop: (String) -> Unit) {
    val query = PriceLinks.query(product)
    Section(stringResource(R.string.find_price_title)) {
        Text(stringResource(R.string.find_price_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("find_price")) {
            PriceLinks.shops.forEach { shop ->
                AssistChip(
                    onClick = { onOpenShop(shop.url(query)) },
                    label = { Text(shop.name) },
                    trailingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}

/** Custom Tab on top of the app; any browser when Custom Tabs are not available. */
private fun openInBrowserTab(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (ignored: ActivityNotFoundException) {
            // No browser at all: nothing to open.
        }
    }
}

/** One line per fact the catalog actually has; nothing is shown for missing data. */
@Composable
private fun Specs(product: CatalogProduct) {
    val details = product.details
    val rows = listOfNotNull(
        details?.visibleCategories?.takeIf { it.isNotEmpty() }?.let { R.string.spec_categories to it.joinToString(", ") },
        details?.visibleLabels?.takeIf { it.isNotEmpty() }?.let { R.string.spec_labels to it.joinToString(", ") },
        details?.periodAfterOpening?.let { R.string.spec_pao to it },
        details?.packaging?.let { R.string.spec_packaging to it },
        details?.manufacturingPlaces?.let { R.string.spec_made_in to it },
        details?.origins?.let { R.string.spec_origins to it },
        details?.countries?.let { R.string.spec_countries to it },
        details?.stores?.let { R.string.spec_stores to it },
        product.barcode?.let { R.string.spec_barcode to it },
    )
    if (rows.isEmpty()) return
    Section(stringResource(R.string.catalog_card_specs)) {
        Column(Modifier.testTag("card_specs")) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
                    Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.58f))
                }
            }
        }
    }
}

@Composable
private fun Ingredients(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Section(stringResource(R.string.catalog_card_ingredients)) {
        Column(Modifier.animateContentSize().clickable { expanded = !expanded }) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (text.length > 160) {
                Text(
                    stringResource(if (expanded) R.string.action_collapse else R.string.action_show_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Source(product: CatalogProduct) {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.catalog_card_source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        product.details?.pageUrl?.let { url ->
            TextButton(onClick = { uri.openUri(url) }) {
                Text(stringResource(R.string.catalog_card_open_site))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(16.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

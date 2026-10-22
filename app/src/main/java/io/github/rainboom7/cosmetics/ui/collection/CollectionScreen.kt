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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import io.github.rainboom7.cosmetics.ui.components.BadgeLabel
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.categoryLabel
import io.github.rainboom7.cosmetics.ui.components.openedLine
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
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding).testTag("collection_grid"),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tab_collection), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_title))
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CollectionViewModel.FILTER_CATEGORIES) { category ->
                        FilterChip(
                            selected = state.query.category == category,
                            onClick = { onCategory(category) },
                            label = {
                                Text(if (category == null) stringResource(R.string.filter_all_count, state.totalCount) else categoryLabel(category))
                            },
                        )
                    }
                }
            }
            if (state.expiringSoon > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.large) {
                        Text(
                            pluralStringResource(R.plurals.expiring_banner, state.expiringSoon, state.expiringSoon),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("expiring_banner"),
                        )
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
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp).testTag("search_field"),
    )
}

@Composable
private fun CollectionCard(item: CosmeticItem, today: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.testTag("item_card"),
    ) {
        Box {
            ItemImage(
                model = item.coverImage,
                category = item.category,
                contentDescription = item.name,
                modifier = Modifier.fillMaxWidth().height(132.dp).clip(MaterialTheme.shapes.large),
            )
            item.badge(today)?.let { BadgeLabel(it, Modifier.align(Alignment.TopStart).padding(8.dp)) }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item.brand?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.usage == UsageState.OPENED) {
                Spacer(Modifier.size(2.dp))
                LinearProgressIndicator(progress = { item.remainingPercent / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp))
            }
            openedLine(item.openedOn, item.expiresOn())?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

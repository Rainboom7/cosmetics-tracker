package io.github.rainboom7.cosmetics.ui.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.Condition
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemBadge
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.ui.components.BadgeLabel
import io.github.rainboom7.cosmetics.ui.components.ConditionPill
import io.github.rainboom7.cosmetics.ui.components.EmptyState
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.SectionHeading
import io.github.rainboom7.cosmetics.ui.components.StoryRing
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import io.github.rainboom7.cosmetics.ui.components.bleed
import io.github.rainboom7.cosmetics.ui.components.categoryLabel
import io.github.rainboom7.cosmetics.ui.components.cloudShadow
import io.github.rainboom7.cosmetics.ui.components.cloudSky
import io.github.rainboom7.cosmetics.ui.components.openedLine
import io.github.rainboom7.cosmetics.ui.components.shelfLabel
import io.github.rainboom7.cosmetics.ui.theme.Candy
import io.github.rainboom7.cosmetics.ui.theme.Sky
import io.github.rainboom7.cosmetics.ui.theme.Spacing
import io.github.rainboom7.cosmetics.ui.theme.tone
import java.time.LocalDate

@Composable
fun CollectionRoute(
    onOpenItem: (Long) -> Unit,
    onAdd: () -> Unit,
    onPhotoSearch: () -> Unit,
    onAbout: () -> Unit,
) {
    val viewModel = appViewModel { CollectionViewModel(it.collection, it.usage) }
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
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleUsedToday = viewModel::toggleUsedToday,
    )
}

/**
 * "Моя полочка", laid out like a beauty shop's home (design spec v3): a cloud-sky header with the
 * search and round "stories" for statuses, soft banners, category tiles, then a two-column grid of
 * product cards, each with a heart and a pink "Сегодня" button in place of "В корзину".
 */
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
    onToggleFavorite: (Long) -> Unit = {},
    onToggleUsedToday: (Long) -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add), fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gap),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap),
            modifier = Modifier.fillMaxSize().padding(padding).testTag("collection_grid"),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader(state, onTextChange, onPhotoSearch, onAbout, onShelf, onFavoritesOnly)
            }
            if (state.expiringSoon > 0 || state.runningLow > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) { Banners(state) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeading(stringResource(R.string.section_categories)) }
            item(span = { GridItemSpan(maxLineSpan) }) { CategoryTiles(state, onCategory) }
            if (state.totalCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeading(stringResource(R.string.section_my_items), count = state.items.size)
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
                ProductCard(
                    item = item,
                    today = state.today,
                    usedToday = item.id in state.usedToday,
                    onClick = { onOpenItem(item.id) },
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onToggleUsedToday = { onToggleUsedToday(item.id) },
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: CollectionUiState,
    onTextChange: (String) -> Unit,
    onPhotoSearch: () -> Unit,
    onAbout: () -> Unit,
    onShelf: (ShelfStatus) -> Unit,
    onFavoritesOnly: () -> Unit,
) {
    Column(
        Modifier
            .bleed(Spacing.screen)
            .cloudSky(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.padding(horizontal = Spacing.screen), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium, color = Candy.ink)
                Text(
                    if (state.totalCount > 0) pluralStringResource(R.plurals.home_count, state.totalCount, state.totalCount) else stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Candy.ink.copy(alpha = 0.7f),
                )
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.75f)) {
                IconButton(onClick = onAbout) {
                    Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_title), tint = Candy.ink)
                }
            }
        }
        SearchField(
            value = state.query.text,
            onValueChange = onTextChange,
            placeholder = stringResource(R.string.collection_search_hint),
            modifier = Modifier.padding(horizontal = Spacing.screen),
            trailing = {
                Surface(onClick = onPhotoSearch, shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp).size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = stringResource(R.string.action_photo_search),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            },
        )
        Stories(state, onShelf, onFavoritesOnly)
    }
}

private fun shelfIcon(shelf: ShelfStatus): ImageVector = when (shelf) {
    ShelfStatus.STOCKED -> Icons.Outlined.Inventory2
    ShelfStatus.RUNNING_LOW -> Icons.Outlined.WaterDrop
    ShelfStatus.TRYING -> Icons.Outlined.Science
    ShelfStatus.NOT_USING -> Icons.Outlined.Bedtime
    ShelfStatus.GIVE_AWAY -> Icons.Outlined.CardGiftcard
}

/**
 * Statuses as round "stories", like the shop's promo circles: a gradient ring when the status has
 * items, a pale ring when it is empty, a solid pink ring when it is the active filter.
 */
@Composable
private fun Stories(state: CollectionUiState, onShelf: (ShelfStatus) -> Unit, onFavoritesOnly: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("status_chips"),
    ) {
        item {
            Story(
                label = stringResource(R.string.filter_favorites),
                count = state.favoriteCount,
                selected = state.query.favoritesOnly,
                tone = Candy.blush,
                ink = MaterialTheme.colorScheme.primary,
                icon = Icons.Filled.Favorite,
                tag = "story_FAVORITES",
                onClick = onFavoritesOnly,
            )
        }
        items(ShelfStatus.entries) { shelf ->
            val tone = shelf.tone()
            Story(
                label = shelfLabel(shelf),
                count = state.shelfCounts[shelf] ?: 0,
                selected = state.query.shelf == shelf,
                tone = tone.tone,
                ink = tone.ink,
                icon = shelfIcon(shelf),
                tag = "story_" + shelf.name,
                onClick = { onShelf(shelf) },
            )
        }
    }
}

@Composable
private fun Story(label: String, count: Int, selected: Boolean, tone: Color, ink: Color, icon: ImageVector, tag: String, onClick: () -> Unit) {
    val description = stringResource(R.string.story_count, label, count)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag(tag),
    ) {
        Box {
            StoryRing(active = count > 0, selected = selected, size = 66.dp) {
                Box(Modifier.fillMaxSize().background(tone, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                }
            }
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = Candy.ink.copy(alpha = if (count > 0 || selected) 1f else 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Soft promo-style banners: what expires this month and what is running low. */
@Composable
private fun Banners(state: CollectionUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.expiringSoon > 0) {
            Banner(
                text = pluralStringResource(R.plurals.expiring_banner, state.expiringSoon, state.expiringSoon),
                icon = Icons.Outlined.HourglassBottom,
                brush = Sky.bannerPink,
                modifier = Modifier.testTag("expiring_banner"),
            )
        }
        if (state.runningLow > 0) {
            Banner(
                text = pluralStringResource(R.plurals.banner_running_low, state.runningLow, state.runningLow),
                icon = Icons.Outlined.WaterDrop,
                brush = Sky.bannerPeach,
                modifier = Modifier.testTag("running_low_banner"),
            )
        }
    }
}

@Composable
private fun Banner(text: String, icon: ImageVector, brush: Brush, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(brush)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).background(Color.White.copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Candy.ink, modifier = Modifier.weight(1f))
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
 * Categories as pastel catalog tiles with the icon in a white "cloud" circle, like the shop's
 * illustrated catalog blocks. Tapping the selected tile again returns to "Все"; empty ones fade.
 */
@Composable
private fun CategoryTiles(state: CollectionUiState, onCategory: (Category?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.bleed(Spacing.screen).testTag("category_bubbles"),
    ) {
        items(CollectionViewModel.FILTER_CATEGORIES) { category ->
            val selected = state.query.category == category
            val count = if (category == null) state.totalCount else state.categoryCounts[category] ?: 0
            val (tone, ink) = if (category == null) Candy.blush to Candy.ink else category.tone().let { it.tone to it.ink }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .width(84.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (selected) tone else tone.copy(alpha = 0.55f))
                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large) else Modifier)
                    .clickable { onCategory(category) }
                    .padding(vertical = 12.dp, horizontal = 6.dp)
                    .alpha(if (count == 0 && !selected) 0.5f else 1f)
                    .testTag("category_" + (category?.name ?: "ALL")),
            ) {
                Box(Modifier.size(44.dp).background(Color.White.copy(alpha = 0.85f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(categoryIcon(category), contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                }
                Text(
                    if (category == null) stringResource(R.string.filter_all) else categoryLabel(category),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.7f))
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
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp).cloudShadow(CircleShape, 4.dp).testTag("search_field"),
    )
}

/**
 * Shop-style product card: photo on a white tile with the heart in the corner, brand in caps,
 * name, the item's condition, "осталось N%" where a shop shows the price, and a full-width pink
 * "Сегодня" button where a shop has "В корзину".
 */
@Composable
private fun ProductCard(
    item: CosmeticItem,
    today: LocalDate,
    usedToday: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleUsedToday: () -> Unit,
) {
    val badge = item.badge(today)
    val shape = MaterialTheme.shapes.large
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.cloudShadow(shape, 3.dp).testTag("item_card"),
    ) {
        Column {
            Box(Modifier.padding(6.dp)) {
                ItemImage(
                    model = item.coverImage,
                    category = item.category,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium),
                )
                badge?.let { BadgeLabel(it, Modifier.align(Alignment.TopStart).padding(6.dp)) }
                HeartButton(item.favorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.brand?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 40.dp))
                // New and opened are the default and need no pill; the badge already says "Заканчивается".
                item.condition()
                    ?.takeUnless { it == Condition.NEW || it == Condition.OPENED }
                    ?.takeUnless { it == Condition.RUNNING_LOW && badge == ItemBadge.RUNNING_LOW }
                    ?.let { ConditionPill(it) }
                if (item.usage == UsageState.OPENED) {
                    Text(
                        stringResource(R.string.remaining_short, item.remainingPercent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { item.remainingPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                }
                openedLine(item.openedOn, item.expiresOn())?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.usage != UsageState.FINISHED) {
                    Spacer(Modifier.height(4.dp))
                    TodayButton(usedToday, onToggleUsedToday)
                }
            }
        }
    }
}

@Composable
fun HeartButton(favorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.9f),
        modifier = modifier.size(size).testTag("card_heart"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(if (favorite) R.string.action_unfavorite else R.string.action_favorite),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.53f),
            )
        }
    }
}

/** The card's call to action: solid pink "＋ Сегодня", or a soft pink "✓ Сегодня" once marked. */
@Composable
private fun TodayButton(usedToday: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (usedToday) colors.primaryContainer else colors.primary,
        contentColor = if (usedToday) colors.onPrimaryContainer else colors.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("card_today"),
    ) {
        val description = stringResource(if (usedToday) R.string.card_unmark_today else R.string.card_mark_today)
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (usedToday) Icons.Outlined.Check else Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.card_today), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

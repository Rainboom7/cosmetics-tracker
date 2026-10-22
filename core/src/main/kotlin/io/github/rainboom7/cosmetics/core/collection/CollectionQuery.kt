package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import java.time.LocalDate

enum class SortOrder { NEWEST, NAME, BRAND, EXPIRES_FIRST }

/** Collection screen filter. Collections are small (hundreds of items), so filtering is in memory. */
data class CollectionQuery(
    val text: String = "",
    val category: Category? = null,
    val sort: SortOrder = SortOrder.NEWEST,
)

fun List<CosmeticItem>.applyQuery(query: CollectionQuery): List<CosmeticItem> {
    val needle = query.text.trim().lowercase()
    val filtered = filter { item ->
        (query.category == null || item.category == query.category) && item.matches(needle)
    }
    return when (query.sort) {
        SortOrder.NEWEST -> filtered.sortedByDescending { it.addedAtMillis }
        SortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
        SortOrder.BRAND -> filtered.sortedWith(compareBy(nullsLast()) { it.brand?.lowercase() })
        SortOrder.EXPIRES_FIRST -> filtered.sortedWith(compareBy<CosmeticItem, LocalDate?>(nullsLast()) { it.expiresOn() })
    }
}

/** Wishlist filter chips: Все / Хочу сильно / Купить снова. */
enum class WishlistFilter { ALL, WANT_BADLY, REBUY }

fun List<CosmeticItem>.applyWishlistFilter(filter: WishlistFilter, text: String = ""): List<CosmeticItem> {
    val needle = text.trim().lowercase()
    return filter { item ->
        when (filter) {
            WishlistFilter.ALL -> true
            WishlistFilter.WANT_BADLY -> item.wantBadly
            WishlistFilter.REBUY -> item.rebuy
        } && item.matches(needle)
    }.sortedWith(compareByDescending<CosmeticItem> { it.wantBadly }.thenByDescending { it.addedAtMillis })
}

private fun CosmeticItem.matches(needle: String): Boolean =
    needle.isEmpty() || listOfNotNull(name, brand, notes).any { needle in it.lowercase() }

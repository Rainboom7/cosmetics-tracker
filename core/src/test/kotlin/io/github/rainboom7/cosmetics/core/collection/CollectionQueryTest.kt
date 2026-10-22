package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CollectionQueryTest {
    private val items = listOf(
        CosmeticItem(id = 1, name = "Тушь Lash", brand = "Maybelline", category = Category.MAKEUP, addedAtMillis = 3),
        CosmeticItem(id = 2, name = "Aloe Water", brand = "L'Oréal", category = Category.SKINCARE, addedAtMillis = 1, openedOn = LocalDate.of(2026, 1, 1), periodAfterOpeningMonths = 12),
        CosmeticItem(id = 3, name = "Шампунь", category = Category.HAIR, addedAtMillis = 2, notes = "для сухих волос", openedOn = LocalDate.of(2026, 1, 1), periodAfterOpeningMonths = 6),
    )

    private fun ids(list: List<CosmeticItem>) = list.map { it.id }

    @Test
    fun `default is newest first`() {
        assertEquals(listOf(1L, 3L, 2L), ids(items.applyQuery(CollectionQuery())))
    }

    @Test
    fun `text search covers name, brand and notes, case-insensitively`() {
        assertEquals(listOf(1L), ids(items.applyQuery(CollectionQuery(text = "maybel"))))
        assertEquals(listOf(3L), ids(items.applyQuery(CollectionQuery(text = "СУХИХ"))))
        assertEquals(listOf(2L), ids(items.applyQuery(CollectionQuery(text = " aloe "))))
    }

    @Test
    fun `category filter`() {
        assertEquals(listOf(3L), ids(items.applyQuery(CollectionQuery(category = Category.HAIR))))
    }

    @Test
    fun `sorts`() {
        assertEquals(listOf(2L, 1L, 3L), ids(items.applyQuery(CollectionQuery(sort = SortOrder.NAME))))
        assertEquals(listOf(2L, 1L, 3L), ids(items.applyQuery(CollectionQuery(sort = SortOrder.BRAND))))
        assertEquals(listOf(3L, 2L, 1L), ids(items.applyQuery(CollectionQuery(sort = SortOrder.EXPIRES_FIRST))))
    }

    @Test
    fun `wishlist filters, want-badly first`() {
        val wishes = listOf(
            CosmeticItem(id = 1, name = "a", addedAtMillis = 5),
            CosmeticItem(id = 2, name = "b", wantBadly = true, addedAtMillis = 1),
            CosmeticItem(id = 3, name = "c", rebuy = true, addedAtMillis = 3),
        )
        assertEquals(listOf(2L, 1L, 3L), ids(wishes.applyWishlistFilter(WishlistFilter.ALL)))
        assertEquals(listOf(2L), ids(wishes.applyWishlistFilter(WishlistFilter.WANT_BADLY)))
        assertEquals(listOf(3L), ids(wishes.applyWishlistFilter(WishlistFilter.REBUY)))
        assertEquals(listOf(1L), ids(wishes.applyWishlistFilter(WishlistFilter.ALL, text = "A")))
    }
}

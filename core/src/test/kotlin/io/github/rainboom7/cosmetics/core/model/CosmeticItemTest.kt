package io.github.rainboom7.cosmetics.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CosmeticItemTest {
    private val opened = LocalDate.of(2026, 1, 15)
    private val cream = CosmeticItem(name = "Cream", openedOn = opened, periodAfterOpeningMonths = 6)

    @Test
    fun `expiry is opened date plus PAO months`() {
        assertEquals(LocalDate.of(2026, 7, 15), cream.expiresOn())
    }

    @Test
    fun `expiry is unknown without opened date or PAO`() {
        assertNull(cream.copy(openedOn = null).expiresOn())
        assertNull(cream.copy(periodAfterOpeningMonths = null).expiresOn())
        assertEquals(ExpiryStatus.UNKNOWN, cream.copy(openedOn = null).expiryStatus(opened))
    }

    @Test
    fun `expiry status around the boundaries`() {
        assertEquals(ExpiryStatus.OK, cream.expiryStatus(LocalDate.of(2026, 6, 14)))
        assertEquals(ExpiryStatus.SOON, cream.expiryStatus(LocalDate.of(2026, 6, 15)))
        assertEquals(ExpiryStatus.SOON, cream.expiryStatus(LocalDate.of(2026, 7, 15)))
        assertEquals(ExpiryStatus.EXPIRED, cream.expiryStatus(LocalDate.of(2026, 7, 16)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name is rejected`() {
        CosmeticItem(name = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `remaining percent out of range is rejected`() {
        CosmeticItem(name = "x", remainingPercent = 101)
    }

    @Test
    fun `cover prefers own photo, pager shows catalog photo first`() {
        val item = CosmeticItem(name = "x", catalogImageUrl = "https://obf/1.jpg", photoUris = listOf("file:///a.jpg"))
        assertEquals("file:///a.jpg", item.coverImage)
        assertEquals(listOf("https://obf/1.jpg", "file:///a.jpg"), item.images)
        assertEquals("https://obf/1.jpg", item.copy(photoUris = emptyList()).coverImage)
        assertEquals(false, item.copy(photoUris = emptyList(), catalogImageUrl = null).hasImage)
    }

    @Test
    fun `badges`() {
        val today = LocalDate.of(2026, 7, 1)
        val opened = cream.copy(usage = UsageState.OPENED, remainingPercent = 80)
        assertEquals(ItemBadge.EXPIRING, opened.badge(today))
        assertEquals(ItemBadge.RUNNING_LOW, opened.copy(periodAfterOpeningMonths = 24, remainingPercent = 35).badge(today))
        assertNull(opened.copy(periodAfterOpeningMonths = 24, remainingPercent = 36).badge(today))
        assertNull(opened.copy(usage = UsageState.FINISHED).badge(today))
        assertNull(opened.copy(status = ItemStatus.WISHLIST).badge(today))
    }

    @Test
    fun `catalog product converts to item`() {
        val product = CatalogProduct("3600523307593", "Aloe Water", "L'Oréal", "https://img", Category.SKINCARE, "70 ml", "Aqua", "obf")
        val item = product.toItem(ItemStatus.WISHLIST, addedAtMillis = 42)
        val expected = CosmeticItem(
            name = "Aloe Water", brand = "L'Oréal", category = Category.SKINCARE, status = ItemStatus.WISHLIST,
            barcode = "3600523307593", quantity = "70 ml", ingredients = "Aqua",
            catalogImageUrl = "https://img", catalogSource = "obf", addedAtMillis = 42,
        )
        assertEquals(expected, item)
        assertNull(product.copy(imageUrl = null).toItem(ItemStatus.OWNED, 0).catalogSource)
    }
}

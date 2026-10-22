package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CollectionBackupTest {
    @Test
    fun `export then import keeps everything but ids`() {
        val items = listOf(
            CosmeticItem(
                id = 7, name = "Aloe", brand = "L'Oréal", category = Category.SKINCARE, barcode = "3600523307593",
                quantity = "70 ml", ingredients = "Aqua", catalogImageUrl = "https://img", catalogSource = "openbeautyfacts",
                photoUris = listOf("file:///1.jpg"), notes = "любимый", usage = UsageState.OPENED, remainingPercent = 40,
                openedOn = LocalDate.of(2026, 3, 8), periodAfterOpeningMonths = 12, addedAtMillis = 123,
            ),
            CosmeticItem(id = 9, name = "Помада", status = ItemStatus.WISHLIST, wantBadly = true, rebuy = true),
        )

        val restored = CollectionBackup.import(CollectionBackup.export(items))

        assertEquals(items.map { it.copy(id = 0) }, restored)
    }

    @Test
    fun `unknown enum names and fields fall back to defaults`() {
        val text = """{"version":1,"items":[{"name":"x","category":"SPACE","usage":"??","futureField":1}]}"""
        val item = CollectionBackup.import(text).single()
        assertEquals(Category.OTHER, item.category)
        assertEquals(UsageState.NEW, item.usage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `garbage is rejected`() {
        CollectionBackup.import("hello")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `newer format is rejected`() {
        CollectionBackup.import("""{"version":99,"items":[]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bad date is rejected`() {
        CollectionBackup.import("""{"version":1,"items":[{"name":"x","openedOn":"yesterday"}]}""")
    }
}

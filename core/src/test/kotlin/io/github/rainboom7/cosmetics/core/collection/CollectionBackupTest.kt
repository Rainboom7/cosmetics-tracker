package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.usage.InMemoryUsageLogRepository
import io.github.rainboom7.cosmetics.core.usage.UsageMark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                shelfStatus = ShelfStatus.STOCKED, favorite = true,
            ),
            CosmeticItem(id = 8, name = "Тушь", usage = UsageState.FINISHED, remainingPercent = 0, finishedOn = LocalDate.of(2026, 9, 1)),
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

    @Test
    fun `a version 1 backup without the new fields still imports`() {
        val item = CollectionBackup.import("""{"version":1,"items":[{"name":"x"}]}""").single()
        assertEquals(null, item.shelfStatus)
        assertEquals(false, item.favorite)
        assertEquals(null, item.finishedOn)
    }

    @Test
    fun `restore brings back the usage calendar under the new ids`() = runTest {
        val items = listOf(CosmeticItem(id = 40, name = "A"), CosmeticItem(id = 50, name = "B"))
        val usage = listOf(
            UsageMark(40, LocalDate.of(2026, 10, 1)),
            UsageMark(40, LocalDate.of(2026, 10, 2)),
            UsageMark(50, LocalDate.of(2026, 10, 2)),
            UsageMark(99, LocalDate.of(2026, 10, 3)), // an item not in the backup: dropped
        )
        val text = CollectionBackup.export(items, usage)
        val repo = InMemoryCollectionRepository(listOf(CosmeticItem(id = 1, name = "old")))
        val log = InMemoryUsageLogRepository()

        CollectionBackup.restore(text, repo, log)

        val restored = repo.observeAll().first().associateBy { it.name }
        assertEquals(setOf("A", "B"), restored.keys)
        val idA = restored.getValue("A").id
        val idB = restored.getValue("B").id
        assertEquals(listOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)), log.observeForItem(idA).first())
        assertEquals(listOf(LocalDate.of(2026, 10, 2)), log.observeForItem(idB).first())
        assertEquals(3, log.observeAll().first().size)
    }

    @Test
    fun `a broken backup changes nothing on restore`() = runTest {
        val repo = InMemoryCollectionRepository(listOf(CosmeticItem(id = 1, name = "old")))
        val result = runCatching { CollectionBackup.restore("hello", repo, InMemoryUsageLogRepository()) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf("old"), repo.observeAll().first().map { it.name })
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

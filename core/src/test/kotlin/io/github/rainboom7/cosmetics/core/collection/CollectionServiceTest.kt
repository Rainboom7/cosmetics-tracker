package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CollectionServiceTest {
    private val now = Instant.parse("2026-10-22T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repo = InMemoryCollectionRepository()
    private val service = CollectionService(repo, clock)

    private val aloe = CatalogProduct("3600523307593", "Aloe Water", "L'Oréal", "https://img", Category.SKINCARE, source = "obf")

    @Test
    fun `adding a catalog product stores it with the clock time`() = runTest {
        val result = service.addFromCatalog(aloe, ItemStatus.OWNED)

        assertTrue(result is CollectionService.AddResult.Added)
        val stored = repo.get(result.id)!!
        assertEquals(now.toEpochMilli(), stored.addedAtMillis)
        assertEquals(ItemStatus.OWNED, stored.status)
        assertEquals("https://img", stored.catalogImageUrl)
    }

    @Test
    fun `adding the same barcode twice to one list does not duplicate`() = runTest {
        val first = service.addFromCatalog(aloe, ItemStatus.WISHLIST)
        val second = service.addFromCatalog(aloe, ItemStatus.WISHLIST)

        assertTrue(second is CollectionService.AddResult.AlreadyThere)
        assertEquals(first.id, second.id)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun `adding a wished product as owned moves it from the wishlist`() = runTest {
        val wished = service.addFromCatalog(aloe, ItemStatus.WISHLIST)
        val owned = service.addFromCatalog(aloe, ItemStatus.OWNED)

        assertTrue(owned is CollectionService.AddResult.MovedFromWishlist)
        assertEquals(wished.id, owned.id)
        assertEquals(ItemStatus.OWNED, repo.get(owned.id)!!.status)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun `products without barcode are always added`() = runTest {
        val noCode = aloe.copy(barcode = null)
        service.addFromCatalog(noCode, ItemStatus.OWNED)
        service.addFromCatalog(noCode, ItemStatus.OWNED)
        assertEquals(2, repo.observeAll().first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `manual item without a photo is rejected`() = runTest {
        service.save(CosmeticItem(name = "Крем"))
    }

    @Test
    fun `manual item with a photo is saved and stamped`() = runTest {
        val id = service.save(CosmeticItem(name = "Крем", photoUris = listOf("file:///p.jpg")))
        assertEquals(now.toEpochMilli(), repo.get(id)!!.addedAtMillis)
    }

    @Test
    fun `bought resets the item to new and returns the old state for undo`() = runTest {
        val id = repo.upsert(
            CosmeticItem(name = "Тушь", status = ItemStatus.WISHLIST, wantBadly = true, rebuy = true, remainingPercent = 10, usage = UsageState.FINISHED),
        )

        val before = service.markBought(id)!!

        val after = repo.get(id)!!
        assertEquals(ItemStatus.OWNED, after.status)
        assertEquals(UsageState.NEW, after.usage)
        assertEquals(100, after.remainingPercent)
        assertFalse(after.wantBadly)
        assertFalse(after.rebuy)

        service.restore(before)
        assertEquals(before, repo.get(id))
    }

    @Test
    fun `bought on an owned item does nothing`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "x"))
        assertNull(service.markBought(id))
    }

    @Test
    fun `buy again puts a rebuy copy on the wishlist once`() = runTest {
        val ownedId = repo.upsert(CosmeticItem(name = "Aloe", barcode = "3600523307593", usage = UsageState.OPENED, remainingPercent = 20, notes = "мой"))

        val wishId = service.buyAgain(ownedId)!!
        val again = service.buyAgain(ownedId)!!

        assertEquals(wishId, again)
        val wish = repo.get(wishId)!!
        assertEquals(ItemStatus.WISHLIST, wish.status)
        assertTrue(wish.rebuy)
        assertEquals(100, wish.remainingPercent)
        assertEquals("", wish.notes)
        assertEquals(ItemStatus.OWNED, repo.get(ownedId)!!.status)
    }

    @Test
    fun `opening starts the PAO countdown today and keeps the first opening date`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "x", periodAfterOpeningMonths = 12))

        service.setUsage(id, UsageState.OPENED)
        assertEquals(LocalDate.of(2026, 10, 22), repo.get(id)!!.openedOn)

        repo.upsert(repo.get(id)!!.copy(openedOn = LocalDate.of(2026, 1, 1)))
        service.setUsage(id, UsageState.OPENED)
        assertEquals(LocalDate.of(2026, 1, 1), repo.get(id)!!.openedOn)

        service.setUsage(id, UsageState.FINISHED)
        assertEquals(0, repo.get(id)!!.remainingPercent)

        service.setUsage(id, UsageState.NEW)
        assertNull(repo.get(id)!!.openedOn)
        assertEquals(1, repo.get(id)!!.remainingPercent)
    }

    @Test
    fun `remaining is clamped and photos are not duplicated`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "x"))
        service.setRemaining(id, 150)
        assertEquals(100, repo.get(id)!!.remainingPercent)

        service.addPhoto(id, "file:///1.jpg")
        service.addPhoto(id, "file:///1.jpg")
        assertEquals(listOf("file:///1.jpg"), repo.get(id)!!.photoUris)
    }

    @Test
    fun `delete returns the item for undo`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "x"))
        val deleted = service.delete(id)!!
        assertNull(repo.get(id))
        service.restore(deleted)
        assertEquals("x", repo.get(id)!!.name)
    }

    @Test
    fun `summary counts expiring items among unfinished owned ones`() {
        val today = LocalDate.of(2026, 10, 22)
        val items = listOf(
            CosmeticItem(name = "soon", openedOn = LocalDate.of(2026, 5, 1), periodAfterOpeningMonths = 6),
            CosmeticItem(name = "expired", openedOn = LocalDate.of(2025, 1, 1), periodAfterOpeningMonths = 6),
            CosmeticItem(name = "finished", openedOn = LocalDate.of(2025, 1, 1), periodAfterOpeningMonths = 6, usage = UsageState.FINISHED),
            CosmeticItem(name = "fresh"),
            CosmeticItem(name = "wish", status = ItemStatus.WISHLIST),
        )

        assertEquals(CollectionSummary(ownedCount = 4, wishlistCount = 1, expiringSoonCount = 1, expiredCount = 1), CollectionService.summarize(items, today))
    }

    @Test
    fun `known barcodes are split by list`() = runTest {
        repo.upsert(CosmeticItem(name = "a", barcode = "1"))
        repo.upsert(CosmeticItem(name = "b", barcode = "2", status = ItemStatus.WISHLIST))
        repo.upsert(CosmeticItem(name = "c"))

        assertEquals(KnownBarcodes(owned = setOf("1"), wishlist = setOf("2")), service.observeKnownBarcodes().first())
    }

    @Test
    fun `finishing stamps the day and drops a running-low mark`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "Тушь", usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 5, 1), shelfStatus = ShelfStatus.RUNNING_LOW))

        service.setUsage(id, UsageState.FINISHED)

        val finished = repo.get(id)!!
        assertEquals(LocalDate.of(2026, 10, 22), finished.finishedOn)
        assertNull(finished.shelfStatus)
        assertEquals(1, service.observeSummary().first().finishedThisYear)

        service.setUsage(id, UsageState.OPENED)
        assertNull(repo.get(id)!!.finishedOn)
    }

    @Test
    fun `opening on a given day starts PAO from that day`() = runTest {
        val id = repo.upsert(CosmeticItem(name = "Крем"))
        service.setUsage(id, UsageState.OPENED, openedOn = LocalDate.of(2026, 10, 3))
        assertEquals(LocalDate.of(2026, 10, 3), repo.get(id)!!.openedOn)
    }

    @Test
    fun `status and favorite marks, counted in the summary`() = runTest {
        val a = repo.upsert(CosmeticItem(name = "A"))
        val b = repo.upsert(CosmeticItem(name = "B", usage = UsageState.OPENED, remainingPercent = 10))

        service.setShelfStatus(a, ShelfStatus.NOT_USING)
        service.setFavorite(b, true)

        val summary = service.observeSummary().first()
        assertEquals(mapOf(ShelfStatus.NOT_USING to 1, ShelfStatus.RUNNING_LOW to 1), summary.shelfCounts)
        assertEquals(1, summary.favoriteCount)

        service.setShelfStatus(a, null)
        assertNull(repo.get(a)!!.shelfStatus)
    }

    @Test
    fun `bought and buy-again copies start without old marks`() = runTest {
        val owned = repo.upsert(
            CosmeticItem(name = "A", barcode = "1", favorite = true, shelfStatus = ShelfStatus.RUNNING_LOW, finishedOn = LocalDate.of(2026, 1, 1)),
        )
        val wishId = service.buyAgain(owned)!!
        val wish = repo.get(wishId)!!
        assertNull(wish.shelfStatus)
        assertFalse(wish.favorite)
        assertNull(wish.finishedOn)

        repo.upsert(wish.copy(shelfStatus = ShelfStatus.TRYING))
        service.markBought(wishId)
        assertNull(repo.get(wishId)!!.shelfStatus)
    }
}

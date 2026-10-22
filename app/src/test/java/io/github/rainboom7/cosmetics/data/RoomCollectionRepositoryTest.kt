package io.github.rainboom7.cosmetics.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.data.db.AppDatabase
import io.github.rainboom7.cosmetics.data.db.RoomCollectionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoomCollectionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomCollectionRepository

    private val full = CosmeticItem(
        name = "Aloe Water",
        brand = "L'Oréal",
        category = Category.SKINCARE,
        status = ItemStatus.OWNED,
        barcode = "3600523307593",
        shade = "универсальный",
        quantity = "70 ml",
        ingredients = "Aqua, Glycerin",
        catalogImageUrl = "https://img/1.jpg",
        catalogSource = "openbeautyfacts",
        photoUris = listOf("file:///a.jpg", "file:///b \"quoted\".jpg"),
        notes = "любимый",
        usage = UsageState.OPENED,
        remainingPercent = 40,
        openedOn = LocalDate.of(2026, 3, 8),
        periodAfterOpeningMonths = 12,
        price = "1 200 ₽",
        link = "https://shop.example/aloe",
        addedAtMillis = 123,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomCollectionRepository(db.items())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `round-trips every field`() = runTest {
        val id = repo.upsert(full)
        assertEquals(full.copy(id = id), repo.get(id))
    }

    @Test
    fun `update keeps the id`() = runTest {
        val id = repo.upsert(full)
        val again = repo.upsert(full.copy(id = id, remainingPercent = 10))
        assertEquals(id, again)
        assertEquals(10, repo.get(id)!!.remainingPercent)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun `observe by status and barcode lookup`() = runTest {
        repo.upsert(full)
        repo.upsert(full.copy(name = "Wish", status = ItemStatus.WISHLIST, addedAtMillis = 200))
        repo.upsert(full.copy(name = "Other", barcode = null, addedAtMillis = 300))

        assertEquals(listOf("Other", "Aloe Water"), repo.observeByStatus(ItemStatus.OWNED).first().map { it.name })
        assertEquals(listOf("Wish"), repo.observeByStatus(ItemStatus.WISHLIST).first().map { it.name })
        assertEquals(2, repo.findByBarcode("3600523307593").size)
    }

    @Test
    fun `observeItem follows changes and deletion`() = runTest {
        val id = repo.upsert(full)
        assertEquals("Aloe Water", repo.observeItem(id).first()!!.name)
        repo.delete(id)
        assertNull(repo.observeItem(id).first())
    }

    @Test
    fun `replaceAll swaps the whole collection`() = runTest {
        repo.upsert(full)
        repo.replaceAll(listOf(full.copy(id = 99, name = "A"), full.copy(id = 99, name = "B")))
        assertEquals(setOf("A", "B"), repo.observeAll().first().map { it.name }.toSet())
    }
}

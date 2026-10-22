package io.github.rainboom7.cosmetics.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.data.db.AppDatabase
import io.github.rainboom7.cosmetics.data.db.RoomCollectionRepository
import io.github.rainboom7.cosmetics.data.db.RoomUsageLogRepository
import io.github.rainboom7.cosmetics.core.usage.UsageMark
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
        shelfStatus = ShelfStatus.STOCKED,
        favorite = true,
        finishedOn = LocalDate.of(2026, 9, 30),
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

    @Test
    fun `usage marks are unique per day and go away with their item`() = runTest {
        val log = RoomUsageLogRepository(db.usage())
        val id = repo.upsert(full)
        val other = repo.upsert(full.copy(name = "Other"))
        val day = LocalDate.of(2026, 10, 20)

        log.add(UsageMark(id, day))
        log.add(UsageMark(id, day))
        log.addAll(listOf(UsageMark(id, day.plusDays(1)), UsageMark(other, day)))

        assertEquals(listOf(day.plusDays(1), day), log.observeForItem(id).first())
        assertEquals(3, log.observeAll().first().size)

        log.remove(UsageMark(id, day.plusDays(1)))
        assertEquals(listOf(day), log.observeForItem(id).first())

        repo.delete(id)
        assertEquals(listOf(UsageMark(other, day)), log.observeAll().first())
    }

    @Test
    fun `replaceAll returns the new ids in order and clears old marks`() = runTest {
        val log = RoomUsageLogRepository(db.usage())
        val old = repo.upsert(full)
        log.add(UsageMark(old, LocalDate.of(2026, 10, 1)))

        val ids = repo.replaceAll(listOf(full.copy(name = "A"), full.copy(name = "B")))

        assertEquals(listOf("A", "B"), ids.map { repo.get(it)!!.name })
        assertEquals(emptyList<UsageMark>(), log.observeAll().first())
    }
}

package io.github.rainboom7.cosmetics.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.usage.UsageMark
import io.github.rainboom7.cosmetics.data.db.AppDatabase
import io.github.rainboom7.cosmetics.data.db.RoomCollectionRepository
import io.github.rainboom7.cosmetics.data.db.RoomUsageLogRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Builds a version 1 database with the exact schema the first release created, then opens it with
 * the current [AppDatabase]. Room validates the migrated schema against the entities on open.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @Before
    fun createVersion1() {
        context.deleteDatabase(name)
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`brand` TEXT, `category` TEXT NOT NULL, `status` TEXT NOT NULL, `barcode` TEXT, `shade` TEXT, `quantity` TEXT, " +
                "`ingredients` TEXT, `catalogImageUrl` TEXT, `catalogSource` TEXT, `photoUris` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                "`usage` TEXT NOT NULL, `remainingPercent` INTEGER NOT NULL, `openedOn` INTEGER, `periodAfterOpeningMonths` INTEGER, " +
                "`wantBadly` INTEGER NOT NULL, `rebuy` INTEGER NOT NULL, `price` TEXT, `link` TEXT, `addedAtMillis` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_barcode` ON `items` (`barcode`)")
        db.execSQL(
            "INSERT INTO items (id, name, category, status, photoUris, notes, usage, remainingPercent, openedOn, wantBadly, rebuy, addedAtMillis) " +
                "VALUES (5, 'Крем', 'SKINCARE', 'OWNED', '[]', '', 'OPENED', 60, ${LocalDate.of(2026, 3, 8).toEpochDay()}, 0, 0, 1)",
        )
        db.version = 1
        db.close()
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun `version 1 data survives and the calendar works after migration`() = runTest {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = RoomCollectionRepository(db.items())
            val item = repo.get(5)!!
            assertEquals("Крем", item.name)
            assertEquals(UsageState.OPENED, item.usage)
            assertEquals(LocalDate.of(2026, 3, 8), item.openedOn)
            assertNull(item.shelfStatus)
            assertFalse(item.favorite)
            assertNull(item.finishedOn)

            val log = RoomUsageLogRepository(db.usage())
            log.add(UsageMark(5, LocalDate.of(2026, 10, 22)))
            assertEquals(listOf(LocalDate.of(2026, 10, 22)), log.observeForItem(5).first())
        } finally {
            db.close()
        }
    }
}

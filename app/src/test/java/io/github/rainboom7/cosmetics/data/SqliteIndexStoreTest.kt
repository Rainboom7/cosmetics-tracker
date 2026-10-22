package io.github.rainboom7.cosmetics.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.search.IndexSchema
import io.github.rainboom7.cosmetics.core.search.LocalIndexCatalog
import io.github.rainboom7.cosmetics.data.catalog.BundledIndexStore
import io.github.rainboom7.cosmetics.data.catalog.SqliteIndexStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The index queries run on Android's SQLite (FTS4, contentless table), not just on the JVM driver. */
@RunWith(AndroidJUnit4::class)
class SqliteIndexStoreTest {
    private val file = File.createTempFile("index", ".db").apply { delete() }

    private fun store(): SqliteIndexStore {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            IndexSchema.CREATE.forEach(db::execSQL)
            listOf(
                Triple("4005808890590", "Nivea crème", "Nivea"),
                Triple("3600531143199", "Lash Sensational mascara", "Maybelline New York"),
                Triple(null, "Крем для рук Бархатные ручки", "Бархатные ручки"),
            ).forEachIndexed { i, (code, name, brand) ->
                val id = i + 1L
                db.execSQL(
                    "INSERT INTO products (id, code, name, brand, categories) VALUES (?, ?, ?, ?, ?)",
                    arrayOf(id, code, name, brand, "en:creams"),
                )
                db.execSQL(
                    "INSERT INTO products_fts (docid, body) VALUES (?, ?)",
                    arrayOf(id, IndexSchema.searchBody(name, brand, null, listOf("en:creams"))),
                )
            }
        }
        return object : SqliteIndexStore() {
            override fun open() = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        }
    }

    @Test
    fun searchesAndLooksUpOnAndroidSqlite() = runTest {
        val catalog = LocalIndexCatalog(store(), dispatcher = Dispatchers.Unconfined)
        assertEquals(listOf("Nivea crème"), catalog.search("нивея").products.map { it.name })
        assertEquals("Lash Sensational mascara", catalog.search("тушь").products.first().name)
        assertEquals("Крем для рук Бархатные ручки", catalog.search("крем для рук").products.first().name)
        assertEquals("Nivea", catalog.findByBarcode("4005808890590")?.brand)
        assertNull(catalog.findByBarcode("4006381333931"))
    }

    @Test
    fun buildWithoutTheAssetFindsNothing() = runTest {
        val bundled = BundledIndexStore(ApplicationProvider.getApplicationContext(), asset = "missing.db")
        val catalog = LocalIndexCatalog(bundled, dispatcher = Dispatchers.Unconfined)
        assertEquals(0, catalog.search("nivea").totalCount)
        assertNull(catalog.findByBarcode("4005808890590"))
    }
}

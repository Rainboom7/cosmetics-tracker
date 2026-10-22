package io.github.rainboom7.cosmetics.core.search

import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.indexer.JdbcIndexStore
import io.github.rainboom7.cosmetics.indexer.ObfIndexBuilder
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Builds a real SQLite index from a slice of the export format and searches it like the app does. */
class LocalIndexTest {
    companion object {
        private lateinit var file: File
        private lateinit var store: JdbcIndexStore

        @BeforeClass
        @JvmStatic
        fun build() {
            file = File.createTempFile("obf-index", ".db")
            val export = LocalIndexTest::class.java.getResourceAsStream("/obf/export_sample.csv")!!.bufferedReader()
            val stats = export.use { ObfIndexBuilder.build(it, file) }
            assertEquals(8, stats.read)
            assertEquals(7, stats.kept)
            store = JdbcIndexStore(file)
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            store.close()
            file.delete()
        }
    }

    private val catalog get() = LocalIndexCatalog(store)

    private suspend fun names(query: String) = catalog.search(query).products.map { it.name }

    @Test
    fun `brand search returns every product, most scanned first`() = runTest {
        assertEquals(listOf("Nivea crème", "Nivea Soft"), names("nivea"))
    }

    @Test
    fun `cyrillic brand finds the latin spelling`() = runTest {
        assertEquals(listOf("Nivea crème", "Nivea Soft"), names("нивея"))
        assertEquals(listOf("Elseve Total Repair 5 shampooing"), names("лореаль"))
    }

    @Test
    fun `russian product words match english and french names`() = runTest {
        assertEquals("Lash Sensational mascara", names("тушь").first())
        assertEquals("Elseve Total Repair 5 shampooing", names("шампунь").first())
        assertEquals("Eau micellaire Skin Active", names("гарньер мицеллярная вода").first())
    }

    @Test
    fun `products matching every word come first, partial matches follow`() = runTest {
        val hits = names("крем для рук")
        assertEquals("Крем для рук Бархатные ручки", hits.first())
        assertTrue("other creams still shown: $hits", hits.size > 1)
    }

    @Test
    fun `accents and apostrophes do not matter`() = runTest {
        assertEquals("Nivea crème", names("creme nivea").first())
        assertEquals(listOf("Elseve Total Repair 5 shampooing"), names("L'OREAL elseve"))
    }

    @Test
    fun `russian brands and prefixes`() = runTest {
        assertEquals(listOf("Чистая линия крем для лица"), names("чистая линия"))
        assertEquals("Lash Sensational mascara", names("maybel").first())
    }

    @Test
    fun `rows map to catalog products with details`() = runTest {
        val soft = catalog.search("nivea soft").products.first()
        assertEquals("Nivea", soft.brand)
        assertEquals("200 ml", soft.quantity)
        assertEquals(Category.SKINCARE, soft.category)
        assertEquals("Crème hydratante", soft.details?.genericName)
        assertEquals(listOf("Vegan"), soft.details?.visibleLabels)
    }

    @Test
    fun `barcode lookup and misses`() = runTest {
        assertEquals("Nivea crème", catalog.findByBarcode("4005808890590")?.name)
        assertNull(catalog.findByBarcode("4006381333931"))
        assertEquals(0, catalog.search("zzzz qqqq").totalCount)
        assertEquals(0, catalog.search("  ").totalCount)
    }

    @Test
    fun `pages split the ranked list`() = runTest {
        val small = LocalIndexCatalog(store, pageSize = 1)
        val first = small.search("nivea", 1)
        val second = small.search("nivea", 2)
        assertEquals(2, first.totalCount)
        assertTrue(first.hasMore)
        assertEquals(listOf("Nivea Soft"), second.products.map { it.name })
    }
}

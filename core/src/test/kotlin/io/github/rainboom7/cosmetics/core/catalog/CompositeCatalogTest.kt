package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeCatalogTest {
    private fun product(name: String, barcode: String? = null, brand: String? = "Nivea") =
        CatalogProduct(barcode, name, brand, imageUrl = null, category = Category.SKINCARE, source = "test")

    private class Source(
        var products: List<CatalogProduct> = emptyList(),
        var failure: CatalogException? = null,
    ) : ProductCatalog {
        var searches = 0
        var lookups = 0

        override suspend fun search(query: String, page: Int): CatalogPage {
            searches++
            failure?.let { throw it }
            return CatalogPage(products, page, pageSize = 30, totalCount = products.size)
        }

        override suspend fun findByBarcode(barcode: String): CatalogProduct? {
            lookups++
            failure?.let { throw it }
            return products.firstOrNull { it.barcode == barcode }
        }
    }

    @Test
    fun `enough local results skip the api`() = runTest {
        val local = Source(List(10) { product("Local $it", "40000000000$it") })
        val remote = Source(listOf(product("Remote")))
        val page = CompositeCatalog(local, remote).search("nivea", 1)
        assertEquals(10, page.products.size)
        assertEquals(0, remote.searches)
    }

    @Test
    fun `few local results are topped up from the api without duplicates`() = runTest {
        val local = Source(listOf(product("Creme", "4005808890590")))
        val remote = Source(listOf(product("Crème (new photo)", "4005808890590"), product("creme"), product("Soft", "4005900009340")))
        val page = CompositeCatalog(local, remote).search("nivea", 1)
        // Same barcode, then same brand + name ("creme" = "Creme"), are dropped.
        assertEquals(listOf("Creme", "Soft"), page.products.map { it.name })
        assertEquals(2, page.totalCount)
    }

    @Test
    fun `api trouble keeps the local results`() = runTest {
        val local = Source(listOf(product("Creme")))
        val remote = Source(failure = CatalogRateLimitedException("slow down"))
        assertEquals(listOf("Creme"), CompositeCatalog(local, remote).search("nivea", 1).products.map { it.name })
    }

    @Test
    fun `an empty index does not hide an api failure`() = runTest {
        val remote = Source(failure = CatalogException("offline"))
        val error = runCatching { CompositeCatalog(Source(), remote).search("nivea", 1) }.exceptionOrNull()
        assertTrue(error is CatalogException)
    }

    @Test
    fun `without an index the api result and its errors pass through`() = runTest {
        val remote = Source(listOf(product("Remote")))
        assertEquals(listOf("Remote"), CompositeCatalog(null, remote).search("nivea", 1).products.map { it.name })
        remote.failure = CatalogException("offline")
        val error = runCatching { CompositeCatalog(null, remote).search("nivea", 1) }.exceptionOrNull()
        assertTrue(error is CatalogException)
    }

    @Test
    fun `barcodes prefer the api and fall back to the index offline`() = runTest {
        val local = Source(listOf(product("Local", "4005808890590")))
        val remote = Source(listOf(product("Remote", "4005808890590")))
        val catalog = CompositeCatalog(local, remote)
        assertEquals("Remote", catalog.findByBarcode("4005808890590")?.name)

        remote.failure = CatalogException("offline")
        assertEquals("Local", catalog.findByBarcode("4005808890590")?.name)
        val unknown = runCatching { catalog.findByBarcode("4006381333931") }
        assertFalse("offline and unknown reports the failure", unknown.isSuccess)

        remote.failure = null
        assertNull(catalog.findByBarcode("4006381333931"))
    }

    @Test
    fun `barcodes unknown to beauty facts are looked up in the fallbacks`() = runTest {
        val fallback = Source(listOf(product("Bioaqua mask", "6901234567892")))
        val catalog = CompositeCatalog(Source(), Source(), barcodeFallbacks = listOf(fallback))
        assertEquals("Bioaqua mask", catalog.findByBarcode("6901234567892")?.name)
        assertNull(catalog.findByBarcode("4006381333931"))
        assertEquals(2, fallback.lookups)
    }
}

package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.Category
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OpenBeautyFactsCatalogTest {
    private val server = MockWebServer()
    private lateinit var catalog: OpenBeautyFactsCatalog

    @Before
    fun setUp() {
        server.start()
        catalog = OpenBeautyFactsCatalog(
            client = OkHttpClient(),
            userAgent = "CosmeticsTracker-Test/1.0",
            baseUrl = server.url("/"),
        )
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String): String =
        javaClass.getResource("/obf/$name")!!.readText()

    @Test
    fun `search parses products, skips nameless ones and duplicates`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("search.json")))

        val page = catalog.search("  aloe  ")

        val request = server.takeRequest()
        assertEquals("/cgi/search.pl", request.requestUrl!!.encodedPath)
        assertEquals("aloe", request.requestUrl!!.queryParameter("search_terms"))
        assertEquals("1", request.requestUrl!!.queryParameter("json"))
        assertEquals("CosmeticsTracker-Test/1.0", request.getHeader("User-Agent"))

        assertEquals(listOf("3600523307593", "3600531143213", "4005900036650"), page.products.map { it.barcode })
        val first = page.products[0]
        assertEquals("Hydra Genius Aloe Water", first.name)
        assertEquals("L'Oréal Paris", first.brand)
        assertEquals("70 ml", first.quantity)
        assertEquals(Category.SKINCARE, first.category)
        assertTrue(first.imageUrl!!.startsWith("https://images.openbeautyfacts.org/"))

        val mascara = page.products[1]
        assertEquals("Volume Million Lashes mascara", mascara.name) // blank product_name falls through
        assertEquals(Category.MAKEUP, mascara.category)
        assertTrue(mascara.imageUrl!!.contains("front_en"))

        val handCream = page.products[2]
        assertEquals("Крем для рук", handCream.name) // Russian name preferred
        assertNull(handCream.imageUrl)

        assertEquals(30, page.totalCount)
        assertTrue(page.hasMore)
    }

    @Test
    fun `blank query does not hit the network`() = runTest {
        val page = catalog.search("   ")
        assertTrue(page.products.isEmpty())
        assertFalse(page.hasMore)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `barcode lookup`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("product.json")))

        val product = catalog.findByBarcode("3600523307593")!!

        assertEquals("/api/v2/product/3600523307593", server.takeRequest().requestUrl!!.encodedPath)
        assertEquals("Hydra Genius Aloe Water", product.name)
        assertEquals(OpenBeautyFactsCatalog.SOURCE, product.source)
    }

    @Test
    fun `unknown barcode is null, both for status 0 and HTTP 404`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("product_not_found.json")))
        server.enqueue(MockResponse().setResponseCode(404).setBody(fixture("product_not_found.json")))

        assertNull(catalog.findByBarcode("4006381333931"))
        assertNull(catalog.findByBarcode("4006381333931"))
    }

    @Test
    fun `invalid barcode is null without a request`() = runTest {
        assertNull(catalog.findByBarcode("123"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `server error becomes CatalogException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            catalog.search("aloe")
            fail("expected CatalogException")
        } catch (e: CatalogException) {
            assertTrue(e.message!!.contains("503"))
        }
    }

    @Test
    fun `malformed JSON becomes CatalogException`() = runTest {
        server.enqueue(MockResponse().setBody("<html>maintenance</html>"))
        try {
            catalog.search("aloe")
            fail("expected CatalogException")
        } catch (e: CatalogException) {
            // expected
        }
    }
}

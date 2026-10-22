package io.github.rainboom7.cosmetics.core.catalog

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses responses captured from the live Open Beauty Facts API (October 2026). */
class RealResponsesTest {
    private val server = MockWebServer().apply { start() }
    private val catalog = OpenBeautyFactsCatalog(OkHttpClient(), "test", baseUrl = server.url("/"))

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String) = javaClass.getResource("/obf/$name")!!.readText()

    @Test
    fun `nivea search keeps every named product`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("real_search_nivea.json")))
        val page = catalog.search("nivea")
        assertEquals(2167, page.totalCount)
        assertTrue("got ${page.products.size}", page.products.size >= 15)
    }

    @Test
    fun `russian search keeps every named product`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("real_search_krem.json")))
        val page = catalog.search("крем")
        assertTrue("got ${page.products.size}", page.products.size >= 15)
    }
}

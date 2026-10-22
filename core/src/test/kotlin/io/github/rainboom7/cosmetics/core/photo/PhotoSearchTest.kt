package io.github.rainboom7.cosmetics.core.photo

import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoSearchTest {
    private val aloe = CatalogProduct("3600523307593", "Hydra Genius Aloe Water", "L'Oréal Paris", null, Category.SKINCARE, source = "test")
    private val other = CatalogProduct("3600531143213", "Revitalift Filler", "L'Oréal Paris", null, Category.SKINCARE, source = "test")

    private class FakeCatalog(
        private val byBarcode: Map<String, CatalogProduct> = emptyMap(),
        private val byText: Map<String, List<CatalogProduct>> = emptyMap(),
    ) : ProductCatalog {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String, page: Int): CatalogPage {
            queries += query
            val products = byText[query].orEmpty()
            return CatalogPage(products, page, 24, products.size)
        }

        override suspend fun findByBarcode(barcode: String): CatalogProduct? {
            queries += "#$barcode"
            return byBarcode[barcode]
        }
    }

    private val jar = PhotoRecognition(
        barcodes = listOf("123", "3600523307593"),
        lines = listOf(
            RecognizedLine("70 ml e", 10),
            RecognizedLine("L'ORÉAL", 80),
            RecognizedLine("Hydra Genius", 50),
            RecognizedLine("Aloe Water / SPF", 30),
        ),
    )

    @Test
    fun `plan puts valid barcodes first, then text from biggest to smallest`() {
        val plan = PhotoQueryPlanner.plan(jar)
        assertEquals(
            listOf(
                PhotoQuery.ByBarcode("3600523307593"),
                PhotoQuery.ByText("L'ORÉAL Hydra Genius Aloe"),
                PhotoQuery.ByText("L'ORÉAL Hydra Genius"),
                PhotoQuery.ByText("L'ORÉAL Hydra"),
                PhotoQuery.ByText("L'ORÉAL"),
            ),
            plan,
        )
    }

    @Test
    fun `tokenizer drops volumes, stop words and single letters`() {
        assertEquals(listOf("Aloe", "Water"), PhotoQueryPlanner.tokenize("Aloe Water 50ml 100 g SPF e"))
        assertEquals(listOf("Крем", "рук"), PhotoQueryPlanner.tokenize("Крем для рук, 75 мл"))
    }

    @Test
    fun `empty recognition plans nothing`() {
        assertTrue(PhotoQueryPlanner.plan(PhotoRecognition()).isEmpty())
    }

    @Test
    fun `barcode hit comes first, text queries add alternatives`() = runTest {
        val catalog = FakeCatalog(
            byBarcode = mapOf(aloe.barcode!! to aloe),
            byText = mapOf("L'ORÉAL" to listOf(other, aloe)),
        )

        val result = PhotoSearch(catalog).search(jar)

        assertEquals(PhotoQuery.ByBarcode("3600523307593"), result.matchedQuery)
        assertEquals(listOf(PhotoMatch(aloe, 100)), result.matches.take(1))
        assertEquals(listOf(aloe, other), result.matches.map { it.product }) // aloe not duplicated
    }

    @Test
    fun `results of several queries are merged and ranked by confidence`() = runTest {
        val catalog = FakeCatalog(
            byText = mapOf(
                "L'ORÉAL Hydra Genius" to listOf(aloe),
                "L'ORÉAL" to listOf(other, aloe),
            ),
        )

        val result = PhotoSearch(catalog).search(jar)

        assertEquals(PhotoQuery.ByText("L'ORÉAL Hydra Genius"), result.matchedQuery)
        assertEquals(listOf(aloe, other), result.matches.map { it.product })
        assertTrue(result.matches[0].confidence > result.matches[1].confidence)
    }

    @Test
    fun `stops querying once enough candidates are found`() = runTest {
        val many = (1..6).map { CatalogProduct("code$it", "Product $it", "Brand", null, Category.OTHER, source = "test") }
        val catalog = FakeCatalog(byText = mapOf("L'ORÉAL Hydra Genius Aloe" to many))

        val result = PhotoSearch(catalog).search(jar)

        assertEquals(6, result.matches.size)
        assertEquals(listOf("#3600523307593", "L'ORÉAL Hydra Genius Aloe"), catalog.queries)
    }

    @Test
    fun `no match returns the plan for the UI`() = runTest {
        val result = PhotoSearch(FakeCatalog()).search(jar)
        assertNull(result.matchedQuery)
        assertTrue(result.matches.isEmpty())
        assertEquals(5, result.tried.size)
    }

    @Test
    fun `match score is accent and case insensitive`() {
        assertEquals(100, MatchScorer.score(listOf("LOREAL", "l'oreal", "PARIS", "hydra", "genius", "aloe", "water"), aloe))
        assertEquals(0, MatchScorer.score(emptyList(), aloe))
        assertEquals(33, MatchScorer.score(listOf("L'ORÉAL", "Paris"), aloe)) // 2 of 6 words
    }
}

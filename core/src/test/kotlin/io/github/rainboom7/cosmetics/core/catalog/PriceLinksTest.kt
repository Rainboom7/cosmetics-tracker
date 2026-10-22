package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.price.PriceLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceLinksTest {
    private fun product(name: String, brand: String?) =
        CatalogProduct(barcode = null, name = name, brand = brand, imageUrl = null, category = Category.SKINCARE, source = "test")

    @Test
    fun `query is the brand and the start of the name, without repeating the brand`() {
        assertEquals("Nivea crème", PriceLinks.query(product("Nivea crème", "Nivea")))
        assertEquals("Garnier Eau micellaire Skin Active 3", PriceLinks.query(product("Eau micellaire Skin Active 3 en 1 peaux sensibles", "Garnier")))
        assertEquals("Крем для рук", PriceLinks.query(product("Крем  для рук", null)))
    }

    @Test
    fun `urls are encoded for every shop`() {
        val urls = PriceLinks.shops.map { it.url("L'Oréal крем & co") }
        assertEquals(7, urls.size)
        urls.forEach { url ->
            assertTrue(url, url.startsWith("https://"))
            assertTrue(url, url.endsWith("L%27Or%C3%A9al%20%D0%BA%D1%80%D0%B5%D0%BC%20%26%20co"))
        }
    }
}

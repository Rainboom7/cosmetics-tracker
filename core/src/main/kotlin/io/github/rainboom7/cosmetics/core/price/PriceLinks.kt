package io.github.rainboom7.cosmetics.core.price

import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import java.net.URLEncoder

/**
 * "Найти цену": links to the shops' own search pages, opened in the browser. The app makes no
 * requests to the shops; the user sees their prices as on the website.
 */
object PriceLinks {
    data class Shop(val name: String, private val searchUrl: String) {
        fun url(query: String): String = searchUrl + URLEncoder.encode(query, Charsets.UTF_8).replace("+", "%20")
    }

    val shops = listOf(
        // Compares many shops at once, so it goes first.
        Shop("Яндекс Маркет", "https://market.yandex.ru/search?text="),
        Shop("Золотое Яблоко", "https://goldapple.ru/catalogsearch/result?q="),
        Shop("Лэтуаль", "https://www.letu.ru/s/search?q="),
        Shop("Подружка", "https://www.podrygka.ru/search/?q="),
        Shop("Рандеву", "https://randewoo.ru/search?q="),
    )

    /** Brand plus the start of the name: shop search engines do worse with long OBF names. */
    fun query(product: CatalogProduct): String {
        val name = product.name.split(Regex("\\s+")).filter { it.isNotBlank() }
        val withoutBrand = if (product.brand != null) name.filterNot { it.equals(product.brand, ignoreCase = true) } else name
        return (listOfNotNull(product.brand) + withoutBrand.take(MAX_NAME_WORDS)).joinToString(" ")
    }

    private const val MAX_NAME_WORDS = 5
}

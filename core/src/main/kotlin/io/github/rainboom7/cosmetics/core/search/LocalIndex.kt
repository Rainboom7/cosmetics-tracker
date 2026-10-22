package io.github.rainboom7.cosmetics.core.search

import io.github.rainboom7.cosmetics.core.catalog.CategoryClassifier
import io.github.rainboom7.cosmetics.core.catalog.OpenBeautyFactsCatalog
import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.model.Barcodes
import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The SQLite file built from the Open Beauty Facts export by the indexer (see `core/src/indexer`)
 * and bundled with the app. Rows are inserted most scanned first, so a lower id means a more
 * popular product. The full-text table is contentless: its docid is the product id and its only
 * column holds [SearchText.normalize]d name, brands, generic name and category words.
 */
object IndexSchema {
    const val VERSION = 1

    val CREATE = listOf(
        "CREATE TABLE products (id INTEGER PRIMARY KEY, code TEXT, name TEXT NOT NULL, brand TEXT, " +
            "quantity TEXT, image_url TEXT, categories TEXT, generic_name TEXT, labels TEXT, countries TEXT)",
        "CREATE INDEX products_code ON products(code)",
        "CREATE VIRTUAL TABLE products_fts USING fts4(content=\"\", body)",
        "CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)",
    )

    const val COLUMNS = "id, code, name, brand, quantity, image_url, categories, generic_name, labels, countries"
    const val MATCH_IDS = "SELECT docid FROM products_fts WHERE products_fts MATCH ?"
    const val BY_CODE = "SELECT $COLUMNS FROM products WHERE code = ? LIMIT 1"

    /** Ids are numbers from [MATCH_IDS], so inlining them is safe and avoids the 999 bind limit. */
    fun byIds(ids: Collection<Long>) = "SELECT $COLUMNS FROM products WHERE id IN (${ids.joinToString(",")})"

    /** What goes into the full-text column for one product. */
    fun searchBody(name: String, brands: String?, genericName: String?, categoryTags: List<String>): String =
        SearchText.normalize(
            listOfNotNull(name, brands, genericName, categoryTags.joinToString(" ") { it.substringAfter(':').replace('-', ' ') })
                .joinToString(" "),
        )
}

/** One row of the products table. [categories], [labels] and [countries] are comma separated tags. */
data class IndexedProduct(
    val id: Long,
    val code: String?,
    val name: String,
    val brand: String?,
    val quantity: String?,
    val imageUrl: String?,
    val categories: String?,
    val genericName: String?,
    val labels: String?,
    val countries: String?,
) {
    fun toCatalogProduct(): CatalogProduct {
        val tags = categories.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)
        return CatalogProduct(
            barcode = code?.let(Barcodes::normalize) ?: code?.takeIf { it.isNotBlank() },
            name = name,
            brand = brand,
            imageUrl = imageUrl,
            category = CategoryClassifier.classify(tags, name),
            quantity = quantity,
            source = OpenBeautyFactsCatalog.SOURCE,
            details = CatalogProduct.Details(
                genericName = genericName,
                categories = tags,
                labels = labels.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty),
                countries = countries,
            ).takeUnless { it.isEmpty },
        )
    }
}

/** Blocking access to the index file. Android and the JVM tests each provide one. */
interface IndexStore {
    fun matchIds(expression: String): Set<Long>
    fun products(ids: Collection<Long>): List<IndexedProduct>
    fun byCode(code: String): IndexedProduct?
}

/**
 * Searches the bundled index: no network, no rate limit. A product's score is the number of
 * query words it matches; ties go to the more popular product.
 */
class LocalIndexCatalog(
    private val store: IndexStore,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProductCatalog {

    override suspend fun search(query: String, page: Int): CatalogPage = withContext(dispatcher) {
        require(page >= 1) { "page starts at 1" }
        val ranked = rank(query)
        val slice = ranked.drop((page - 1) * pageSize).take(pageSize)
        val rows = if (slice.isEmpty()) emptyMap() else store.products(slice).associateBy { it.id }
        val products = slice.mapNotNull { rows[it]?.toCatalogProduct() }
        CatalogPage(products, page = page, pageSize = pageSize, totalCount = ranked.size)
    }

    override suspend fun findByBarcode(barcode: String): CatalogProduct? {
        val code = Barcodes.normalize(barcode) ?: return null
        return withContext(dispatcher) { store.byCode(code)?.toCatalogProduct() }
    }

    internal fun rank(query: String): List<Long> {
        val words = IndexQuery.plan(query)
        if (words.isEmpty()) return emptyList()
        val scores = HashMap<Long, Int>()
        words.forEach { word -> store.matchIds(word.expression).forEach { id -> scores.merge(id, 1, Int::plus) } }
        // With several words, a product matching only one of them is noise unless nothing better exists.
        val best = scores.values.maxOrNull() ?: return emptyList()
        val floor = if (words.size >= 3) maxOf(1, best - 1) else 1
        return scores.entries
            .filter { it.value >= floor }
            .sortedWith(compareByDescending<Map.Entry<Long, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
            .take(MAX_RESULTS)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
        const val MAX_RESULTS = 600
    }
}

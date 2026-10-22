package io.github.rainboom7.cosmetics.core.photo

import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.model.Barcodes
import io.github.rainboom7.cosmetics.core.model.CatalogProduct

/** A line of text found on the photo. [height] is the line's box height in pixels: bigger text first. */
data class RecognizedLine(val text: String, val height: Int)

/**
 * What on-device recognition (ML Kit on Android) found on a photo of a product. Kept platform-free
 * so the search strategy is unit-tested on the JVM.
 */
data class PhotoRecognition(
    val barcodes: List<String> = emptyList(),
    val lines: List<RecognizedLine> = emptyList(),
)

/** One way of looking a photo up in the catalog, tried in order by [PhotoSearch]. */
sealed interface PhotoQuery {
    data class ByBarcode(val barcode: String) : PhotoQuery
    data class ByText(val text: String) : PhotoQuery
}

/**
 * Turns a [PhotoRecognition] into catalog queries: every valid barcode first (exact match), then
 * text queries built from the biggest words on the package (usually brand and product line),
 * from most to least specific.
 */
object PhotoQueryPlanner {
    private const val MAX_WORDS = 4
    private const val MIN_WORD_LENGTH = 2

    // Words that are on every package and only add noise to a search.
    private val STOP_WORDS = setOf(
        "ml", "мл", "g", "г", "gr", "oz", "fl", "spf", "pa", "new", "новинка", "with", "and", "for", "the",
        "de", "la", "le", "et", "pour", "avec", "для", "и", "с", "made", "in", "ingredients", "состав", "www", "com",
    )

    fun plan(recognition: PhotoRecognition): List<PhotoQuery> {
        val barcodeQueries = recognition.barcodes.mapNotNull(Barcodes::normalize).distinct().map(PhotoQuery::ByBarcode)
        val words = recognition.lines
            .sortedByDescending { it.height }
            .flatMap { tokenize(it.text) }
            .distinctBy { it.lowercase() }
        val textQueries = (MAX_WORDS downTo 1)
            .map { words.take(it).joinToString(" ") }
            .filter { it.isNotEmpty() }
            .distinct()
            .map(PhotoQuery::ByText)
        return barcodeQueries + textQueries
    }

    internal fun tokenize(text: String): List<String> =
        text.split(Regex("[^\\p{L}\\p{N}'&-]+"))
            .map { it.trim('-', '\'', '&') }
            .filter { word ->
                word.length >= MIN_WORD_LENGTH &&
                    word.any(Char::isLetter) &&
                    word.lowercase() !in STOP_WORDS &&
                    !word.matches(Regex("\\d+[\\p{L}]{0,2}")) // "50ml", "100g"
            }
}

/** A catalog product with how well it matches the photo, 0..100 ("Совпадение N%"). */
data class PhotoMatch(val product: CatalogProduct, val confidence: Int)

data class PhotoSearchResult(
    /** The query that produced [matches], null when nothing matched. */
    val matchedQuery: PhotoQuery?,
    /** Best first. */
    val matches: List<PhotoMatch>,
    /** Every query that was planned, so the UI can show "searched for …". */
    val tried: List<PhotoQuery>,
)

/**
 * Scores a text match: the share of the product's brand+name words that were read on the photo,
 * so "L'Oréal Hydra Genius" read on a jar scores high for that product and low for other L'Oréal
 * products. Accent- and case-insensitive.
 */
object MatchScorer {
    fun score(recognizedWords: Collection<String>, product: CatalogProduct): Int {
        val seen = recognizedWords.mapTo(HashSet(), ::fold)
        val wanted = PhotoQueryPlanner.tokenize(listOfNotNull(product.brand, product.name).joinToString(" "))
            .mapTo(LinkedHashSet(), ::fold)
        if (wanted.isEmpty() || seen.isEmpty()) return 0
        val hits = wanted.count { word -> seen.any { it == word || (it.length >= 4 && word.startsWith(it)) } }
        return hits * 100 / wanted.size
    }

    private fun fold(word: String): String =
        java.text.Normalizer.normalize(word.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}

/**
 * Runs the [PhotoQueryPlanner] plan against a catalog. A barcode hit or the most specific text
 * query often returns a single product, so results of several queries are merged until there are
 * [wanted] candidates, then ranked: exact barcode matches first, then by [MatchScorer] confidence.
 */
class PhotoSearch(
    private val catalog: ProductCatalog,
    private val maxQueries: Int = 5,
    private val wanted: Int = 5,
) {
    suspend fun search(recognition: PhotoRecognition): PhotoSearchResult {
        val plan = PhotoQueryPlanner.plan(recognition)
        val words = recognition.lines.flatMap { PhotoQueryPlanner.tokenize(it.text) }
        val found = LinkedHashMap<String, PhotoMatch>()
        var firstHit: PhotoQuery? = null
        for (query in plan.take(maxQueries)) {
            if (found.size >= wanted) break
            val matches = when (query) {
                is PhotoQuery.ByBarcode -> listOfNotNull(catalog.findByBarcode(query.barcode)).map { PhotoMatch(it, 100) }
                is PhotoQuery.ByText -> catalog.search(query.text).products.map { PhotoMatch(it, MatchScorer.score(words, it)) }
            }
            if (matches.isNotEmpty() && firstHit == null) firstHit = query
            matches.forEach { found.putIfAbsent(it.product.barcode ?: it.product.name, it) }
        }
        return PhotoSearchResult(firstHit, found.values.sortedByDescending { it.confidence }, plan)
    }
}

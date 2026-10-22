package io.github.rainboom7.cosmetics.indexer

import io.github.rainboom7.cosmetics.core.search.LocalIndexCatalog
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import java.util.zip.GZIPInputStream
import kotlin.system.measureTimeMillis

/** Usage: IndexerMain <export url or .csv(.gz) file> <output .db>. Prints a self-check. */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: <export url or file> <output.db>" }
    val (source, out) = args
    val raw = if (source.startsWith("http")) {
        URI(source).toURL().openConnection().apply {
            setRequestProperty("User-Agent", "CosmeticsTracker-Indexer/1.0 (github.com/Rainboom7)")
        }.getInputStream()
    } else {
        File(source).inputStream()
    }
    val stream = if (source.endsWith(".gz")) GZIPInputStream(raw.buffered()) else raw
    val output = File(out)
    val stats = stream.bufferedReader().use { ObfIndexBuilder.build(it, output, sourceNote = source.substringAfterLast('/')) }
    println("Read ${stats.read} products, indexed ${stats.kept}, ${output.length() / 1024} KiB -> ${output.path}")

    JdbcIndexStore(output).use { store ->
        println("Meta: ${store.meta()}")
        val catalog = LocalIndexCatalog(store)
        SELF_CHECK.forEach { query ->
            var line = ""
            val ms = measureTimeMillis {
                val page = runBlocking { catalog.search(query) }
                line = "${page.totalCount} hits: " + page.products.take(5).joinToString(" | ") { "${it.brand} ${it.name}" }
            }
            println("  \"$query\" (${ms} ms) $line")
        }
    }
}

private val SELF_CHECK = listOf(
    "nivea", "нивея", "нивея крем", "крем для рук", "тушь", "шампунь", "l'oreal elseve", "лореаль", "garnier",
    "гарньер мицеллярная вода", "чистая линия", "la roche posay", "сыворотка с гиалуроновой кислотой", "spf 50",
)

package io.github.rainboom7.cosmetics.indexer

import io.github.rainboom7.cosmetics.core.search.IndexSchema
import java.io.BufferedReader
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Builds the bundled search index from the Open Beauty Facts CSV export: a tab-separated file,
 * one product per line, ~200 columns of which we keep a handful. Products without a name are
 * skipped, the rest are stored most scanned first (see [IndexSchema]).
 */
object ObfIndexBuilder {
    data class Stats(val read: Int, val kept: Int)

    private class Row(
        val code: String?,
        val name: String,
        val brands: String?,
        val quantity: String?,
        val imageUrl: String?,
        val categories: List<String>,
        val genericName: String?,
        val labels: List<String>,
        val countries: String?,
        val scans: Int,
        val completeness: Double,
    )

    fun build(input: BufferedReader, output: File, sourceNote: String = ""): Stats {
        val header = input.readLine()?.split('\t') ?: error("Empty export")
        val column = header.withIndex().associate { it.value to it.index }
        fun List<String>.at(name: String): String? = column[name]?.let { getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }
        fun tags(value: String?) = value.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)

        var read = 0
        val rows = ArrayList<Row>()
        input.lineSequence().forEach { line ->
            read++
            val f = line.split('\t')
            val name = f.at("product_name") ?: f.at("abbreviated_product_name") ?: return@forEach
            rows += Row(
                code = f.at("code"),
                name = name.take(MAX_TEXT),
                brands = f.at("brands")?.take(MAX_TEXT),
                quantity = f.at("quantity")?.take(40),
                imageUrl = f.at("image_url"),
                categories = tags(f.at("categories_tags")),
                genericName = f.at("generic_name")?.take(MAX_TEXT)?.takeUnless { it.equals(name, ignoreCase = true) },
                labels = tags(f.at("labels_tags")),
                countries = f.at("countries_en")?.take(MAX_TEXT),
                scans = f.at("unique_scans_n")?.toIntOrNull() ?: 0,
                completeness = f.at("completeness")?.toDoubleOrNull() ?: 0.0,
            )
        }
        rows.sortWith(
            compareByDescending<Row> { it.scans }
                .thenByDescending { it.imageUrl != null }
                .thenByDescending { it.completeness },
        )

        output.parentFile?.mkdirs()
        output.delete()
        DriverManager.getConnection("jdbc:sqlite:${output.path}").use { db ->
            db.autoCommit = false
            db.createStatement().use { st -> IndexSchema.CREATE.forEach(st::execute) }
            insert(db, rows)
            db.prepareStatement("INSERT INTO meta (key, value) VALUES (?, ?)").use { st ->
                mapOf(
                    "schema" to IndexSchema.VERSION.toString(),
                    "built_at" to System.currentTimeMillis().toString(),
                    "rows" to rows.size.toString(),
                    "source" to "Open Beauty Facts (ODbL) $sourceNote".trim(),
                ).forEach { (k, v) -> st.setString(1, k); st.setString(2, v); st.addBatch() }
                st.executeBatch()
            }
            db.commit()
            db.autoCommit = true
            db.createStatement().use { it.executeUpdate("INSERT INTO products_fts(products_fts) VALUES ('optimize')") }
            db.createStatement().use { it.executeUpdate("VACUUM") }
        }
        return Stats(read = read, kept = rows.size)
    }

    private fun insert(db: Connection, rows: List<Row>) {
        val product = db.prepareStatement(
            "INSERT INTO products (id, code, name, brand, quantity, image_url, categories, generic_name, labels, countries) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        val fts = db.prepareStatement("INSERT INTO products_fts (docid, body) VALUES (?, ?)")
        rows.forEachIndexed { index, row ->
            val id = index + 1L
            product.setLong(1, id)
            product.setString(2, row.code)
            product.setString(3, row.name)
            product.setString(4, row.brands?.split(',')?.map(String::trim)?.firstOrNull { it.isNotEmpty() })
            product.setString(5, row.quantity)
            product.setString(6, row.imageUrl)
            product.setString(7, row.categories.joinToString(",").ifEmpty { null })
            product.setString(8, row.genericName)
            product.setString(9, row.labels.joinToString(",").ifEmpty { null })
            product.setString(10, row.countries)
            product.addBatch()
            fts.setLong(1, id)
            fts.setString(2, IndexSchema.searchBody(row.name, row.brands, row.genericName, row.categories))
            fts.addBatch()
            if (index % 5_000 == 4_999) {
                product.executeBatch()
                fts.executeBatch()
            }
        }
        product.executeBatch()
        fts.executeBatch()
        product.close()
        fts.close()
    }

    private const val MAX_TEXT = 200
}

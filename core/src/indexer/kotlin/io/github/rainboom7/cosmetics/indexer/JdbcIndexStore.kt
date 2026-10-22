package io.github.rainboom7.cosmetics.indexer

import io.github.rainboom7.cosmetics.core.search.IndexSchema
import io.github.rainboom7.cosmetics.core.search.IndexStore
import io.github.rainboom7.cosmetics.core.search.IndexedProduct
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/** [IndexStore] over JDBC, for the indexer's self-check and the JVM tests. */
class JdbcIndexStore(file: File) : IndexStore, AutoCloseable {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite:${file.path}")

    override fun matchIds(expression: String): Set<Long> =
        db.prepareStatement(IndexSchema.MATCH_IDS).use { st ->
            st.setString(1, expression)
            st.executeQuery().use { rs -> buildSet { while (rs.next()) add(rs.getLong(1)) } }
        }

    override fun products(ids: Collection<Long>): List<IndexedProduct> {
        if (ids.isEmpty()) return emptyList()
        return db.createStatement().use { st -> st.executeQuery(IndexSchema.byIds(ids)).use(::readAll) }
    }

    override fun byCode(code: String): IndexedProduct? =
        db.prepareStatement(IndexSchema.BY_CODE).use { st ->
            st.setString(1, code)
            st.executeQuery().use(::readAll).firstOrNull()
        }

    fun meta(): Map<String, String> = db.createStatement().use { st ->
        st.executeQuery("SELECT key, value FROM meta").use { rs -> buildMap { while (rs.next()) put(rs.getString(1), rs.getString(2)) } }
    }

    private fun readAll(rs: ResultSet): List<IndexedProduct> = buildList {
        while (rs.next()) {
            add(
                IndexedProduct(
                    id = rs.getLong(1), code = rs.getString(2), name = rs.getString(3), brand = rs.getString(4),
                    quantity = rs.getString(5), imageUrl = rs.getString(6), categories = rs.getString(7),
                    genericName = rs.getString(8), labels = rs.getString(9), countries = rs.getString(10),
                ),
            )
        }
    }

    override fun close() = db.close()
}

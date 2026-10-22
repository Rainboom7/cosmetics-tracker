package io.github.rainboom7.cosmetics.data.catalog

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.github.rainboom7.cosmetics.core.search.IndexSchema
import io.github.rainboom7.cosmetics.core.search.IndexStore
import io.github.rainboom7.cosmetics.core.search.IndexedProduct
import java.io.File
import java.io.IOException

/**
 * The Open Beauty Facts index shipped in the APK assets (built in CI, see `core/src/indexer`).
 * SQLite cannot read from assets, so the first query copies it to app storage, once per app
 * version. Builds without the asset (local builds) simply find nothing and the app searches
 * online. All methods block: call them off the main thread.
 */
class BundledIndexStore(context: Context, private val asset: String = ASSET) : IndexStore {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase? by lazy { open() }

    override fun matchIds(expression: String): Set<Long> =
        db?.rawQuery(IndexSchema.MATCH_IDS, arrayOf(expression))?.use { c ->
            buildSet { while (c.moveToNext()) add(c.getLong(0)) }
        }.orEmpty()

    override fun products(ids: Collection<Long>): List<IndexedProduct> {
        if (ids.isEmpty()) return emptyList()
        return db?.rawQuery(IndexSchema.byIds(ids), null)?.use(::readAll).orEmpty()
    }

    override fun byCode(code: String): IndexedProduct? =
        db?.rawQuery(IndexSchema.BY_CODE, arrayOf(code))?.use(::readAll)?.firstOrNull()

    private fun open(): SQLiteDatabase? = try {
        val version = appContext.packageManager.getPackageInfo(appContext.packageName, 0).lastUpdateTime
        val dir = File(appContext.noBackupFilesDir, "obf-index").apply { mkdirs() }
        val file = File(dir, "index-$version.db")
        if (!file.exists()) copyAsset(dir, file)
        if (file.exists()) SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY) else null
    } catch (e: Exception) {
        Log.w(TAG, "Bundled index unavailable, searching online only", e)
        null
    }

    private fun copyAsset(dir: File, target: File) {
        val input = try {
            appContext.assets.open(asset)
        } catch (e: IOException) {
            return // No index in this build.
        }
        dir.listFiles()?.forEach { it.delete() } // Older versions of the index.
        val partial = File(dir, target.name + ".part")
        input.use { source -> partial.outputStream().use { source.copyTo(it, bufferSize = 64 * 1024) } }
        partial.renameTo(target)
    }

    private fun readAll(c: Cursor): List<IndexedProduct> = buildList {
        while (c.moveToNext()) {
            add(
                IndexedProduct(
                    id = c.getLong(0), code = c.getString(1), name = c.getString(2), brand = c.getString(3),
                    quantity = c.getString(4), imageUrl = c.getString(5), categories = c.getString(6),
                    genericName = c.getString(7), labels = c.getString(8), countries = c.getString(9),
                ),
            )
        }
    }

    companion object {
        const val ASSET = "obf-index.db"
        private const val TAG = "BundledIndexStore"
    }
}

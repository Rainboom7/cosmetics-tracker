package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.search.SearchText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Searches the bundled index first and asks the online API only when the index finds little,
 * so the API's small rate limit is kept for barcodes. Results are merged without duplicates
 * (same barcode, or same brand and name). Barcode lookups prefer the API, which is fresher and
 * has ingredients, fall back to the index offline, then to [barcodeFallbacks] (Open Food
 * Facts / Open Products Facts, where some cosmetics end up).
 */
class CompositeCatalog(
    private val local: ProductCatalog?,
    private val remote: ProductCatalog,
    private val enoughLocal: Int = 8,
    private val remoteTimeoutMillis: Long = 6_000,
    /** Barcode-only sources asked when neither the API nor the index knows the code. */
    private val barcodeFallbacks: List<ProductCatalog> = emptyList(),
) : ProductCatalog {

    override suspend fun search(query: String, page: Int): CatalogPage {
        val localResult = local?.let { attempt { it.search(query, page) } }
        val localPage = localResult?.getOrNull()
        val needRemote = localPage == null || (page == 1 && localPage.products.size < enoughLocal)
        if (!needRemote) return localPage!!

        val remotePage = if (localPage == null) {
            // No index (or it failed): the API is all there is, so let its errors through.
            remote.search(query, page)
        } else {
            attempt { withTimeoutOrNull(remoteTimeoutMillis) { remote.search(query, page) } }
                // Nothing locally and the API failed: say so instead of "nothing found".
                .getOrElse { if (localPage.products.isEmpty()) throw it else null }
                ?: return localPage
        }
        if (localPage == null) return remotePage

        val merged = merge(localPage.products, remotePage.products)
        val extra = merged.size - localPage.products.size
        return localPage.copy(products = merged, totalCount = localPage.totalCount + extra)
    }

    override suspend fun findByBarcode(barcode: String): CatalogProduct? {
        val online = attempt { remote.findByBarcode(barcode) }
        online.getOrNull()?.let { return it }
        val offline = local?.let { attempt { it.findByBarcode(barcode) }.getOrNull() }
        if (offline != null) return offline
        for (fallback in barcodeFallbacks) {
            attempt { fallback.findByBarcode(barcode) }.getOrNull()?.let { return it }
        }
        // Neither knows it: report the API failure if there was one, so the UI can say "offline".
        online.exceptionOrNull()?.let { throw it }
        return null
    }

    private inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: CatalogException) {
        Result.failure(e)
    }

    companion object {
        /** [first] keeps its order; [second] adds what [first] lacks. */
        fun merge(first: List<CatalogProduct>, second: List<CatalogProduct>): List<CatalogProduct> {
            val seen = HashSet<String>()
            val names = HashSet<String>()
            return (first + second).filter { p ->
                val name = SearchText.normalize("${p.brand.orEmpty()} ${p.name}")
                (p.barcode == null || seen.add(p.barcode)) && names.add(name)
            }
        }
    }
}

package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct

/** An external product database. Implementations must be main-safe. */
interface ProductCatalog {
    /** Full-text search. [page] starts at 1. */
    suspend fun search(query: String, page: Int = 1): CatalogPage

    /** Exact lookup by GTIN/EAN barcode, null when the catalog does not know it. */
    suspend fun findByBarcode(barcode: String): CatalogProduct?
}

/** Network or parsing failure talking to a catalog. The message is for logs, not for users. */
open class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The catalog asked us to slow down (HTTP 429/503), or our own limiter refused the call. */
class CatalogRateLimitedException(message: String) : CatalogException(message)

package io.github.rainboom7.cosmetics.core.model

/** A product found in an external catalog (e.g. Open Beauty Facts). Read-only, not stored as is. */
data class CatalogProduct(
    val barcode: String?,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val category: Category,
    val quantity: String? = null,
    val ingredients: String? = null,
    val source: String,
) {
    /** Converts the catalog entry into a collection item with the given [status]. */
    fun toItem(status: ItemStatus, addedAtMillis: Long, photoUris: List<String> = emptyList()): CosmeticItem = CosmeticItem(
        name = name,
        brand = brand,
        category = category,
        status = status,
        barcode = barcode,
        quantity = quantity,
        ingredients = ingredients,
        catalogImageUrl = imageUrl,
        catalogSource = source.takeIf { imageUrl != null },
        photoUris = photoUris,
        addedAtMillis = addedAtMillis,
    )
}

data class CatalogPage(
    val products: List<CatalogProduct>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
) {
    val hasMore: Boolean get() = page * pageSize < totalCount

    companion object {
        val EMPTY = CatalogPage(emptyList(), page = 1, pageSize = 0, totalCount = 0)
    }
}

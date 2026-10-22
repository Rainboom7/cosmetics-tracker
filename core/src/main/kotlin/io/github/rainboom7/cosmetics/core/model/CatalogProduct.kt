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
    /** Extra facts for the product card. Null when the source had none; fields are never blank. */
    val details: Details? = null,
) {
    /**
     * What the catalog knows beyond the basics, shown on the product card only when present.
     * Tags are Open Beauty Facts taxonomy ids such as "en:organic"; [tagLabel] makes them readable.
     */
    data class Details(
        val genericName: String? = null,
        val categories: List<String> = emptyList(),
        val labels: List<String> = emptyList(),
        val countries: String? = null,
        val origins: String? = null,
        val manufacturingPlaces: String? = null,
        val packaging: String? = null,
        val periodAfterOpening: String? = null,
        val stores: String? = null,
        val pageUrl: String? = null,
    ) {
        val isEmpty: Boolean get() = this == Details()

        /** Categories worth showing: Open Beauty Facts bookkeeping tags are dropped. */
        val visibleCategories: List<String>
            get() = categories.filter { tag -> HIDDEN_CATEGORIES.none { tag.endsWith(it) } }.map(::tagLabel).distinct()

        val visibleLabels: List<String> get() = labels.map(::tagLabel).distinct()

        companion object {
            private val HIDDEN_CATEGORIES = listOf("non-food-products", "open-beauty-facts", "open-food-facts", "baby-foods")

            /** "en:eu-organic" -> "Eu organic"; plain text stays as is. */
            fun tagLabel(tag: String): String {
                val text = tag.substringAfter(':').replace('-', ' ').trim()
                return text.replaceFirstChar { it.uppercase() }
            }
        }
    }

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

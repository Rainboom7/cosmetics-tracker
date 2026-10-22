package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.Barcodes
import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * [ProductCatalog] backed by Open Beauty Facts (https://world.openbeautyfacts.org), an open
 * (ODbL) crowd-sourced cosmetics database with product photos. No API key is needed, but the API
 * rules ask every client to send a descriptive User-Agent.
 */
class OpenBeautyFactsCatalog(
    private val client: OkHttpClient,
    private val userAgent: String,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProductCatalog {

    override suspend fun search(query: String, page: Int): CatalogPage {
        require(page >= 1) { "page starts at 1" }
        val terms = query.trim()
        if (terms.isEmpty()) return CatalogPage.EMPTY
        val url = baseUrl.newBuilder()
            .addPathSegments("cgi/search.pl")
            .addQueryParameter("search_terms", terms)
            .addQueryParameter("search_simple", "1")
            .addQueryParameter("action", "process")
            .addQueryParameter("json", "1")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("page_size", pageSize.toString())
            .addQueryParameter("fields", FIELDS)
            .build()
        val response = decode<SearchResponse>(get(url), url)
        val products = response.products.mapNotNull { it.toCatalogProduct() }.distinctBy { it.barcode ?: it.name }
        return CatalogPage(products, page = page, pageSize = pageSize, totalCount = response.count)
    }

    override suspend fun findByBarcode(barcode: String): CatalogProduct? {
        val code = Barcodes.normalize(barcode) ?: return null
        val url = baseUrl.newBuilder()
            .addPathSegments("api/v2/product")
            .addPathSegment(code)
            .addQueryParameter("fields", FIELDS)
            .build()
        val body = get(url, allowNotFound = true) ?: return null
        val response = decode<ProductResponse>(body, url)
        if (response.status != 1) return null
        return response.product?.toCatalogProduct()
    }

    private suspend fun get(url: HttpUrl): String = get(url, allowNotFound = false)!!

    private suspend fun get(url: HttpUrl, allowNotFound: Boolean): String? = withContext(dispatcher) {
        val request = Request.Builder().url(url).header("User-Agent", userAgent).get().build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 && allowNotFound -> null
                    !response.isSuccessful -> throw CatalogException("HTTP ${response.code} for $url")
                    else -> response.body?.string() ?: throw CatalogException("Empty body for $url")
                }
            }
        } catch (e: IOException) {
            throw CatalogException("Network error for $url", e)
        }
    }

    private inline fun <reified T> decode(body: String, url: HttpUrl): T = try {
        json.decodeFromString<T>(body)
    } catch (e: SerializationException) {
        throw CatalogException("Bad JSON from $url", e)
    } catch (e: IllegalArgumentException) {
        throw CatalogException("Bad JSON from $url", e)
    }

    @Serializable
    internal data class SearchResponse(
        val count: Int = 0,
        val products: List<ProductDto> = emptyList(),
    )

    @Serializable
    internal data class ProductResponse(
        val status: Int = 0,
        val product: ProductDto? = null,
    )

    @Serializable
    internal data class ProductDto(
        val code: String? = null,
        @SerialName("product_name") val productName: String? = null,
        @SerialName("product_name_ru") val productNameRu: String? = null,
        @SerialName("product_name_en") val productNameEn: String? = null,
        @SerialName("generic_name") val genericName: String? = null,
        val brands: String? = null,
        val quantity: String? = null,
        @SerialName("image_front_url") val imageFrontUrl: String? = null,
        @SerialName("image_url") val imageUrl: String? = null,
        @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
        @SerialName("categories_tags") val categoriesTags: List<String> = emptyList(),
        @SerialName("ingredients_text") val ingredientsText: String? = null,
    ) {
        fun toCatalogProduct(): CatalogProduct? {
            val name = listOf(productNameRu, productName, productNameEn, genericName)
                .firstOrNull { !it.isNullOrBlank() }?.trim() ?: return null
            return CatalogProduct(
                barcode = code?.let(Barcodes::normalize) ?: code?.takeIf { it.isNotBlank() },
                name = name,
                brand = brands?.split(',')?.map(String::trim)?.firstOrNull { it.isNotEmpty() },
                imageUrl = listOf(imageFrontUrl, imageUrl, imageFrontSmallUrl).firstOrNull { !it.isNullOrBlank() },
                category = CategoryClassifier.classify(categoriesTags, name),
                quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
                ingredients = ingredientsText?.trim()?.takeIf { it.isNotEmpty() },
                source = SOURCE,
            )
        }
    }

    companion object {
        const val SOURCE = "openbeautyfacts"
        const val DEFAULT_BASE_URL = "https://world.openbeautyfacts.org/"
        const val DEFAULT_PAGE_SIZE = 24
        internal const val FIELDS =
            "code,product_name,product_name_ru,product_name_en,generic_name,brands,quantity," +
                "image_front_url,image_url,image_front_small_url,categories_tags,ingredients_text"

        internal val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    }
}

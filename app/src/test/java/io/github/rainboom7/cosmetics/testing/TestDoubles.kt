package io.github.rainboom7.cosmetics.testing

import android.net.Uri
import io.github.rainboom7.cosmetics.core.catalog.CatalogException
import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.model.CatalogPage
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.photo.PhotoRecognition
import io.github.rainboom7.cosmetics.data.photo.PhotoRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Swaps Dispatchers.Main for a test dispatcher so viewModelScope runs in tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** In-memory catalog: exact barcode map plus "query contains" text search. */
class FakeCatalog(
    var products: List<CatalogProduct> = emptyList(),
    var pageSize: Int = 24,
) : ProductCatalog {
    var failing = false
    val searches = mutableListOf<Pair<String, Int>>()
    val lookups = mutableListOf<String>()

    override suspend fun search(query: String, page: Int): CatalogPage {
        searches += query to page
        if (failing) throw CatalogException("offline")
        val words = query.lowercase().split(' ').filter { it.isNotBlank() }
        val hits = products.filter { p ->
            val hay = listOfNotNull(p.brand, p.name).joinToString(" ").lowercase()
            words.all { it in hay }
        }
        val pageItems = hits.drop((page - 1) * pageSize).take(pageSize)
        return CatalogPage(pageItems, page, pageSize, hits.size)
    }

    override suspend fun findByBarcode(barcode: String): CatalogProduct? {
        lookups += barcode
        if (failing) throw CatalogException("offline")
        return products.firstOrNull { it.barcode == barcode }
    }
}

class FakeRecognizer(var result: PhotoRecognition = PhotoRecognition()) : PhotoRecognizer {
    val seen = mutableListOf<Uri>()

    override suspend fun recognize(imageUri: Uri): PhotoRecognition {
        seen += imageUri
        return result
    }
}

object Samples {
    val aloe = CatalogProduct(
        barcode = "3600523307593",
        name = "Hydra Genius Aloe Water",
        brand = "L'Oréal Paris",
        imageUrl = "https://images.openbeautyfacts.org/aloe.jpg",
        category = Category.SKINCARE,
        quantity = "70 ml",
        source = "openbeautyfacts",
    )
    val mascara = CatalogProduct(
        barcode = "3600531143213",
        name = "Volume Million Lashes",
        brand = "L'Oréal Paris",
        imageUrl = null,
        category = Category.MAKEUP,
        source = "openbeautyfacts",
    )
    val nivea = CatalogProduct(
        barcode = "4005900036650",
        name = "Hand cream",
        brand = "Nivea",
        imageUrl = "https://images.openbeautyfacts.org/nivea.jpg",
        category = Category.BODY,
        source = "openbeautyfacts",
    )
}

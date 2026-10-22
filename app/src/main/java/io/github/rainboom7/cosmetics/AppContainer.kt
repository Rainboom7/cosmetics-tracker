package io.github.rainboom7.cosmetics

import android.content.Context
import io.github.rainboom7.cosmetics.core.catalog.CompositeCatalog
import io.github.rainboom7.cosmetics.core.catalog.OpenBeautyFactsCatalog
import io.github.rainboom7.cosmetics.core.catalog.ProductCatalog
import io.github.rainboom7.cosmetics.core.collection.CollectionRepository
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.photo.PhotoSearch
import io.github.rainboom7.cosmetics.core.search.LocalIndexCatalog
import io.github.rainboom7.cosmetics.core.usage.UsageCalendar
import io.github.rainboom7.cosmetics.core.usage.UsageLogRepository
import io.github.rainboom7.cosmetics.data.catalog.BundledIndexStore
import io.github.rainboom7.cosmetics.data.db.AppDatabase
import io.github.rainboom7.cosmetics.data.db.RoomCollectionRepository
import io.github.rainboom7.cosmetics.data.db.RoomUsageLogRepository
import io.github.rainboom7.cosmetics.data.photo.MlKitPhotoRecognizer
import io.github.rainboom7.cosmetics.data.photo.PhotoRecognizer
import io.github.rainboom7.cosmetics.data.photo.PhotoStorage
import io.github.rainboom7.cosmetics.ui.catalogcard.CatalogSelection
import io.github.rainboom7.cosmetics.ui.scan.AddFlowStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection: one instance per app, created in [CosmeticsApp]. ViewModels get
 * their dependencies from here through their factories. Tests build ViewModels directly with fakes.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    val repository: CollectionRepository by lazy { RoomCollectionRepository(database.items()) }

    val collection: CollectionService by lazy { CollectionService(repository) }

    val usageLog: UsageLogRepository by lazy { RoomUsageLogRepository(database.usage()) }

    val usage: UsageCalendar by lazy { UsageCalendar(usageLog, collection) }

    /** The Open Beauty Facts API: barcodes, product details, and search when the index finds little. */
    val onlineCatalog: ProductCatalog by lazy {
        OpenBeautyFactsCatalog(
            client = okHttp,
            // Open Beauty Facts asks apps to identify themselves: AppName/Version (contact).
            userAgent = "CosmeticsTracker-Android/${BuildConfig.VERSION_NAME} (github.com/Rainboom7)",
        )
    }

    /** Offline full-text search over the index bundled in the APK. */
    val localCatalog: ProductCatalog by lazy { LocalIndexCatalog(BundledIndexStore(appContext)) }

    /** Some cosmetics live in Open Food Facts / Open Products Facts; asked only by barcode. */
    private val openFactsFallback: ProductCatalog by lazy {
        OpenBeautyFactsCatalog(
            client = okHttp,
            userAgent = "CosmeticsTracker-Android/${BuildConfig.VERSION_NAME} (github.com/Rainboom7)",
            baseUrl = OpenBeautyFactsCatalog.OPEN_FOOD_FACTS_URL.toHttpUrl(),
            productApi = OpenBeautyFactsCatalog.ProductApi.V3_ANY_PRODUCT_TYPE,
            source = OpenBeautyFactsCatalog.OPEN_FACTS_SOURCE,
        )
    }

    val catalog: ProductCatalog by lazy {
        CompositeCatalog(localCatalog, onlineCatalog, barcodeFallbacks = listOf(openFactsFallback))
    }

    val photoSearch: PhotoSearch by lazy { PhotoSearch(catalog) }

    val photoStorage: PhotoStorage by lazy { PhotoStorage(appContext) }

    val recognizer: PhotoRecognizer by lazy { MlKitPhotoRecognizer(appContext) }

    /** Hands the tapped search result to the product card. */
    val catalogSelection: CatalogSelection by lazy { CatalogSelection() }

    /** Hands a photo and its recognition from the scanner to the add form. */
    val addFlow: AddFlowStore by lazy { AddFlowStore() }
}

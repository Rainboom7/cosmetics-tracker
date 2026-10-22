package io.github.rainboom7.cosmetics

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import io.github.rainboom7.cosmetics.work.ExpiryReminderWorker

open class CosmeticsApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scheduleBackgroundWork()
    }

    /** Overridden in Robolectric tests, where WorkManager is not initialized. */
    protected open fun scheduleBackgroundWork() {
        ExpiryReminderWorker.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.okHttp })) }
            .build()
}

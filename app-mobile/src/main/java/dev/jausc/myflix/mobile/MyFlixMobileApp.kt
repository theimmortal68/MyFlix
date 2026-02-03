package dev.jausc.myflix.mobile

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dev.jausc.myflix.core.common.image.MyFlixImageLoader

/**
 * Application class for MyFlix Mobile app.
 *
 * Initializes:
 * - Coil image loader with optimized caching for mobile
 */
class MyFlixMobileApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Coil will call newImageLoader() when first image is requested
    }

    /**
     * Create optimized ImageLoader for mobile.
     * - 25% memory cache for smooth scrolling
     * - 250MB disk cache for offline browsing
     * - Crossfade enabled for smoother transitions
     */
    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return MyFlixImageLoader.createForMobile(context)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Let Coil handle memory trimming automatically
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Coil's memory cache will be cleared automatically
    }
}

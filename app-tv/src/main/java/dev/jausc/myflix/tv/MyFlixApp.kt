package dev.jausc.myflix.tv

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dev.jausc.myflix.core.common.image.MyFlixImageLoader

/**
 * Application class for MyFlix TV app.
 *
 * Initializes:
 * - Coil image loader with optimized caching for TV
 */
class MyFlixApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Coil will call newImageLoader() when first image is requested
    }

    /**
     * Create optimized ImageLoader for TV.
     * - 25% memory cache for fast scrolling through media grids
     * - 250MB disk cache for persistent caching
     * - Crossfade disabled for instant image display
     */
    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return MyFlixImageLoader.createForTv(context)
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

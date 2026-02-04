package dev.jausc.myflix.core.common.image

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade

/**
 * Factory for creating optimized Coil ImageLoader instances.
 *
 * Configuration:
 * - Memory cache: 25% of available heap for fast in-memory access
 * - Disk cache: 250MB for persistent caching across app restarts
 * - Crossfade disabled for snappier feel on TV (images appear instantly)
 * - Aggressive caching policies to minimize network requests
 */
object MyFlixImageLoader {
    private const val TAG = "MyFlixImageLoader"

    /**
     * Create an optimized ImageLoader for the application.
     *
     * @param context Application context
     * @param memoryCachePercent Memory cache size as percentage of heap (default: 25%)
     * @param diskCacheSizeBytes Disk cache size in bytes (default: 250MB)
     * @param enableCrossfade Whether to enable crossfade animation (default: false for TV)
     * @return Configured ImageLoader instance
     */
    fun create(
        context: Context,
        memoryCachePercent: Double = ImageConstants.MEMORY_CACHE_PERCENT,
        diskCacheSizeBytes: Long = ImageConstants.DISK_CACHE_SIZE_BYTES,
        enableCrossfade: Boolean = ImageConstants.CROSSFADE_ENABLED,
    ): ImageLoader {
        Log.d(TAG, "Creating ImageLoader: memory=${(memoryCachePercent * 100).toInt()}%, " +
            "disk=${diskCacheSizeBytes / 1024 / 1024}MB, crossfade=$enableCrossfade")

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryCachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(ImageConstants.DISK_CACHE_DIRECTORY))
                    .maxSizeBytes(diskCacheSizeBytes)
                    .build()
            }
            .crossfade(enableCrossfade)
            // Always prefer cached images - Jellyfin images don't change frequently
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    /**
     * Create an ImageLoader optimized for TV apps.
     * Uses default settings with crossfade disabled for instant image display.
     */
    fun createForTv(context: Context): ImageLoader = create(
        context = context,
        enableCrossfade = false,
    )

    /**
     * Create an ImageLoader optimized for mobile apps.
     * Enables subtle crossfade for smoother transitions.
     */
    fun createForMobile(context: Context): ImageLoader = create(
        context = context,
        enableCrossfade = true,
    )
}

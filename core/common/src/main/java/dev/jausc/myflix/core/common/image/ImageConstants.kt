package dev.jausc.myflix.core.common.image

/**
 * Constants for image loading and caching configuration.
 */
object ImageConstants {
    /** Memory cache size as percentage of available heap (0.25 = 25%) */
    const val MEMORY_CACHE_PERCENT = 0.25

    /** Disk cache size in bytes (250MB) */
    const val DISK_CACHE_SIZE_BYTES = 250L * 1024 * 1024

    /** Directory name for disk cache */
    const val DISK_CACHE_DIRECTORY = "image_cache"

    /** Maximum backdrop image width (downscale larger images) */
    const val MAX_BACKDROP_WIDTH = 1920

    /** Maximum backdrop image height (downscale larger images) */
    const val MAX_BACKDROP_HEIGHT = 1080

    /** Maximum poster image width */
    const val MAX_POSTER_WIDTH = 500

    /** Maximum poster image height */
    const val MAX_POSTER_HEIGHT = 750

    /** Crossfade disabled for snappier feel on TV */
    const val CROSSFADE_ENABLED = false
}

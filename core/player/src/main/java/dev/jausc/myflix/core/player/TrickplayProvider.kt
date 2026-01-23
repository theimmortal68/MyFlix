package dev.jausc.myflix.core.player

import dev.jausc.myflix.core.common.model.TrickplayInfo

/**
 * Provider for trickplay (seek preview) thumbnail calculations.
 * Handles the math to determine which tile image and pixel offset for a given position.
 *
 * Trickplay images are organized as tile grids (e.g., 10x10 thumbnails per image).
 * Each thumbnail represents a specific time interval in the video.
 */
class TrickplayProvider(
    private val trickplayInfo: TrickplayInfo,
    private val mediaSourceId: String?,
) {
    /** Total thumbnails per tile grid image */
    private val thumbnailsPerTile: Int = trickplayInfo.thumbnailsPerTile

    /** Width of each individual thumbnail in pixels */
    val thumbnailWidth: Int = trickplayInfo.width

    /** Height of each individual thumbnail in pixels */
    val thumbnailHeight: Int = trickplayInfo.height

    /** Width of tile grid (number of thumbnails horizontally) */
    val tileWidth: Int = trickplayInfo.tileWidth

    /** Height of tile grid (number of thumbnails vertically) */
    val tileHeight: Int = trickplayInfo.tileHeight

    /** Interval in milliseconds between thumbnails */
    private val intervalMs: Int = trickplayInfo.interval

    /** Total number of thumbnails available */
    private val thumbnailCount: Int = trickplayInfo.thumbnailCount

    /**
     * Get the thumbnail index for a given playback position.
     *
     * @param positionMs Playback position in milliseconds
     * @return Thumbnail index (0-based, clamped to valid range)
     */
    fun getThumbnailIndex(positionMs: Long): Int {
        if (intervalMs <= 0 || positionMs < 0) return 0
        val index = (positionMs / intervalMs).toInt()
        return index.coerceIn(0, thumbnailCount - 1)
    }

    /**
     * Get the tile grid image index for a given playback position.
     * This determines which .jpg file to fetch.
     *
     * @param positionMs Playback position in milliseconds
     * @return Tile image index (0-based)
     */
    fun getTileImageIndex(positionMs: Long): Int {
        val thumbnailIndex = getThumbnailIndex(positionMs)
        return thumbnailIndex / thumbnailsPerTile
    }

    /**
     * Get the pixel offset within the tile grid image for cropping.
     *
     * @param positionMs Playback position in milliseconds
     * @return Pair of (x, y) pixel offsets within the tile image
     */
    fun getTileOffset(positionMs: Long): Pair<Int, Int> {
        val thumbnailIndex = getThumbnailIndex(positionMs)
        val indexWithinTile = thumbnailIndex % thumbnailsPerTile

        // Calculate row and column within the tile grid
        val column = indexWithinTile % tileWidth
        val row = indexWithinTile / tileWidth

        // Convert to pixel offsets
        val xOffset = column * thumbnailWidth
        val yOffset = row * thumbnailHeight

        return xOffset to yOffset
    }

    /**
     * Get the media source ID for URL building.
     */
    fun getMediaSourceId(): String? = mediaSourceId

    companion object {
        /**
         * Create a TrickplayProvider from item trickplay data.
         * Returns null if trickplay data is not available.
         *
         * @param trickplayData The trickplay map from JellyfinItem
         * @param mediaSourceId The media source ID to use (or null for first available)
         * @return TrickplayProvider or null if no trickplay data
         */
        fun create(
            trickplayData: Map<String, Map<String, TrickplayInfo>>?,
            mediaSourceId: String?,
        ): TrickplayProvider? {
            if (trickplayData.isNullOrEmpty()) return null

            // Get the trickplay info for the media source
            // If mediaSourceId is null, use the first available source
            val sourceId = mediaSourceId ?: trickplayData.keys.firstOrNull() ?: return null
            val resolutionMap = trickplayData[sourceId] ?: return null

            // Get the highest resolution available (largest width)
            val width = resolutionMap.keys
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull()
                ?: return null

            val info = resolutionMap[width.toString()] ?: return null

            return TrickplayProvider(info, sourceId)
        }
    }
}

package dev.jausc.myflix.tv.channels

import android.content.Context
import android.net.Uri
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Builds TvContractCompat preview programs from JellyfinItem data.
 *
 * This utility creates PreviewProgram objects for custom app channels on the Android TV
 * home screen, and WatchNextProgram objects for the system "Play Next" row.
 */
object PreviewProgramBuilder {

    private const val TICKS_PER_MS = 10_000L

    /**
     * Build a PreviewProgram for the custom channel.
     *
     * @param context Android context (unused but kept for potential future use)
     * @param item The Jellyfin item to build a program from
     * @param channelId The ID of the channel this program belongs to
     * @param serverUrl The base URL of the Jellyfin server for image URLs
     * @return A PreviewProgram ready to be inserted into the TV provider
     */
    @Suppress("UNUSED_PARAMETER")
    fun buildPreviewProgram(
        context: Context,
        item: JellyfinItem,
        channelId: Long,
        serverUrl: String,
    ): PreviewProgram {
        val type = when (item.type) {
            "Movie" -> TvContractCompat.PreviewPrograms.TYPE_MOVIE
            "Episode" -> TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE
            "Series" -> TvContractCompat.PreviewPrograms.TYPE_TV_SERIES
            else -> TvContractCompat.PreviewPrograms.TYPE_CLIP
        }

        val builder = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(type)
            .setTitle(getDisplayTitle(item))
            .setDescription(item.overview ?: "")
            .setIntentUri(buildPlaybackUri(item))
            .setInternalProviderId(item.id)
            .setPosterArtUri(buildPosterUri(item, serverUrl))
            .setContentId(item.id)

        // Episode-specific metadata
        if (item.type == "Episode") {
            item.indexNumber?.let { builder.setEpisodeNumber(it) }
            item.parentIndexNumber?.let { builder.setSeasonNumber(it) }
            item.seriesName?.let { builder.setSeasonTitle(it) }
        }

        // Duration
        item.runTimeTicks?.let { ticks ->
            val durationMs = ticks / TICKS_PER_MS
            builder.setDurationMillis(durationMs.toInt())
        }

        // Resume position (for Continue Watching items)
        val positionTicks = item.userData?.playbackPositionTicks ?: 0
        if (positionTicks > 0) {
            val positionMs = positionTicks / TICKS_PER_MS
            builder.setLastPlaybackPositionMillis(positionMs.toInt())
        }

        return builder.build()
    }

    /**
     * Build a WatchNextProgram for the Play Next row.
     *
     * @param item The Jellyfin item to build a program from
     * @param positionMs Current playback position in milliseconds
     * @param serverUrl The base URL of the Jellyfin server for image URLs
     * @param watchNextType The type of Watch Next entry (CONTINUE, NEXT, NEW, WATCHLIST)
     * @return A WatchNextProgram ready to be inserted into the TV provider
     */
    fun buildWatchNextProgram(
        item: JellyfinItem,
        positionMs: Long,
        serverUrl: String,
        watchNextType: Int = TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE,
    ): WatchNextProgram {
        val type = when (item.type) {
            "Movie" -> TvContractCompat.WatchNextPrograms.TYPE_MOVIE
            "Episode" -> TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE
            else -> TvContractCompat.WatchNextPrograms.TYPE_CLIP
        }

        val builder = WatchNextProgram.Builder()
            .setType(type)
            .setWatchNextType(watchNextType)
            .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
            .setTitle(getDisplayTitle(item))
            .setDescription(item.overview ?: "")
            .setIntentUri(buildPlaybackUri(item, positionMs))
            .setInternalProviderId(item.id)
            .setPosterArtUri(buildPosterUri(item, serverUrl))
            .setContentId(item.id)

        // Episode-specific metadata
        if (item.type == "Episode") {
            item.indexNumber?.let { builder.setEpisodeNumber(it) }
            item.parentIndexNumber?.let { builder.setSeasonNumber(it) }
            item.seriesName?.let { builder.setSeasonTitle(it) }
        }

        // Duration and position
        item.runTimeTicks?.let { ticks ->
            val durationMs = ticks / TICKS_PER_MS
            builder.setDurationMillis(durationMs.toInt())
        }
        if (positionMs > 0) {
            builder.setLastPlaybackPositionMillis(positionMs.toInt())
        }

        return builder.build()
    }

    /**
     * Get display title - for episodes include series and episode info.
     *
     * @param item The Jellyfin item
     * @return A formatted display title
     */
    private fun getDisplayTitle(item: JellyfinItem): String {
        return if (item.type == "Episode") {
            val series = item.seriesName ?: ""
            val season = item.parentIndexNumber?.let { "S$it" } ?: ""
            val episode = item.indexNumber?.let { "E$it" } ?: ""
            val episodeTitle = item.name
            if (series.isNotEmpty()) {
                "$series $season$episode - $episodeTitle"
            } else {
                episodeTitle
            }
        } else {
            item.name
        }
    }

    /**
     * Build deep link URI for playback.
     *
     * @param item The Jellyfin item
     * @param positionMs Optional start position in milliseconds
     * @return A deep link URI that can be used to launch playback
     */
    private fun buildPlaybackUri(item: JellyfinItem, positionMs: Long = 0): Uri {
        val builder = Uri.Builder()
            .scheme("myflix")
            .authority("play")
            .appendPath(item.id)
        if (positionMs > 0) {
            builder.appendQueryParameter("startPositionMs", positionMs.toString())
        }
        return builder.build()
    }

    /**
     * Build poster image URI from Jellyfin server.
     *
     * For episodes, uses the series poster if available (via seriesId).
     * For other items, uses the item's own primary image.
     *
     * @param item The Jellyfin item
     * @param serverUrl The base URL of the Jellyfin server
     * @return A URI pointing to the poster image
     */
    private fun buildPosterUri(item: JellyfinItem, serverUrl: String): Uri {
        // Use series poster for episodes (better visual on home screen), otherwise use item's own image
        val imageItemId = if (item.type == "Episode" && item.seriesId != null) {
            item.seriesId!!
        } else {
            item.id
        }
        return Uri.parse("$serverUrl/Items/$imageItemId/Images/Primary?maxWidth=400&quality=90")
    }
}

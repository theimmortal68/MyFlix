package dev.jausc.myflix.tv.channels

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.TvContractCompat
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages the MyFlix preview channel on the Android TV home screen.
 * Shows Continue Watching + Next Up items in a single row.
 */
class TvChannelManager private constructor(
    private val context: Context,
) {
    private val mutex = Mutex()
    private val contentResolver = context.contentResolver

    companion object {
        private const val TAG = "TvChannelManager"
        private const val CHANNEL_ID = "myflix_continue_watching"
        private const val CHANNEL_DISPLAY_NAME = "MyFlix"

        /**
         * Maximum programs to show in the channel.
         */
        private const val MAX_PROGRAMS = 20

        @Volatile
        private var instance: TvChannelManager? = null

        fun getInstance(context: Context): TvChannelManager {
            return instance ?: synchronized(this) {
                instance ?: TvChannelManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Get or create the MyFlix channel. Returns the channel ID.
     */
    suspend fun getOrCreateChannel(): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Check for existing channel
            val existingId = findChannel()
            if (existingId != null) {
                return@withContext existingId
            }

            // Create new channel
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(CHANNEL_DISPLAY_NAME)
                .setAppLinkIntentUri(Uri.parse("myflix://home"))
                .setInternalProviderId(CHANNEL_ID)
                .build()

            val channelUri = contentResolver.insert(
                TvContractCompat.Channels.CONTENT_URI,
                channel.toContentValues(),
            )

            val channelId = channelUri?.let { ContentUris.parseId(it) }
                ?: throw IllegalStateException("Failed to create channel")

            // Set channel logo
            try {
                val logoDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                if (logoDrawable != null) {
                    val bitmap: Bitmap = logoDrawable.toBitmap()
                    ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set channel logo", e)
            }

            Log.d(TAG, "Created MyFlix channel with ID: $channelId")
            channelId
        }
    }

    /**
     * Update channel programs with Continue Watching + Next Up items.
     *
     * @param continueWatching Items the user has started watching
     * @param nextUp Next episodes in series the user is watching
     * @param serverUrl Jellyfin server URL for poster images
     */
    suspend fun updatePrograms(
        continueWatching: List<JellyfinItem>,
        nextUp: List<JellyfinItem>,
        serverUrl: String,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val channelId = getOrCreateChannel()

                // Clear existing programs
                contentResolver.delete(
                    TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                    null,
                    null,
                )

                // Combine and dedupe: Continue Watching first, then Next Up
                val seenIds = mutableSetOf<String>()
                val allItems = mutableListOf<JellyfinItem>()

                continueWatching.forEach { item ->
                    if (seenIds.add(item.id)) {
                        allItems.add(item)
                    }
                }
                nextUp.forEach { item ->
                    if (seenIds.add(item.id)) {
                        allItems.add(item)
                    }
                }

                // Insert programs (limited to MAX_PROGRAMS)
                allItems.take(MAX_PROGRAMS).forEach { item ->
                    val program = PreviewProgramBuilder.buildPreviewProgram(
                        context = context,
                        item = item,
                        channelId = channelId,
                        serverUrl = serverUrl,
                    )
                    contentResolver.insert(
                        TvContractCompat.PreviewPrograms.CONTENT_URI,
                        program.toContentValues(),
                    )
                }

                Log.d(TAG, "Updated channel with ${allItems.take(MAX_PROGRAMS).size} programs")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating channel programs", e)
            }
        }
    }

    /**
     * Find existing MyFlix channel by internal provider ID.
     */
    private fun findChannel(): Long? {
        val cursor = contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            Channel.PROJECTION,
            null,
            null,
            null,
        ) ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val channel = Channel.fromCursor(it)
                if (channel.internalProviderId == CHANNEL_ID) {
                    return channel.id
                }
            }
        }
        return null
    }

    /**
     * Request the system to make the channel visible (browsable).
     * User must approve this via system dialog.
     */
    fun requestChannelBrowsable(channelId: Long) {
        TvContractCompat.requestChannelBrowsable(context, channelId)
    }

    /**
     * Delete the channel and all its programs.
     */
    suspend fun deleteChannel() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val channelId = findChannel() ?: return@withContext
            contentResolver.delete(
                TvContractCompat.buildChannelUri(channelId),
                null,
                null,
            )
            Log.d(TAG, "Deleted MyFlix channel")
        }
    }
}

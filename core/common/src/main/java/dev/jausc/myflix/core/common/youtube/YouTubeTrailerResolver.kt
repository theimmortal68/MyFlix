package dev.jausc.myflix.core.common.youtube

import android.content.Context
import android.util.Log
import dev.jausc.myflix.core.common.potoken.PoTokenProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "YouTubeTrailerResolver"

data class YouTubeStream(
    val url: String,
    val title: String?,
    val durationMs: Long,
    val isHls: Boolean,
)

object YouTubeTrailerResolver {
    private val initialized = AtomicBoolean(false)

    /**
     * Initialize the YouTube resolver with context for PoToken generation.
     * Must be called before resolving trailers.
     */
    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(OkHttpDownloader())
            PoTokenProviderImpl.initialize(context)
            YoutubeStreamExtractor.setPoTokenProvider(PoTokenProviderImpl)
            // Enable iOS client as fallback when Android client doesn't have a valid PoToken
            YoutubeStreamExtractor.setFetchIosClient(true)
        }
    }

    @Deprecated("Use initialize(context) instead", ReplaceWith("initialize(context)"))
    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(OkHttpDownloader())
            // Without context, PoTokenProvider won't work
        }
    }

    suspend fun resolveTrailer(context: Context, videoKey: String): Result<YouTubeStream> =
        withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Starting trailer resolution for videoKey: $videoKey ===")
            initialize(context)
            Log.d(TAG, "NewPipe initialized, fetching stream info...")

            val videoUrl = "https://www.youtube.com/watch?v=$videoKey"
            val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
            Log.d(TAG, "StreamInfo received: title=${info.name}, duration=${info.duration}s")
            Log.d(TAG, "HLS URL: ${info.hlsUrl}")
            Log.d(TAG, "Video streams count: ${info.videoStreams?.size ?: 0}")
            Log.d(TAG, "Audio streams count: ${info.audioStreams?.size ?: 0}")

            val hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() }
            if (hlsUrl != null) {
                Log.d(TAG, "Using HLS stream: $hlsUrl")
                return@withContext Result.success(
                    YouTubeStream(
                    url = hlsUrl,
                    title = info.name,
                    durationMs = info.duration.coerceAtLeast(0) * 1000,
                    isHls = true,
                )
                )
            }

            Log.d(TAG, "No HLS, selecting best progressive stream...")
            info.videoStreams?.forEachIndexed { idx, stream ->
                Log.d(TAG, "  Stream[$idx]: ${stream.resolution} ${stream.format?.name} codec=${stream.codec} videoOnly=${stream.isVideoOnly}")
            }

            val stream = selectBestStream(info)
            if (stream == null) {
                Log.e(TAG, "No playable streams found!")
                throw IOException("No playable YouTube streams available")
            }

            Log.d(TAG, "Selected stream: ${stream.resolution} ${stream.format?.name} codec=${stream.codec}")

            val streamUrl = stream.content
            if (streamUrl.isNullOrBlank()) {
                Log.e(TAG, "Stream URL is null or blank!")
                throw IOException("Stream URL is not available")
            }

            Log.d(TAG, "=== SUCCESS: Stream URL obtained (${streamUrl.take(100)}...) ===")

            Result.success(
                YouTubeStream(
                url = streamUrl,
                title = info.name,
                durationMs = info.duration.coerceAtLeast(0) * 1000,
                isHls = stream.deliveryMethod == DeliveryMethod.HLS,
            )
            )
        } catch (e: Exception) {
            Log.e(TAG, "=== FAILED: Error resolving trailer ===", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            e.cause?.let { Log.e(TAG, "Cause: ${it.message}", it) }
            Result.failure(e)
        }
    }

    private fun selectBestStream(info: StreamInfo): VideoStream? {
        val allStreams = info.videoStreams ?: emptyList()
        val withAudio = allStreams.filter { !it.isVideoOnly }
        val preferred = if (withAudio.isNotEmpty()) withAudio else allStreams

        // Prefer H.264/AVC over HEVC for better compatibility
        val h264Streams = preferred.filter {
            val codec = it.codec?.lowercase(Locale.US) ?: ""
            codec.contains("avc") || codec.contains("h264") || codec.contains("264")
        }

        // Also prefer MP4 format
        val mp4Preferred = preferred.filter {
            it.format?.name?.lowercase(Locale.US)?.contains("mp4") == true
        }

        // First try H.264 MP4, then H.264 any, then MP4, then any
        val h264Mp4 = h264Streams.filter { it.format?.name?.lowercase(Locale.US)?.contains("mp4") == true }
        val candidates = when {
            h264Mp4.isNotEmpty() -> h264Mp4
            h264Streams.isNotEmpty() -> h264Streams
            mp4Preferred.isNotEmpty() -> mp4Preferred
            else -> preferred
        }

        return candidates.maxByOrNull { parseResolution(it.resolution) }
    }

    private fun parseResolution(value: String?): Int {
        if (value.isNullOrBlank()) return 0
        val digits = value.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}

private class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder().url(request.url())
        request.headers().forEach { (key, values) ->
            values.forEach { value -> requestBuilder.addHeader(key, value) }
        }

        // Add User-Agent if not present (required by YouTube)
        if (!request.headers().containsKey("User-Agent")) {
            requestBuilder.addHeader("User-Agent", USER_AGENT)
        }

        val method = request.httpMethod().uppercase(Locale.US)
        val body = request.dataToSend()?.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

        when (method) {
            "POST" -> requestBuilder.post(body ?: ByteArray(0).toRequestBody(null))
            "HEAD" -> requestBuilder.head()
            else -> requestBuilder.get()
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string().orEmpty()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            response.request.url.toString(),
        )
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}

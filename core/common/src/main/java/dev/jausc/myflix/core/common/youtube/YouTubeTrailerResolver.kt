package dev.jausc.myflix.core.common.youtube

import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
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
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream

data class YouTubeStream(
    val url: String,
    val title: String?,
    val durationMs: Long,
    val isHls: Boolean,
)

object YouTubeTrailerResolver {
    private val initialized = AtomicBoolean(false)

    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(OkHttpDownloader())
        }
    }

    suspend fun resolveTrailer(videoKey: String): Result<YouTubeStream> = withContext(Dispatchers.IO) {
        runCatching {
            initialize()
            val videoUrl = "https://www.youtube.com/watch?v=$videoKey"
            val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)

            val hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() }
            if (hlsUrl != null) {
                return@runCatching YouTubeStream(
                    url = hlsUrl,
                    title = info.name,
                    durationMs = info.duration.coerceAtLeast(0) * 1000,
                    isHls = true,
                )
            }

            val stream = selectBestStream(info)
                ?: throw IOException("No playable YouTube streams available")

            YouTubeStream(
                url = stream.url,
                title = info.name,
                durationMs = info.duration.coerceAtLeast(0) * 1000,
                isHls = stream.deliveryMethod == DeliveryMethod.HLS,
            )
        }
    }

    private fun selectBestStream(info: StreamInfo): VideoStream? {
        val allStreams = info.videoStreams ?: emptyList()
        val withAudio = allStreams.filter { !it.isVideoOnly }
        val preferred = if (withAudio.isNotEmpty()) withAudio else allStreams
        val mp4Preferred = preferred.filter {
            it.format?.name?.lowercase(Locale.US)?.contains("mp4") == true
        }
        val candidates = if (mp4Preferred.isNotEmpty()) mp4Preferred else preferred
        return candidates.maxByOrNull { parseResolution(it.resolution) }
    }

    private fun parseResolution(value: String?): Int {
        if (value.isNullOrBlank()) return 0
        val digits = value.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}

private class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient()

    override fun download(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder().url(request.url)
        request.headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val method = request.httpMethod?.uppercase(Locale.US) ?: "GET"
        val body = request.data?.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

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
            responseBody,
            response.headers.toMultimap(),
            response.request.url.toString(),
        )
    }
}

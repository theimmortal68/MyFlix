package dev.jausc.myflix.core.network

import dev.jausc.myflix.core.common.model.GitHubRelease
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * GitHub API client for checking releases and downloading APKs.
 */
class GitHubClient {
    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val REPO_OWNER = "theimmortal68"
        private const val REPO_NAME = "MyFlix"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(this@GitHubClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Fetch the latest release from GitHub.
     */
    suspend fun getLatestRelease(): Result<GitHubRelease> = runCatching {
        httpClient.get("$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/releases/latest") {
            header("Accept", "application/vnd.github.v3+json")
        }.body()
    }

    /**
     * Download a file from URL with progress callback.
     *
     * @param url The download URL
     * @param destinationFile The file to write to
     * @param onProgress Callback with progress (0.0 to 1.0)
     */
    suspend fun downloadFile(url: String, destinationFile: File, onProgress: (Float) -> Unit,): Result<File> =
        runCatching {
        val response = httpClient.get(url)
        val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: 0L
        val channel = response.bodyAsChannel()

        destinationFile.parentFile?.mkdirs()

        FileOutputStream(destinationFile).use { outputStream ->
            var totalBytesRead = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead <= 0) break

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    onProgress(totalBytesRead.toFloat() / contentLength.toFloat())
                }
            }
        }

        destinationFile
    }

    /**
     * Clean up resources.
     */
    fun close() {
        httpClient.close()
    }
}

package dev.jausc.myflix.core.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dev.jausc.myflix.core.common.model.AppType
import dev.jausc.myflix.core.common.model.GitHubRelease
import dev.jausc.myflix.core.common.model.UpdateInfo
import java.io.File

/**
 * Manages app updates from GitHub releases.
 * Handles version checking, APK downloading, and installation.
 */
class UpdateManager(
    private val context: Context,
    private val gitHubClient: GitHubClient = GitHubClient(),
) {
    companion object {
        private const val TAG = "UpdateManager"
        private const val UPDATES_DIR = "updates"
    }

    /**
     * Check if an update is available.
     *
     * @param currentVersion Current app version (e.g., "1.0.0")
     * @param appType Which app to check updates for
     */
    suspend fun checkForUpdate(currentVersion: String, appType: AppType): Result<UpdateInfo> {
        return gitHubClient.getLatestRelease().map { release ->
            val latestVersion = parseVersion(release.tagName)
            val hasUpdate = isNewerVersion(currentVersion, latestVersion)

            val asset = findApkAsset(release, appType)

            UpdateInfo(
                hasUpdate = hasUpdate,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseNotes = release.body,
                downloadUrl = asset?.downloadUrl,
                apkSize = asset?.size ?: 0L,
            )
        }
    }

    /**
     * Download APK from URL with progress tracking.
     *
     * @param url Download URL
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Downloaded APK file
     */
    suspend fun downloadApk(url: String, onProgress: (Float) -> Unit): Result<File> {
        val updatesDir = File(context.cacheDir, UPDATES_DIR)
        val apkFile = File(updatesDir, "update.apk")

        // Clean up old downloads
        updatesDir.listFiles()?.forEach { it.delete() }

        return gitHubClient.downloadFile(url, apkFile, onProgress)
    }

    /**
     * Launch APK installation.
     *
     * @param file The APK file to install
     */
    fun installApk(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file,
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            throw e
        }
    }

    /**
     * Parse version string, removing 'v' prefix if present.
     * "v1.2.3" -> "1.2.3"
     */
    private fun parseVersion(version: String): String = version.removePrefix("v").removePrefix("V").trim()

    /**
     * Compare versions to check if latest is newer than current.
     * Supports semantic versioning (major.minor.patch).
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }

            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }

        return false // Versions are equal
    }

    /**
     * Find the APK asset for the given app type.
     * Looks for assets matching "app-tv" or "app-mobile" prefix.
     */
    private fun findApkAsset(release: GitHubRelease, appType: AppType) = release.assets.find { asset ->
            asset.name.startsWith(appType.assetPrefix) &&
                asset.name.endsWith(".apk")
        }

    /**
     * Clean up resources.
     */
    fun close() {
        gitHubClient.close()
    }
}

package dev.jausc.myflix.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release API response model.
 * Used for OTA update checking from GitHub releases.
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String,
    val body: String,
    @SerialName("published_at") val publishedAt: String,
    val assets: List<GitHubAsset>,
    val prerelease: Boolean,
    val draft: Boolean,
)

/**
 * GitHub Release Asset model.
 * Represents a downloadable file attached to a release (e.g., APK).
 */
@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long,
    @SerialName("content_type") val contentType: String,
)

/**
 * Update information returned by UpdateManager.
 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val apkSize: Long,
)

/**
 * App type for selecting the correct APK asset.
 */
enum class AppType(val assetPrefix: String) {
    TV("app-tv"),
    MOBILE("app-mobile"),
}

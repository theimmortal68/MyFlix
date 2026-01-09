package dev.jausc.myflix.core.common.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class JellyfinModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ==================== Type Check Tests ====================

    @Test
    fun `isMovie returns true for Movie type`() {
        val item = createItem(type = "Movie")
        assertThat(item.isMovie).isTrue()
        assertThat(item.isSeries).isFalse()
        assertThat(item.isEpisode).isFalse()
    }

    @Test
    fun `isSeries returns true for Series type`() {
        val item = createItem(type = "Series")
        assertThat(item.isSeries).isTrue()
        assertThat(item.isMovie).isFalse()
    }

    @Test
    fun `isEpisode returns true for Episode type`() {
        val item = createItem(type = "Episode")
        assertThat(item.isEpisode).isTrue()
        assertThat(item.isMovie).isFalse()
    }

    @Test
    fun `isSeason returns true for Season type`() {
        val item = createItem(type = "Season")
        assertThat(item.isSeason).isTrue()
    }

    // ==================== Runtime Tests ====================

    @Test
    fun `runtimeMinutes calculates correctly`() {
        // 2 hours = 120 minutes = 72,000,000,000,000 ticks (100ns per tick)
        val twoHoursInTicks = 120L * 60 * 10_000_000
        val item = createItem(runTimeTicks = twoHoursInTicks)
        assertThat(item.runtimeMinutes).isEqualTo(120)
    }

    @Test
    fun `runtimeMinutes returns null when runTimeTicks is null`() {
        val item = createItem(runTimeTicks = null)
        assertThat(item.runtimeMinutes).isNull()
    }

    @Test
    fun `runtimeMinutes handles short content`() {
        // 30 minutes
        val thirtyMinutesInTicks = 30L * 60 * 10_000_000
        val item = createItem(runTimeTicks = thirtyMinutesInTicks)
        assertThat(item.runtimeMinutes).isEqualTo(30)
    }

    // ==================== Progress Tests ====================

    @Test
    fun `progressPercent calculates correctly`() {
        val totalTicks = 100_000L
        val positionTicks = 50_000L
        val item = createItem(
            runTimeTicks = totalTicks,
            userData = UserData(playbackPositionTicks = positionTicks)
        )
        assertThat(item.progressPercent).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun `progressPercent returns 0 when no progress`() {
        val item = createItem(
            runTimeTicks = 100_000L,
            userData = UserData(playbackPositionTicks = 0)
        )
        assertThat(item.progressPercent).isEqualTo(0f)
    }

    @Test
    fun `progressPercent clamps to 1 when position exceeds total`() {
        val item = createItem(
            runTimeTicks = 100_000L,
            userData = UserData(playbackPositionTicks = 150_000L)
        )
        assertThat(item.progressPercent).isEqualTo(1f)
    }

    @Test
    fun `progressPercent returns 0 when runTimeTicks is null`() {
        val item = createItem(runTimeTicks = null)
        assertThat(item.progressPercent).isEqualTo(0f)
    }

    // ==================== Video Quality Tests ====================

    @Test
    fun `is4K returns true for 4K content`() {
        val item = createItemWithVideo(width = 3840, height = 2160)
        assertThat(item.is4K).isTrue()
    }

    @Test
    fun `is4K returns true for UHD content above 3840`() {
        val item = createItemWithVideo(width = 4096, height = 2160)
        assertThat(item.is4K).isTrue()
    }

    @Test
    fun `is4K returns false for 1080p content`() {
        val item = createItemWithVideo(width = 1920, height = 1080)
        assertThat(item.is4K).isFalse()
    }

    @Test
    fun `is4K returns false when no video stream`() {
        val item = createItem()
        assertThat(item.is4K).isFalse()
    }

    // ==================== HDR Tests ====================

    @Test
    fun `isHdr detects HDR10 content`() {
        val item = createItemWithVideo(videoRangeType = "HDR10")
        assertThat(item.isHdr).isTrue()
    }

    @Test
    fun `isHdr detects HLG content`() {
        val item = createItemWithVideo(videoRangeType = "HLG")
        assertThat(item.isHdr).isTrue()
    }

    @Test
    fun `isHdr returns false for SDR content`() {
        val item = createItemWithVideo(videoRangeType = "SDR", videoRange = "SDR")
        assertThat(item.isHdr).isFalse()
    }

    // ==================== Dolby Vision Tests ====================

    @Test
    fun `isDolbyVision detects DOVIWithHDR10 range type`() {
        val item = createItemWithVideo(videoRangeType = "DOVIWithHDR10")
        assertThat(item.isDolbyVision).isTrue()
    }

    @Test
    fun `isDolbyVision detects dolby in videoRange`() {
        val item = createItemWithVideo(videoRange = "Dolby Vision")
        assertThat(item.isDolbyVision).isTrue()
    }

    @Test
    fun `isDolbyVision detects dvhe profile`() {
        val item = createItemWithVideo(profile = "dvhe.05.06")
        assertThat(item.isDolbyVision).isTrue()
    }

    @Test
    fun `isDolbyVision detects DV in display title`() {
        val item = createItemWithVideo(displayTitle = "HEVC / DV / 4K")
        assertThat(item.isDolbyVision).isTrue()
    }

    @Test
    fun `isDolbyVision returns false for HDR10 only`() {
        val item = createItemWithVideo(
            videoRangeType = "HDR10",
            videoRange = "HDR",
            profile = "Main 10"
        )
        assertThat(item.isDolbyVision).isFalse()
    }

    // ==================== Video Quality Label Tests ====================

    @Test
    fun `videoQualityLabel shows 4K Dolby Vision`() {
        val item = createItemWithVideo(
            width = 3840,
            height = 2160,
            videoRangeType = "DOVIWithHDR10"
        )
        assertThat(item.videoQualityLabel).isEqualTo("4K · Dolby Vision")
    }

    @Test
    fun `videoQualityLabel shows 1080p HDR`() {
        val item = createItemWithVideo(
            width = 1920,
            height = 1080,
            videoRangeType = "HDR10"
        )
        assertThat(item.videoQualityLabel).isEqualTo("1080p · HDR")
    }

    @Test
    fun `videoQualityLabel shows 720p for SD content`() {
        val item = createItemWithVideo(
            width = 1280,
            height = 720,
            videoRangeType = "SDR"
        )
        assertThat(item.videoQualityLabel).isEqualTo("720p")
    }

    // ==================== Serialization Tests ====================

    @Test
    fun `JellyfinItem deserializes from JSON`() {
        val jsonString = """
            {
                "Id": "abc123",
                "Name": "Test Movie",
                "Type": "Movie",
                "ProductionYear": 2024,
                "CommunityRating": 8.5,
                "RunTimeTicks": 72000000000
            }
        """.trimIndent()

        val item = json.decodeFromString<JellyfinItem>(jsonString)

        assertThat(item.id).isEqualTo("abc123")
        assertThat(item.name).isEqualTo("Test Movie")
        assertThat(item.type).isEqualTo("Movie")
        assertThat(item.productionYear).isEqualTo(2024)
        assertThat(item.communityRating).isEqualTo(8.5f)
    }

    @Test
    fun `AuthResponse deserializes correctly`() {
        val jsonString = """
            {
                "AccessToken": "token123",
                "User": {
                    "Id": "user456",
                    "Name": "TestUser"
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<AuthResponse>(jsonString)

        assertThat(response.accessToken).isEqualTo("token123")
        assertThat(response.user.id).isEqualTo("user456")
        assertThat(response.user.name).isEqualTo("TestUser")
    }

    @Test
    fun `UserData has sensible defaults`() {
        val userData = UserData()

        assertThat(userData.playbackPositionTicks).isEqualTo(0)
        assertThat(userData.playCount).isEqualTo(0)
        assertThat(userData.isFavorite).isFalse()
        assertThat(userData.played).isFalse()
    }

    // ==================== Helper Functions ====================

    private fun createItem(
        id: String = "test-id",
        name: String = "Test Item",
        type: String = "Movie",
        runTimeTicks: Long? = null,
        userData: UserData? = null,
        mediaSources: List<MediaSource>? = null
    ) = JellyfinItem(
        id = id,
        name = name,
        type = type,
        runTimeTicks = runTimeTicks,
        userData = userData,
        mediaSources = mediaSources
    )

    private fun createItemWithVideo(
        width: Int? = null,
        height: Int? = null,
        videoRangeType: String? = null,
        videoRange: String? = null,
        profile: String? = null,
        displayTitle: String? = null
    ): JellyfinItem {
        val videoStream = MediaStream(
            index = 0,
            type = "Video",
            codec = "hevc",
            width = width,
            height = height,
            videoRangeType = videoRangeType,
            videoRange = videoRange,
            profile = profile,
            displayTitle = displayTitle
        )
        val mediaSource = MediaSource(
            id = "source-1",
            mediaStreams = listOf(videoStream)
        )
        return createItem(mediaSources = listOf(mediaSource))
    }
}

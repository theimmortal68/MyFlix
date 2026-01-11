package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSource
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.UserData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerScreenStateTest {

    private lateinit var mockReporter: PlaybackReporter
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val movieItem = JellyfinItem(
        id = "movie-1",
        name = "Test Movie",
        type = "Movie",
        userData = UserData(
            playbackPositionTicks = 600_000_000L, // 1 minute in ticks (10,000 ticks per ms)
        ),
        mediaSources = listOf(
            MediaSource(
                id = "source-1",
                mediaStreams = listOf(
                    MediaStream(
                        index = 0,
                        type = "Video",
                        codec = "h264",
                        profile = "High",
                        videoRangeType = "SDR",
                        width = 1920,
                        height = 1080,
                        bitRate = 8_000_000L,
                    ),
                ),
            ),
        ),
    )

    private val movieWithoutResume = JellyfinItem(
        id = "movie-2",
        name = "Test Movie 2",
        type = "Movie",
        userData = null,
    )

    @Before
    fun setUp() {
        mockReporter = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)
    }

    // region Initial State Tests

    @Test
    fun `initial state has correct itemId and isLoading true`() {
        val state = createState("movie-1")

        assertEquals("movie-1", state.itemId)
        assertTrue(state.isLoading)
        assertNull(state.item)
        assertNull(state.streamUrl)
        assertEquals(0L, state.startPositionMs)
        assertNull(state.error)
        assertFalse(state.playerReady)
        assertTrue(state.showControls)
    }

    @Test
    fun `initial mediaInfo is null`() {
        val state = createState("movie-1")

        assertNull(state.mediaInfo)
    }

    // endregion

    // region loadItem Tests

    @Test
    fun `loadItem loads item and stream URL successfully`() = testScope.runTest {
        coEvery { mockReporter.loadItem("movie-1") } returns Result.success(movieItem)
        coEvery { mockReporter.getStreamUrl("movie-1") } returns "http://server/stream/movie-1"

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals(movieItem, state.item)
        assertEquals("http://server/stream/movie-1", state.streamUrl)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadItem calculates startPositionMs from playback position ticks`() = testScope.runTest {
        coEvery { mockReporter.loadItem("movie-1") } returns Result.success(movieItem)
        coEvery { mockReporter.getStreamUrl("movie-1") } returns "http://server/stream"

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        // 600,000,000 ticks / 10,000 = 60,000 ms = 1 minute
        assertEquals(60_000L, state.startPositionMs)
    }

    @Test
    fun `loadItem sets startPositionMs to 0 when no resume position`() = testScope.runTest {
        coEvery { mockReporter.loadItem("movie-2") } returns Result.success(movieWithoutResume)
        coEvery { mockReporter.getStreamUrl("movie-2") } returns "http://server/stream"

        val state = createState("movie-2")
        state.loadItem()
        advanceUntilIdle()

        assertEquals(0L, state.startPositionMs)
    }

    @Test
    fun `loadItem sets error on failure`() = testScope.runTest {
        coEvery { mockReporter.loadItem("movie-1") } returns Result.failure(Exception("Network error"))

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals("Network error", state.error)
        assertFalse(state.isLoading)
        assertNull(state.item)
    }

    @Test
    fun `mediaInfo is populated after loadItem`() = testScope.runTest {
        coEvery { mockReporter.loadItem("movie-1") } returns Result.success(movieItem)
        coEvery { mockReporter.getStreamUrl("movie-1") } returns "http://server/stream"

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        val mediaInfo = state.mediaInfo
        assertNotNull(mediaInfo)
        assertEquals("Test Movie", mediaInfo.title)
        assertEquals("h264", mediaInfo.videoCodec)
        assertEquals("High", mediaInfo.videoProfile)
        assertEquals("SDR", mediaInfo.videoRangeType)
        assertEquals(1920, mediaInfo.width)
        assertEquals(1080, mediaInfo.height)
        assertEquals(8_000_000L, mediaInfo.bitrate)
    }

    // endregion

    // region Playback Reporting Tests

    @Test
    fun `onPlaybackStarted reports playback start`() = testScope.runTest {
        val state = createState("movie-1")

        state.onPlaybackStarted(30_000L) // 30 seconds
        // Use advanceTimeBy to let the coroutine start without waiting for the infinite loop
        advanceTimeBy(100L)

        // 30,000 ms * 10,000 = 300,000,000 ticks
        coVerify { mockReporter.reportPlaybackStart("movie-1", 300_000_000L) }

        state.cleanup()
    }

    @Test
    fun `onPlaybackStarted is idempotent`() = testScope.runTest {
        val state = createState("movie-1")

        state.onPlaybackStarted(30_000L)
        advanceTimeBy(100L)
        state.onPlaybackStarted(60_000L)
        advanceTimeBy(100L)

        // Should only be called once
        coVerify(exactly = 1) { mockReporter.reportPlaybackStart(any(), any()) }

        state.cleanup()
    }

    @Test
    fun `reportProgress reports progress with position in ticks`() = testScope.runTest {
        val state = createState("movie-1")

        // Must start playback first
        state.onPlaybackStarted(0L)
        advanceTimeBy(100L)

        state.reportProgress(60_000L, isPaused = false)
        advanceTimeBy(100L)

        coVerify { mockReporter.reportPlaybackProgress("movie-1", 600_000_000L, false) }

        state.cleanup()
    }

    @Test
    fun `reportProgress does nothing if playback not started`() = testScope.runTest {
        val state = createState("movie-1")

        state.reportProgress(60_000L, isPaused = false)
        advanceTimeBy(100L)

        coVerify(exactly = 0) { mockReporter.reportPlaybackProgress(any(), any(), any()) }
    }

    @Test
    fun `onPauseStateChanged reports pause state`() = testScope.runTest {
        val state = createState("movie-1")
        state.onPlaybackStarted(0L)
        advanceTimeBy(100L)

        state.onPauseStateChanged(60_000L, isPaused = true)
        advanceTimeBy(100L)

        coVerify { mockReporter.reportPlaybackProgress("movie-1", 600_000_000L, true) }

        state.cleanup()
    }

    @Test
    fun `reportPlaybackStopped reports stopped with position`() = testScope.runTest {
        val state = createState("movie-1")

        state.reportPlaybackStopped(120_000L) // 2 minutes

        coVerify { mockReporter.reportPlaybackStopped("movie-1", 1_200_000_000L) }
    }

    // endregion

    // region checkVideoCompletion Tests

    @Test
    fun `checkVideoCompletion marks as played at 95 percent`() = testScope.runTest {
        val state = createState("movie-1")
        state.onPlaybackStarted(0L)
        advanceTimeBy(100L)

        // 95% of 100,000ms = 95,000ms
        state.checkVideoCompletion(95_000L, 100_000L)
        advanceTimeBy(100L)

        coVerify { mockReporter.setPlayed("movie-1", true) }

        state.cleanup()
    }

    @Test
    fun `checkVideoCompletion does not mark as played below 95 percent`() = testScope.runTest {
        val state = createState("movie-1")
        state.onPlaybackStarted(0L)
        advanceTimeBy(100L)

        // 90% of 100,000ms = 90,000ms
        state.checkVideoCompletion(90_000L, 100_000L)
        advanceTimeBy(100L)

        coVerify(exactly = 0) { mockReporter.setPlayed(any(), any()) }

        state.cleanup()
    }

    @Test
    fun `checkVideoCompletion only marks once`() = testScope.runTest {
        val state = createState("movie-1")
        state.onPlaybackStarted(0L)
        advanceTimeBy(100L)

        state.checkVideoCompletion(96_000L, 100_000L)
        advanceTimeBy(100L)
        state.checkVideoCompletion(98_000L, 100_000L)
        advanceTimeBy(100L)

        coVerify(exactly = 1) { mockReporter.setPlayed("movie-1", true) }

        state.cleanup()
    }

    @Test
    fun `checkVideoCompletion does nothing if playback not started`() = testScope.runTest {
        val state = createState("movie-1")

        state.checkVideoCompletion(95_000L, 100_000L)
        advanceTimeBy(100L)

        coVerify(exactly = 0) { mockReporter.setPlayed(any(), any()) }
    }

    @Test
    fun `checkVideoCompletion does nothing with invalid duration`() = testScope.runTest {
        val state = createState("movie-1")
        state.onPlaybackStarted(0L)
        advanceTimeBy(100L)

        state.checkVideoCompletion(95_000L, 0L)
        advanceTimeBy(100L)

        coVerify(exactly = 0) { mockReporter.setPlayed(any(), any()) }

        state.cleanup()
    }

    // endregion

    // region Controls Visibility Tests

    @Test
    fun `showControls shows controls`() {
        val state = createState("movie-1")
        state.hideControls()
        assertFalse(state.showControls)

        state.showControls()

        assertTrue(state.showControls)
    }

    @Test
    fun `hideControls hides controls`() {
        val state = createState("movie-1")
        assertTrue(state.showControls)

        state.hideControls()

        assertFalse(state.showControls)
    }

    @Test
    fun `toggleControls toggles visibility`() {
        val state = createState("movie-1")
        assertTrue(state.showControls)

        state.toggleControls()
        assertFalse(state.showControls)

        state.toggleControls()
        assertTrue(state.showControls)
    }

    @Test
    fun `resetControlsHideTimer auto-hides after delay`() = testScope.runTest {
        val state = createState("movie-1")
        state.showControls()

        state.resetControlsHideTimer()
        advanceTimeBy(4_999L)
        assertTrue(state.showControls)

        advanceTimeBy(2L) // Total 5001ms
        assertFalse(state.showControls)
    }

    @Test
    fun `resetControlsHideTimer cancels previous timer`() = testScope.runTest {
        val state = createState("movie-1")
        state.showControls()

        state.resetControlsHideTimer()
        advanceTimeBy(3_000L)

        // Reset timer
        state.resetControlsHideTimer()
        advanceTimeBy(3_000L)

        // Only 3 seconds since reset, should still show
        assertTrue(state.showControls)

        advanceTimeBy(2_001L) // Total 5001ms since reset
        assertFalse(state.showControls)
    }

    // endregion

    // region cleanup Tests

    @Test
    fun `cleanup cancels controls hide timer`() = testScope.runTest {
        val state = createState("movie-1")
        state.showControls()
        state.resetControlsHideTimer()

        // Cleanup before the timer fires
        state.cleanup()

        // After cleanup, timer should be cancelled
        advanceTimeBy(10_000L)
        // Controls should still be showing because timer was cancelled
        assertTrue(state.showControls)
    }

    @Test
    fun `cleanup cancels progress reporting job`() = testScope.runTest {
        val state = createState("movie-1")
        state.onPlaybackStarted(0L)
        // Don't call advanceUntilIdle - just cleanup immediately
        state.cleanup()
        // Test passes if no UncompletedCoroutinesError is thrown
    }

    // endregion

    // Helper methods

    private fun createState(itemId: String): PlayerScreenState {
        return PlayerScreenState(itemId, mockReporter, testScope)
    }
}

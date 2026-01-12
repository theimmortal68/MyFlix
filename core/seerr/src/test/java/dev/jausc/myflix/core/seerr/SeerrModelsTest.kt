package dev.jausc.myflix.core.seerr

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for Seerr data models and computed properties.
 */
class SeerrModelsTest {

    // ==================== SeerrMedia Tests ====================

    @Test
    fun `displayTitle prefers title over name`() {
        val media = SeerrMedia(id = 1, title = "Movie Title", name = "TV Name")
        assertEquals("Movie Title", media.displayTitle)
    }

    @Test
    fun `displayTitle falls back to name when title is null`() {
        val media = SeerrMedia(id = 1, name = "TV Show Name")
        assertEquals("TV Show Name", media.displayTitle)
    }

    @Test
    fun `displayTitle falls back to originalTitle when title and name are null`() {
        val media = SeerrMedia(id = 1, originalTitle = "Original Title")
        assertEquals("Original Title", media.displayTitle)
    }

    @Test
    fun `displayTitle falls back to originalName when others are null`() {
        val media = SeerrMedia(id = 1, originalName = "Original Name")
        assertEquals("Original Name", media.displayTitle)
    }

    @Test
    fun `displayTitle returns Unknown when all title fields are null`() {
        val media = SeerrMedia(id = 1)
        assertEquals("Unknown", media.displayTitle)
    }

    @Test
    fun `year extracts from releaseDate for movies`() {
        val media = SeerrMedia(id = 1, releaseDate = "2024-06-15")
        assertEquals(2024, media.year)
    }

    @Test
    fun `year extracts from firstAirDate for TV shows`() {
        val media = SeerrMedia(id = 1, firstAirDate = "2023-11-20")
        assertEquals(2023, media.year)
    }

    @Test
    fun `year prefers releaseDate over firstAirDate`() {
        val media = SeerrMedia(id = 1, releaseDate = "2024-01-01", firstAirDate = "2023-06-01")
        assertEquals(2024, media.year)
    }

    @Test
    fun `year returns null when no date is present`() {
        val media = SeerrMedia(id = 1)
        assertNull(media.year)
    }

    @Test
    fun `year returns null for invalid date format`() {
        val media = SeerrMedia(id = 1, releaseDate = "invalid")
        assertNull(media.year)
    }

    @Test
    fun `isAvailable returns true when status is 5`() {
        val media = SeerrMedia(id = 1, mediaInfo = SeerrMediaInfo(status = 5))
        assertTrue(media.isAvailable)
    }

    @Test
    fun `isAvailable returns false when status is not 5`() {
        val media = SeerrMedia(id = 1, mediaInfo = SeerrMediaInfo(status = 2))
        assertFalse(media.isAvailable)
    }

    @Test
    fun `isAvailable returns false when mediaInfo is null`() {
        val media = SeerrMedia(id = 1)
        assertFalse(media.isAvailable)
    }

    @Test
    fun `isPending returns true when status is 2`() {
        val media = SeerrMedia(id = 1, mediaInfo = SeerrMediaInfo(status = 2))
        assertTrue(media.isPending)
    }

    @Test
    fun `isPending returns true when status is 3`() {
        val media = SeerrMedia(id = 1, mediaInfo = SeerrMediaInfo(status = 3))
        assertTrue(media.isPending)
    }

    @Test
    fun `isPending returns false when status is 5`() {
        val media = SeerrMedia(id = 1, mediaInfo = SeerrMediaInfo(status = 5))
        assertFalse(media.isPending)
    }

    @Test
    fun `isMovie returns true for mediaType movie`() {
        val media = SeerrMedia(id = 1, mediaType = "movie")
        assertTrue(media.isMovie)
    }

    @Test
    fun `isMovie returns true when mediaType is empty but title is present`() {
        val media = SeerrMedia(id = 1, mediaType = "", title = "Movie Title")
        assertTrue(media.isMovie)
    }

    @Test
    fun `isMovie returns false for TV show`() {
        val media = SeerrMedia(id = 1, mediaType = "tv")
        assertFalse(media.isMovie)
    }

    @Test
    fun `isTvShow returns true for mediaType tv`() {
        val media = SeerrMedia(id = 1, mediaType = "tv")
        assertTrue(media.isTvShow)
    }

    @Test
    fun `isTvShow returns true when mediaType is empty but name is present`() {
        val media = SeerrMedia(id = 1, mediaType = "", name = "TV Show Name")
        assertTrue(media.isTvShow)
    }

    @Test
    fun `isTvShow returns false for movie`() {
        val media = SeerrMedia(id = 1, mediaType = "movie")
        assertFalse(media.isTvShow)
    }

    @Test
    fun `availabilityStatus returns mediaInfo status`() {
        val media = SeerrMedia(id = 1, mediaInfo = SeerrMediaInfo(status = 4))
        assertEquals(4, media.availabilityStatus)
    }

    @Test
    fun `availabilityStatus returns null when mediaInfo is null`() {
        val media = SeerrMedia(id = 1)
        assertNull(media.availabilityStatus)
    }

    // ==================== SeerrRequest Tests ====================

    @Test
    fun `isPendingApproval returns true for status 1`() {
        val request = SeerrRequest(id = 1, status = 1)
        assertTrue(request.isPendingApproval)
    }

    @Test
    fun `isApproved returns true for status 2`() {
        val request = SeerrRequest(id = 1, status = 2)
        assertTrue(request.isApproved)
    }

    @Test
    fun `isDeclined returns true for status 3`() {
        val request = SeerrRequest(id = 1, status = 3)
        assertTrue(request.isDeclined)
    }

    @Test
    fun `statusText returns Pending for status 1`() {
        val request = SeerrRequest(id = 1, status = 1)
        assertEquals("Pending", request.statusText)
    }

    @Test
    fun `statusText returns Approved for status 2`() {
        val request = SeerrRequest(id = 1, status = 2)
        assertEquals("Approved", request.statusText)
    }

    @Test
    fun `statusText returns Declined for status 3`() {
        val request = SeerrRequest(id = 1, status = 3)
        assertEquals("Declined", request.statusText)
    }

    @Test
    fun `statusText returns Unknown for other statuses`() {
        val request = SeerrRequest(id = 1, status = 99)
        assertEquals("Unknown", request.statusText)
    }

    // ==================== SeerrMediaStatus Tests ====================

    @Test
    fun `SeerrMediaStatus constants have correct values`() {
        assertEquals(1, SeerrMediaStatus.UNKNOWN)
        assertEquals(2, SeerrMediaStatus.PENDING)
        assertEquals(3, SeerrMediaStatus.PROCESSING)
        assertEquals(4, SeerrMediaStatus.PARTIALLY_AVAILABLE)
        assertEquals(5, SeerrMediaStatus.AVAILABLE)
    }

    @Test
    fun `SeerrMediaStatus toDisplayString returns correct strings`() {
        assertEquals("Not Requested", SeerrMediaStatus.toDisplayString(1))
        assertEquals("Pending", SeerrMediaStatus.toDisplayString(2))
        assertEquals("Processing", SeerrMediaStatus.toDisplayString(3))
        assertEquals("Partially Available", SeerrMediaStatus.toDisplayString(4))
        assertEquals("Available", SeerrMediaStatus.toDisplayString(5))
        assertEquals("Unknown", SeerrMediaStatus.toDisplayString(99))
        assertEquals("Unknown", SeerrMediaStatus.toDisplayString(null))
    }

    // ==================== SeerrRequestStatus Tests ====================

    @Test
    fun `SeerrRequestStatus constants have correct values`() {
        assertEquals(1, SeerrRequestStatus.PENDING_APPROVAL)
        assertEquals(2, SeerrRequestStatus.APPROVED)
        assertEquals(3, SeerrRequestStatus.DECLINED)
    }

    @Test
    fun `SeerrRequestStatus toDisplayString returns correct strings`() {
        assertEquals("Pending Approval", SeerrRequestStatus.toDisplayString(1))
        assertEquals("Approved", SeerrRequestStatus.toDisplayString(2))
        assertEquals("Declined", SeerrRequestStatus.toDisplayString(3))
        assertEquals("Unknown", SeerrRequestStatus.toDisplayString(99))
    }
}

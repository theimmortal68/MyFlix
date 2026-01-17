package dev.jausc.myflix.core.common.util

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for NavigationHelper.
 */
class NavigationHelperTest {

    // ==================== encodeNavArg Tests ====================

    @Test
    fun `encodeNavArg encodes spaces as plus signs`() {
        val result = NavigationHelper.encodeNavArg("Sci-Fi Movies")
        assertEquals("Sci-Fi+Movies", result)
    }

    @Test
    fun `encodeNavArg encodes special characters`() {
        val result = NavigationHelper.encodeNavArg("Action & Adventure")
        assertEquals("Action+%26+Adventure", result)
    }

    @Test
    fun `encodeNavArg handles empty string`() {
        val result = NavigationHelper.encodeNavArg("")
        assertEquals("", result)
    }

    @Test
    fun `encodeNavArg handles string with no special characters`() {
        val result = NavigationHelper.encodeNavArg("Movies")
        assertEquals("Movies", result)
    }

    @Test
    fun `encodeNavArg encodes forward slashes`() {
        val result = NavigationHelper.encodeNavArg("TV/Shows")
        assertEquals("TV%2FShows", result)
    }

    @Test
    fun `encodeNavArg encodes question marks`() {
        val result = NavigationHelper.encodeNavArg("What?")
        assertEquals("What%3F", result)
    }

    // ==================== decodeNavArg Tests ====================

    @Test
    fun `decodeNavArg decodes plus signs as spaces`() {
        val result = NavigationHelper.decodeNavArg("Sci-Fi+Movies")
        assertEquals("Sci-Fi Movies", result)
    }

    @Test
    fun `decodeNavArg decodes percent-encoded characters`() {
        val result = NavigationHelper.decodeNavArg("Action+%26+Adventure")
        assertEquals("Action & Adventure", result)
    }

    @Test
    fun `decodeNavArg handles empty string`() {
        val result = NavigationHelper.decodeNavArg("")
        assertEquals("", result)
    }

    @Test
    fun `decodeNavArg handles string with no encoded characters`() {
        val result = NavigationHelper.decodeNavArg("Movies")
        assertEquals("Movies", result)
    }

    @Test
    fun `decodeNavArg decodes forward slashes`() {
        val result = NavigationHelper.decodeNavArg("TV%2FShows")
        assertEquals("TV/Shows", result)
    }

    // ==================== Roundtrip Tests ====================

    @Test
    fun `encode then decode returns original string`() {
        val original = "Sci-Fi & Fantasy Movies (2024)"
        val encoded = NavigationHelper.encodeNavArg(original)
        val decoded = NavigationHelper.decodeNavArg(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `encode then decode handles unicode characters`() {
        val original = "日本語タイトル"
        val encoded = NavigationHelper.encodeNavArg(original)
        val decoded = NavigationHelper.decodeNavArg(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `encode then decode handles emojis`() {
        val original = "Movies 🎬"
        val encoded = NavigationHelper.encodeNavArg(original)
        val decoded = NavigationHelper.decodeNavArg(encoded)
        assertEquals(original, decoded)
    }

    // ==================== buildLibraryRoute Tests ====================

    @Test
    fun `buildLibraryRoute creates correct route format`() {
        val route = NavigationHelper.buildLibraryRoute("lib123", "Movies")
        assertEquals("library/lib123/Movies", route)
    }

    @Test
    fun `buildLibraryRoute encodes library name with spaces`() {
        val route = NavigationHelper.buildLibraryRoute("lib456", "Sci-Fi Movies")
        assertEquals("library/lib456/Sci-Fi+Movies", route)
    }

    @Test
    fun `buildLibraryRoute encodes library name with special characters`() {
        val route = NavigationHelper.buildLibraryRoute("lib789", "TV & Movies")
        assertEquals("library/lib789/TV+%26+Movies", route)
    }

    // ==================== Route Constants Tests ====================

    @Test
    fun `SEERR_SEARCH_ROUTE has correct value`() {
        assertEquals("seerr/search", NavigationHelper.SEERR_SEARCH_ROUTE)
    }

    @Test
    fun `SEERR_REQUESTS_ROUTE has correct value`() {
        assertEquals("seerr/requests", NavigationHelper.SEERR_REQUESTS_ROUTE)
    }

    // ==================== buildSeerrDiscoverRoute Tests ====================

    @Test
    fun `buildSeerrDiscoverRoute creates correct route format`() {
        val route = NavigationHelper.buildSeerrDiscoverRoute("trending")
        assertEquals("seerr/discover/trending", route)
    }

    @Test
    fun `buildSeerrDiscoverRoute encodes category with spaces`() {
        val route = NavigationHelper.buildSeerrDiscoverRoute("popular movies")
        assertEquals("seerr/discover/popular+movies", route)
    }

    @Test
    fun `buildSeerrDiscoverRoute encodes category with special characters`() {
        val route = NavigationHelper.buildSeerrDiscoverRoute("Action & Adventure")
        assertEquals("seerr/discover/Action+%26+Adventure", route)
    }

    // ==================== buildSeerrCollectionRoute Tests ====================

    @Test
    fun `buildSeerrCollectionRoute creates correct route format`() {
        val route = NavigationHelper.buildSeerrCollectionRoute(12345)
        assertEquals("seerr/collection/12345", route)
    }

    @Test
    fun `buildSeerrCollectionRoute handles single digit id`() {
        val route = NavigationHelper.buildSeerrCollectionRoute(1)
        assertEquals("seerr/collection/1", route)
    }

    @Test
    fun `buildSeerrCollectionRoute handles large id`() {
        val route = NavigationHelper.buildSeerrCollectionRoute(999999999)
        assertEquals("seerr/collection/999999999", route)
    }

    // ==================== buildDetailRoute Tests ====================

    @Test
    fun `buildDetailRoute creates correct route format`() {
        val route = NavigationHelper.buildDetailRoute("item123")
        assertEquals("detail/item123", route)
    }
}

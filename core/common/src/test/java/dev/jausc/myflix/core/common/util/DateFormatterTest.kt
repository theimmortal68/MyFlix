package dev.jausc.myflix.core.common.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for DateFormatter.
 */
class DateFormatterTest {

    // ==================== formatFull Tests ====================

    @Test
    fun `formatFull returns null for null input`() {
        val result = DateFormatter.formatFull(null)
        assertNull(result)
    }

    @Test
    fun `formatFull formats ISO date correctly`() {
        val result = DateFormatter.formatFull("2025-11-14")
        assertEquals("November 14, 2025", result)
    }

    @Test
    fun `formatFull formats January date correctly`() {
        val result = DateFormatter.formatFull("2024-01-01")
        assertEquals("January 1, 2024", result)
    }

    @Test
    fun `formatFull formats December date correctly`() {
        val result = DateFormatter.formatFull("2024-12-31")
        assertEquals("December 31, 2024", result)
    }

    @Test
    fun `formatFull returns original string for invalid format`() {
        val result = DateFormatter.formatFull("invalid-date")
        assertEquals("invalid-date", result)
    }

    @Test
    fun `formatFull returns original string for partial date`() {
        val result = DateFormatter.formatFull("2024-01")
        assertEquals("2024-01", result)
    }

    @Test
    fun `formatFull returns original string for empty string`() {
        val result = DateFormatter.formatFull("")
        assertEquals("", result)
    }

    @Test
    fun `formatFull handles leap year date`() {
        val result = DateFormatter.formatFull("2024-02-29")
        assertEquals("February 29, 2024", result)
    }

    @Test
    fun `formatFull handles date with time suffix gracefully`() {
        // ISO date with time should fail to parse as LocalDate and return original
        val result = DateFormatter.formatFull("2024-01-15T10:30:00")
        assertEquals("2024-01-15T10:30:00", result)
    }

    // ==================== formatBirthday Tests ====================

    @Test
    fun `formatBirthday returns null for null input`() {
        val result = DateFormatter.formatBirthday(null)
        assertNull(result)
    }

    @Test
    fun `formatBirthday formats with Born prefix`() {
        val result = DateFormatter.formatBirthday("1985-11-14")
        assertEquals("Born November 14, 1985", result)
    }

    @Test
    fun `formatBirthday handles various dates`() {
        val result = DateFormatter.formatBirthday("1990-06-25")
        assertEquals("Born June 25, 1990", result)
    }

    @Test
    fun `formatBirthday returns Born with original for invalid date`() {
        val result = DateFormatter.formatBirthday("invalid")
        assertEquals("Born invalid", result)
    }

    // ==================== extractYear Tests ====================

    @Test
    fun `extractYear returns null for null input`() {
        val result = DateFormatter.extractYear(null)
        assertNull(result)
    }

    @Test
    fun `extractYear extracts year from ISO date`() {
        val result = DateFormatter.extractYear("2024-06-15")
        assertEquals(2024, result)
    }

    @Test
    fun `extractYear extracts year from date string`() {
        val result = DateFormatter.extractYear("1999-12-31")
        assertEquals(1999, result)
    }

    @Test
    fun `extractYear extracts from partial date using fallback`() {
        // Should fall back to taking first 4 chars
        val result = DateFormatter.extractYear("2020")
        assertEquals(2020, result)
    }

    @Test
    fun `extractYear extracts from year-only string`() {
        val result = DateFormatter.extractYear("2025")
        assertEquals(2025, result)
    }

    @Test
    fun `extractYear returns null for non-numeric string`() {
        val result = DateFormatter.extractYear("abc")
        assertNull(result)
    }

    @Test
    fun `extractYear returns null for empty string`() {
        val result = DateFormatter.extractYear("")
        assertNull(result)
    }

    @Test
    fun `extractYear handles date with time`() {
        // Full ISO datetime should fall back to taking first 4 chars which works
        val result = DateFormatter.extractYear("2024-01-15T10:30:00")
        assertEquals(2024, result)
    }

    @Test
    fun `extractYear handles early years`() {
        val result = DateFormatter.extractYear("1900-01-01")
        assertEquals(1900, result)
    }

    @Test
    fun `extractYear handles future years`() {
        val result = DateFormatter.extractYear("2099-12-31")
        assertEquals(2099, result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `formatFull handles single digit day`() {
        val result = DateFormatter.formatFull("2024-03-05")
        assertEquals("March 5, 2024", result)
    }

    @Test
    fun `formatFull handles single digit month`() {
        val result = DateFormatter.formatFull("2024-09-15")
        assertEquals("September 15, 2024", result)
    }
}

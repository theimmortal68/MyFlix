package dev.jausc.myflix.core.common.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility for formatting dates consistently across the app.
 */
object DateFormatter {
    private val fullDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)

    /**
     * Format a date string (ISO format: yyyy-MM-dd) to full format (e.g., "November 14, 2025").
     * Returns the original string if parsing fails.
     */
    fun formatFull(dateString: String?): String? {
        if (dateString == null) return null
        return try {
            val date = LocalDate.parse(dateString)
            date.format(fullDateFormatter)
        } catch (_: Exception) {
            dateString
        }
    }

    /**
     * Format a birthday with "Born" prefix (e.g., "Born November 14, 1985").
     * Returns null if the date string is null.
     */
    fun formatBirthday(dateString: String?): String? {
        val formatted = formatFull(dateString) ?: return null
        return "Born $formatted"
    }

    /**
     * Extract the year from a date string (ISO format: yyyy-MM-dd).
     * Returns null if parsing fails.
     */
    fun extractYear(dateString: String?): Int? {
        if (dateString == null) return null
        return try {
            val date = LocalDate.parse(dateString)
            date.year
        } catch (_: Exception) {
            dateString.take(4).toIntOrNull()
        }
    }
}

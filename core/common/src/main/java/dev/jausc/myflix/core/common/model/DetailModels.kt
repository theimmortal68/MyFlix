package dev.jausc.myflix.core.common.model

/**
 * Represents a single detail info item (label-value pair).
 * Used in detail screens to display metadata like Director, Writer, Studio, etc.
 */
data class DetailInfoItem(
    val label: String,
    val value: String,
)

/**
 * Represents an external link item (e.g., IMDb, TMDb, Trakt).
 * Used in detail screens to display links to external services.
 */
data class ExternalLinkItem(
    val label: String,
    val url: String,
)

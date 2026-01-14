package dev.jausc.myflix.core.player

/**
 * Display modes for video rendering within the player surface.
 */
enum class PlayerDisplayMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    STRETCH("Stretch"),
}

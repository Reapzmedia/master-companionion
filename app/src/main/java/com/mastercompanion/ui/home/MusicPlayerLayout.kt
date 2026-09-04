package com.mastercompanion.ui.home

/**
 * Presentation modes for the Music Dashboard page.
 */
enum class MusicPlayerLayout(val displayName: String) {
    /** Native Spotify car view 2-column landscape & bespoke mobile portrait */
    STANDARD("Standard Car View"),

    /** Turntable standby with spinning realistic vinyl record, tonearm & RPM badge */
    VINYL("Vinyl Turntable"),

    /** Synchronized karaoke-style scrolling lyrics tracking playback progress */
    LYRICS("Synced Lyrics"),

    /** High-visibility standby desk clock with compact playback telemetry */
    MINIMAL("Desk Clock Minimal"),

    /** Edge-to-edge full bleed artwork backdrop with floating glass controls */
    FULL_BLEED("Full-Bleed Artwork")
}

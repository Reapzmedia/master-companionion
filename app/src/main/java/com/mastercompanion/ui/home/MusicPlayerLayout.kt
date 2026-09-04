package com.mastercompanion.ui.home

/**
 * Presentation modes for the Music Dashboard page.
 */
enum class MusicPlayerLayout {
    /** Full playback controls, artwork, seekbar and secondary action row */
    STANDARD,

    /** Synchronized karaoke-style scrolling lyrics tracking playback progress */
    LYRICS,

    /** High-visibility standby clock with compact playback telemetry */
    MINIMAL
}

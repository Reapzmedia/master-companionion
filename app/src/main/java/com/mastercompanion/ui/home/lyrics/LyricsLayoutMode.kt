package com.mastercompanion.ui.home.lyrics

/**
 * 5 Presentation Faces for the Synced Lyrics Mode.
 */
enum class LyricsLayoutMode(
    val title: String,
    val subtitle: String
) {
    /** Classic Standby: Album Art & Controls on Left, Karaoke Lyrics on Right */
    CLASSIC_SPLIT("Classic Split", "Art & playback on left, synced lyrics on right"),

    /** Inverted Standby: Karaoke Lyrics on Left, Album Art & Controls on Right */
    SWAPPED_SPLIT("Swapped Split", "Synced lyrics on left, art & playback on right"),

    /** Top Banner: Compact song metadata banner on top, expansive lyrics below */
    TOP_BANNER("Top Banner", "Track banner on top, expansive lyrics below"),

    /** Pure Fullscreen Centered: Distraction-free karaoke lyrics centered */
    FULLSCREEN_CENTER("Pure Centered", "Distraction-free karaoke lyrics centered on screen"),

    /** Pure Fullscreen Left: Distraction-free karaoke lyrics snapped to left margin */
    FULLSCREEN_LEFT("Left Snapped", "Large karaoke lyrics aligned to left margin");

    companion object {
        fun fromIndex(index: Int): LyricsLayoutMode {
            val all = values()
            return if (index in all.indices) all[index] else CLASSIC_SPLIT
        }
    }
}

package com.mastercompanion.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val startTimeMs: Long,
    val words: String
)

@Serializable
data class TrackLyrics(
    val trackId: String,
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = true
) {
    /**
     * Determines the active lyric line index based on current playback progress.
     * Returns -1 if playback hasn't reached the first line yet.
     */
    fun getCurrentLineIndex(progressMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].startTimeMs <= progressMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }

    companion object {
        /**
         * Sample mock lyrics for "Black and Yellow" by Wiz Khalifa
         */
        val MockBlackAndYellow = TrackLyrics(
            trackId = "wiz_black_and_yellow",
            lines = listOf(
                LyricLine(0L, "Yeah, uh-huh, you know what it is"),
                LyricLine(4000L, "Black and yellow, black and yellow"),
                LyricLine(8000L, "Black and yellow, black and yellow"),
                LyricLine(12000L, "Yeah, uh-huh, you know what it is"),
                LyricLine(16000L, "Black and yellow, black and yellow"),
                LyricLine(20000L, "Black and yellow, black and yellow"),
                LyricLine(24000L, "Everything I do, I do it big"),
                LyricLine(28000L, "Yeah, uh-huh, screaming, that's nothin'"),
                LyricLine(32000L, "When I'm off in the club, put it down"),
                LyricLine(36000L, "Got a call from my jeweler, same thing, 20 minutes"),
                LyricLine(40000L, "Taylor Gang or die, that's the business"),
                LyricLine(44000L, "Black and yellow, black and yellow"),
                LyricLine(48000L, "Black and yellow, black and yellow"),
                LyricLine(52000L, "Hear the engine revvin', scream at the light"),
                LyricLine(56000L, "See me when you see me, yeah, everything nice")
        )

        /**
         * Sample mock lyrics for "Intro" by Sickick (from user reference image)
         */
        val MockSickickIntro = TrackLyrics(
            trackId = "sickick_intro",
            lines = listOf(
                LyricLine(0L, "Ju-ju-just watch!"),
                LyricLine(12000L, "Ju-ju-just watch!"),
                LyricLine(24000L, "They call me the freak of the fall"),
                LyricLine(36000L, "I'm the new high and you're the same bong"),
                LyricLine(48000L, "I know I'm, I know I'm hot don't cry"),
                LyricLine(60000L, "Talksick 3 in the fall"),
                LyricLine(72000L, "Watch me take over the scene"),
                LyricLine(84000L, "Sickick mode activated")
            )
        )
    }
}

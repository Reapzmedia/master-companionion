package com.mastercompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {

    @Test
    fun `getCurrentLineIndex resolves correct line based on progressMs`() {
        val lyrics = TrackLyrics(
            trackId = "test",
            lines = listOf(
                LyricLine(0L, "Intro"),
                LyricLine(5000L, "Verse 1"),
                LyricLine(10000L, "Chorus")
            )
        )

        // Before start or at start
        assertEquals(0, lyrics.getCurrentLineIndex(0L))
        assertEquals(0, lyrics.getCurrentLineIndex(2500L))

        // At second line
        assertEquals(1, lyrics.getCurrentLineIndex(5000L))
        assertEquals(1, lyrics.getCurrentLineIndex(7500L))

        // At third line
        assertEquals(2, lyrics.getCurrentLineIndex(10000L))
        assertEquals(2, lyrics.getCurrentLineIndex(15000L))
    }

    @Test
    fun `getCurrentLineIndex returns -1 for empty lyrics`() {
        val emptyLyrics = TrackLyrics(trackId = "empty", lines = emptyList())
        assertEquals(-1, emptyLyrics.getCurrentLineIndex(5000L))
    }
}

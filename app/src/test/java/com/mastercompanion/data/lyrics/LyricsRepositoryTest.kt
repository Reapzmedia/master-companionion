package com.mastercompanion.data.lyrics

import com.mastercompanion.data.lyrics.dto.LrclibResponse
import com.mastercompanion.data.lyrics.dto.LyricsOvhResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsRepositoryTest {

    private lateinit var lrclibApi: LrclibApi
    private lateinit var lyricsOvhApi: LyricsOvhApi
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: LyricsRepository

    @Before
    fun setUp() {
        lrclibApi = mockk(relaxed = true)
        lyricsOvhApi = mockk(relaxed = true)
        repository = LyricsRepository(lrclibApi, lyricsOvhApi, testDispatcher)
    }

    @Test
    fun `sanitizeTitle removes remasters, editions, and features`() {
        assertEquals("In The End", repository.sanitizeTitle("In The End - 2020 Remaster"))
        assertEquals("Starboy", repository.sanitizeTitle("Starboy (feat. Daft Punk)"))
        assertEquals("HUMBLE.", repository.sanitizeTitle("HUMBLE. - Explicit Version"))
        assertEquals("Levitating", repository.sanitizeTitle("Levitating (feat. DaBaby) - Single Version"))
        assertEquals("Hotel California", repository.sanitizeTitle("Hotel California - 2013 Remaster"))
        assertEquals("Bohemian Rhapsody", repository.sanitizeTitle("Bohemian Rhapsody - Remastered 2011"))
        assertEquals("Creep", repository.sanitizeTitle("Creep (Acoustic)"))
        assertEquals("Numb", repository.sanitizeTitle("Numb [Live at Milton Keynes]"))
    }

    @Test
    fun `extractPrimaryArtist handles multiple collaborating artists`() {
        assertEquals("Drake", repository.extractPrimaryArtist("Drake, 21 Savage"))
        assertEquals("The Weeknd", repository.extractPrimaryArtist("The Weeknd feat. Daft Punk"))
        assertEquals("Calvin Harris", repository.extractPrimaryArtist("Calvin Harris & Dua Lipa"))
        assertEquals("Coldplay", repository.extractPrimaryArtist("Coldplay"))
    }

    @Test
    fun `parseLrc parses timestamped lines correctly`() {
        val lrc = """
            [ti:Test Song]
            [ar:Test Artist]
            [00:04.50]First line of song
            [00:10.25]Second line of song
            [01:02.00]Third line of song
        """.trimIndent()

        val parsed = repository.parseLrc(lrc, "test_key")
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(4500L, parsed.lines[0].startTimeMs)
        assertEquals("First line of song", parsed.lines[0].words)
        assertEquals(10250L, parsed.lines[1].startTimeMs)
        assertEquals(62000L, parsed.lines[2].startTimeMs)
    }

    @Test
    fun `parsePlainLyrics creates proportional paced lines`() {
        val plain = """
            Line one
            Line two
            Line three
            Line four
        """.trimIndent()

        val parsed = repository.parsePlainLyrics(plain, durationMs = 120_000L, trackId = "plain_key")
        assertFalse(parsed.isSynced)
        assertEquals(4, parsed.lines.size)
        assertEquals("Line one", parsed.lines[0].words)
        assertTrue(parsed.lines[0].startTimeMs > 0)
        assertTrue(parsed.lines[1].startTimeMs > parsed.lines[0].startTimeMs)
    }

    @Test
    fun `getLyrics falls back to clean search and secondary provider`() = runTest {
        // Raw get fails with 404
        coEvery {
            lrclibApi.getLyrics(
                trackName = "In The End - 2020 Remaster",
                artistName = "Linkin Park",
                albumName = any(),
                durationSeconds = any()
            )
        } returns Response.error(404, okhttp3.ResponseBody.create(null, "Not Found"))

        // Search with cleaned name succeeds
        val sampleLrc = "[00:15.00]It starts with one thing"
        coEvery {
            lrclibApi.getLyrics(trackName = "In The End", artistName = "Linkin Park")
        } returns Response.success(
            LrclibResponse(
                trackName = "In The End",
                artistName = "Linkin Park",
                syncedLyrics = sampleLrc
            )
        )

        val lyrics = repository.getLyrics("In The End - 2020 Remaster", "Linkin Park")
        assertNotNull(lyrics)
        assertEquals(1, lyrics?.lines?.size)
        assertEquals("It starts with one thing", lyrics?.lines?.get(0)?.words)
    }

    @Test
    fun `getLyrics falls back to LyricsOvh when LRCLIB has no lyrics`() = runTest {
        coEvery { lrclibApi.getLyrics(any(), any(), any(), any()) } returns Response.error(404, okhttp3.ResponseBody.create(null, ""))
        coEvery { lrclibApi.getLyrics(any(), any()) } returns Response.error(404, okhttp3.ResponseBody.create(null, ""))
        coEvery { lrclibApi.searchLyrics(any(), any(), any()) } returns Response.success(emptyList())

        coEvery { lyricsOvhApi.getLyrics("Coldplay", "Yellow") } returns Response.success(
            LyricsOvhResponse(lyrics = "Look at the stars\nLook how they shine for you")
        )

        val lyrics = repository.getLyrics("Yellow", "Coldplay", durationMs = 240_000L)
        assertNotNull(lyrics)
        assertEquals(2, lyrics?.lines?.size)
        assertEquals("Look at the stars", lyrics?.lines?.get(0)?.words)
        assertFalse(lyrics!!.isSynced)
    }
}

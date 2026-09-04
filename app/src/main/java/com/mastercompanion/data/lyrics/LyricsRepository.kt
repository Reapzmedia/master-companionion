package com.mastercompanion.data.lyrics

import com.mastercompanion.di.IoDispatcher
import com.mastercompanion.domain.model.LyricLine
import com.mastercompanion.domain.model.TrackLyrics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val lrclibApi: LrclibApi,
    private val lyricsOvhApi: LyricsOvhApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val cache = ConcurrentHashMap<String, TrackLyrics>()
    private val lrcPattern = Pattern.compile("^\\[(\\d{1,2}):(\\d{2}(?:\\.\\d{1,3})?)\\]\\s*(.*)$")

    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 0L
    ): TrackLyrics? = withContext(ioDispatcher) {
        if (title.isBlank() || artist.isBlank()) return@withContext null

        val cacheKey = "${artist.trim().lowercase()}_${title.trim().lowercase()}"
        cache[cacheKey]?.let { return@withContext it }

        val cleanTitle = sanitizeTitle(title)
        val primaryArtist = extractPrimaryArtist(artist)
        val durationSeconds = if (durationMs > 0) (durationMs / 1000).toInt() else null

        Timber.d("Searching lyrics for '$title' ($cleanTitle) by '$artist' ($primaryArtist)")

        var fallbackPlainLyrics: String? = null

        // ═══════════════════════════════════════════════════════════
        // Tier 1: LRCLIB Exact Match (api/get with raw metadata)
        // ═══════════════════════════════════════════════════════════
        try {
            val resp = lrclibApi.getLyrics(
                trackName = title.trim(),
                artistName = artist.trim(),
                albumName = album.trim().takeIf { it.isNotBlank() },
                durationSeconds = durationSeconds
            )
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (!body.syncedLyrics.isNullOrBlank()) {
                    val parsed = parseLrc(body.syncedLyrics, trackId = cacheKey)
                    if (parsed.lines.isNotEmpty()) {
                        cache[cacheKey] = parsed
                        Timber.i("Found synced lyrics via LRCLIB exact for '$cleanTitle'")
                        return@withContext parsed
                    }
                }
                if (!body.plainLyrics.isNullOrBlank() && fallbackPlainLyrics == null) {
                    fallbackPlainLyrics = body.plainLyrics
                }
            }
        } catch (e: Exception) {
            Timber.d("Tier 1 LRCLIB exact raw failed: ${e.message}")
        }

        // ═══════════════════════════════════════════════════════════
        // Tier 2: LRCLIB Exact with Cleaned Title & Primary Artist
        // (no album or strict duration constraint to avoid mismatch)
        // ═══════════════════════════════════════════════════════════
        if (cleanTitle != title.trim() || primaryArtist != artist.trim()) {
            try {
                val resp = lrclibApi.getLyrics(
                    trackName = cleanTitle,
                    artistName = primaryArtist
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    if (!body.syncedLyrics.isNullOrBlank()) {
                        val parsed = parseLrc(body.syncedLyrics, trackId = cacheKey)
                        if (parsed.lines.isNotEmpty()) {
                            cache[cacheKey] = parsed
                            Timber.i("Found synced lyrics via LRCLIB clean get for '$cleanTitle'")
                            return@withContext parsed
                        }
                    }
                    if (!body.plainLyrics.isNullOrBlank() && fallbackPlainLyrics == null) {
                        fallbackPlainLyrics = body.plainLyrics
                    }
                }
            } catch (e: Exception) {
                Timber.d("Tier 2 LRCLIB clean get failed: ${e.message}")
            }
        }

        // ═══════════════════════════════════════════════════════════
        // Tier 3: LRCLIB Structured Search (track_name + artist_name)
        // ═══════════════════════════════════════════════════════════
        try {
            val resp = lrclibApi.searchLyrics(
                trackName = cleanTitle,
                artistName = primaryArtist
            )
            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val list = resp.body()!!
                // First look for any item with synced lyrics
                val syncedMatch = list.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                if (syncedMatch != null) {
                    val parsed = parseLrc(syncedMatch.syncedLyrics!!, trackId = cacheKey)
                    if (parsed.lines.isNotEmpty()) {
                        cache[cacheKey] = parsed
                        Timber.i("Found synced lyrics via LRCLIB structured search for '$cleanTitle'")
                        return@withContext parsed
                    }
                }
                if (fallbackPlainLyrics == null) {
                    fallbackPlainLyrics = list.firstOrNull { !it.plainLyrics.isNullOrBlank() }?.plainLyrics
                }
            }
        } catch (e: Exception) {
            Timber.d("Tier 3 LRCLIB structured search failed: ${e.message}")
        }

        // ═══════════════════════════════════════════════════════════
        // Tier 4: LRCLIB Full-Text Fuzzy Search (query = "$cleanTitle $primaryArtist")
        // ═══════════════════════════════════════════════════════════
        try {
            val resp = lrclibApi.searchLyrics(query = "$cleanTitle $primaryArtist")
            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val list = resp.body()!!
                val syncedMatch = list.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                if (syncedMatch != null) {
                    val parsed = parseLrc(syncedMatch.syncedLyrics!!, trackId = cacheKey)
                    if (parsed.lines.isNotEmpty()) {
                        cache[cacheKey] = parsed
                        Timber.i("Found synced lyrics via LRCLIB fuzzy query for '$cleanTitle'")
                        return@withContext parsed
                    }
                }
                if (fallbackPlainLyrics == null) {
                    fallbackPlainLyrics = list.firstOrNull { !it.plainLyrics.isNullOrBlank() }?.plainLyrics
                }
            }
        } catch (e: Exception) {
            Timber.d("Tier 4 LRCLIB fuzzy search failed: ${e.message}")
        }

        // ═══════════════════════════════════════════════════════════
        // Tier 5: Secondary Lyrics API (Lyrics.ovh)
        // ═══════════════════════════════════════════════════════════
        if (fallbackPlainLyrics.isNullOrBlank()) {
            try {
                val resp = lyricsOvhApi.getLyrics(
                    artist = primaryArtist,
                    title = cleanTitle
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val text = resp.body()!!.lyrics
                    if (!text.isNullOrBlank()) {
                        fallbackPlainLyrics = text
                        Timber.i("Found plain lyrics via Lyrics.ovh for '$cleanTitle' by '$primaryArtist'")
                    }
                }
            } catch (e: Exception) {
                Timber.d("Tier 5 Lyrics.ovh failed: ${e.message}")
            }
        }

        // ═══════════════════════════════════════════════════════════
        // Tier 6: Plain Lyrics to Paced Lyrics Fallback
        // ═══════════════════════════════════════════════════════════
        if (!fallbackPlainLyrics.isNullOrBlank()) {
            val parsedPlain = parsePlainLyrics(fallbackPlainLyrics, durationMs, trackId = cacheKey)
            if (parsedPlain.lines.isNotEmpty()) {
                cache[cacheKey] = parsedPlain
                Timber.i("Using plain lyrics fallback (${parsedPlain.lines.size} lines) for '$cleanTitle'")
                return@withContext parsedPlain
            }
        }

        Timber.w("No lyrics found across all providers for '$title' by '$artist'")
        null
    }

    fun sanitizeTitle(rawTitle: String): String {
        var title = rawTitle.trim()
        // 1. Remove feat/featuring in brackets: [feat. Daft Punk] or (feat. Drake)
        title = title.replace(Regex("""\s*[\(\[]\s*(?:feat\.?|featuring|with|ft\.)\s+[^)\]]+[\)\]]""", RegexOption.IGNORE_CASE), "")
        // 2. Remove remaster/deluxe/bonus in brackets: (Remastered 2021), [Deluxe Edition], (Live), (Bonus Track)
        title = title.replace(Regex("""\s*[\(\[]\s*(?:remaster(?:ed)?(?:\s*\d{4})?|deluxe|bonus(?:\s*track)?|live(?:\s*at\s*[^)\]]+)?|radio\s*edit|acoustic|official\s*audio|official\s*video|music\s*video|single\s*version|album\s*version|explicit\s*version)\s*[\)\]]""", RegexOption.IGNORE_CASE), "")
        // 3. Remove trailing "- Remastered...", "- Live...", "- Single Version...", "- 2011 Remaster"
        title = title.replace(Regex("""\s*-\s*(?:remaster(?:ed)?(?:\s*\d{4})?|\d{4}\s*remaster(?:ed)?|deluxe(?:\s*edition)?|single(?:\s*version)?|radio(?:\s*edit)?|bonus(?:\s*track)?|live(?:\s*at\s*[^-\n]+)?|acoustic(?:\s*version)?|explicit(?:\s*version)?|mono(?:\s*version)?|stereo(?:\s*version)?|anniversary(?:\s*edition)?).*""", RegexOption.IGNORE_CASE), "")
        return title.trim().ifBlank { rawTitle.trim() }
    }

    fun extractPrimaryArtist(rawArtist: String): String {
        val delimiters = listOf(",", "&", " feat.", " ft.", " featuring ")
        var primary = rawArtist.trim()
        for (d in delimiters) {
            if (primary.contains(d, ignoreCase = true)) {
                primary = primary.split(Regex(Regex.escape(d), RegexOption.IGNORE_CASE))[0].trim()
            }
        }
        return primary.ifBlank { rawArtist.trim() }
    }

    fun parseLrc(lrcContent: String, trackId: String = ""): TrackLyrics {
        val lines = mutableListOf<LyricLine>()
        val rawLines = lrcContent.lines()

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("[ar:") || trimmed.startsWith("[al:") ||
                trimmed.startsWith("[ti:") || trimmed.startsWith("[by:") || trimmed.startsWith("[re:") ||
                trimmed.startsWith("[length:") || trimmed.startsWith("[offset:")
            ) {
                continue
            }

            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.matches()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toDoubleOrNull() ?: 0.0
                val words = matcher.group(3)?.trim() ?: ""

                val timestampMs = (minutes * 60_000L) + (seconds * 1000.0).toLong()
                lines.add(LyricLine(startTimeMs = timestampMs, words = words))
            }
        }

        lines.sortBy { it.startTimeMs }
        return TrackLyrics(trackId = trackId, lines = lines, isSynced = true)
    }

    fun parsePlainLyrics(plainText: String, durationMs: Long, trackId: String = ""): TrackLyrics {
        val rawLines = plainText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("Paroles de la chanson") }

        if (rawLines.isEmpty()) return TrackLyrics()

        val totalMs = if (durationMs > 0) durationMs else (rawLines.size * 4000L)
        val introMs = (totalMs * 0.06).toLong()
        val singingMs = (totalMs * 0.88).toLong()
        val stepMs = (singingMs / rawLines.size.coerceAtLeast(1)).coerceAtLeast(1500L)

        val lines = rawLines.mapIndexed { index, words ->
            val timestamp = (introMs + (index * stepMs)).coerceAtMost(totalMs)
            LyricLine(startTimeMs = timestamp, words = words)
        }

        return TrackLyrics(trackId = trackId, lines = lines, isSynced = false)
    }
}

package com.vibeon.music.data.lyrics

import com.vibeon.music.domain.model.LyricLine
import com.vibeon.music.domain.model.LyricsData
import com.vibeon.music.domain.model.Song
import com.vibeon.music.util.TubeJson
import com.vibeon.music.util.bool
import com.vibeon.music.util.jsonObjectOrNull
import com.vibeon.music.util.str
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches time-synced lyrics from the free LRCLIB API and parses the LRC payload
 * into timed lines. Results are cached in memory per track for the app session.
 */
@Singleton
class LyricsRepository @Inject constructor() {

    private val http = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
        expectSuccess = false
    }

    private val cache = mutableMapOf<String, LyricsData?>()

    suspend fun fetch(song: Song): LyricsData? {
        val key = "${song.videoId}|${song.title}|${song.duration}"
        if (cache.containsKey(key)) return cache[key]

        val artist = song.artists.firstOrNull()?.name.orEmpty()
        val query = buildList {
            add("track_name" to song.title)
            if (artist.isNotBlank()) add("artist_name" to artist)
            if (song.duration > 0) add("duration" to song.duration.toString())
        }.joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }

        val result = runCatching {
            val raw = http.get("https://lrclib.net/api/get?$query") {
                header("User-Agent", "VibeOn/2.0 (Android; +https://github.com/kishor-dev-970/VibeOn)")
                header("Accept", "application/json")
            }.bodyAsText()
            parse(raw)
        }.getOrNull()

        cache[key] = result
        return result
    }

    private fun parse(raw: String): LyricsData? {
        val body = TubeJson.parseToJsonElement(raw).jsonObjectOrNull ?: return null
        val instrumental = body.bool("instrumental") == true
        val synced = parseLrc(body.str("syncedLyrics"))
        val plain = parsePlain(body.str("plainLyrics"))
        if (synced.isEmpty() && plain.isEmpty()) {
            return if (instrumental) LyricsData(emptyList(), synced = false, instrumental = true) else null
        }
        return LyricsData(
            lines = if (synced.isNotEmpty()) synced else plain,
            synced = synced.isNotEmpty(),
            instrumental = instrumental,
        )
    }

    /** Parses `[mm:ss.xx] text` lines, including lines with multiple timestamps. */
    private fun parseLrc(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        val stamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
        return buildList {
            lrc.lineSequence().forEach { rawLine ->
                val matches = stamp.findAll(rawLine).toList()
                if (matches.isEmpty()) return@forEach
                val text = rawLine.substring(matches.last().range.last + 1).trim()
                if (text.isEmpty()) return@forEach
                matches.forEach { match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                    val fraction = match.groupValues[3]
                    val millis = when {
                        fraction.isBlank() -> 0L
                        fraction.length >= 3 -> fraction.take(3).toLongOrNull() ?: 0L
                        else -> (fraction + "0".repeat(3 - fraction.length)).toLongOrNull() ?: 0L
                    }
                    add(LyricLine(minutes * 60_000L + seconds * 1_000L + millis, text))
                }
            }
        }.sortedBy { it.timeMs }
    }

    private fun parsePlain(plain: String?): List<LyricLine> {
        if (plain.isNullOrBlank()) return emptyList()
        return plain.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { LyricLine(-1L, it) }
            .toList()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

package com.vibeon.music.domain.model

/**
 * A single lyric line. [timeMs] is milliseconds from the start of the track,
 * or -1 for plain (untimed) lyrics.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class LyricsData(
    val lines: List<LyricLine>,
    val synced: Boolean,
    val instrumental: Boolean = false,
)

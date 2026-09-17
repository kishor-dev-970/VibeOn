package com.vibeon.music.util

const val GITHUB_RELEASES_URL = "https://github.com/kishor-dev-970/VibeOn/releases/latest"

/**
 * True when [latest] is a higher semantic version than [current], e.g. "v2.0.3" > "v2.0.2".
 * Both arguments may carry a leading "v" and any number of dot segments.
 */
fun isNewerVersion(latest: String, current: String): Boolean {
    fun parse(v: String): List<Int> =
        v.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val l = parse(latest)
    val c = parse(current)
    for (i in 0 until maxOf(l.size, c.size)) {
        val a = l.getOrElse(i) { 0 }
        val b = c.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
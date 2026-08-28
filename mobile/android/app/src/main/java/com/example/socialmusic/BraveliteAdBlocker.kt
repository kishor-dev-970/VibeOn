package com.example.socialmusic

import android.net.Uri

object BraveliteAdBlocker {

    private val BLOCKED_HOST_SUFFIXES = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "googletagservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "app-measurement.com",
        "analytics.google.com",
        "adservice.google.com",
        "ads.youtube.com",
        "2mdn.net",
        "admob.com",
        "adnxs.com",
        "adsrvr.org",
        "rubiconproject.com",
        "moatads.com",
        "adsafeprotected.com",
        "spotx.tv",
        "springserve.com",
        "smartadserver.com"
    )

    private val YT_BLOCKED_PATH_PREFIXES = listOf(
        "/api/stats/ads",
        "/pagead/",
        "/ptracking",
        "/get_midroll_info",
        "/ad_break",
        "/ad_frame",
        "/youtubei/v1/ads",
        "/api/youtube_ad_serving/"
    )

    fun isBlocked(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        if (BLOCKED_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }) return true
        if (host == "youtube.com" || host.endsWith(".youtube.com")) {
            val path = uri.path?.lowercase() ?: return false
            if (YT_BLOCKED_PATH_PREFIXES.any { path.startsWith(it) }) return true
        }
        return false
    }
}

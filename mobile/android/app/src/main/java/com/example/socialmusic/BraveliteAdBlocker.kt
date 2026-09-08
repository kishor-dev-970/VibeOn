package com.example.socialmusic

import android.net.Uri

object BraveliteAdBlocker {

    private val BLOCKED_HOSTS = hashSetOf(
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
        "smartadserver.com",
        "pagead.l.google.com",
        "optimizationguide-pa.google.com",
        "googleadapis.com",
        "gstaticad.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "openx.net",
        "taboola.com",
        "outbrain.com",
        "scorecardresearch.com",
        "quantserve.com",
        "casalemedia.com"
    )

    private val YT_BLOCKED_PATH_PREFIXES = listOf(
        "/api/stats/ads",
        "/pagead/",
        "/ptracking",
        "/get_midroll_info",
        "/ad_break",
        "/ad_frame",
        "/youtubei/v1/ads",
        "/api/youtube_ad_serving/",
        "/youtubei/v1/player_ads"
    )

    fun isBlocked(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false

        // O(K) where K is domain depth (at most 3-4 steps), doing O(1) HashSet lookups
        var domain: String? = host
        while (!domain.isNullOrEmpty()) {
            if (BLOCKED_HOSTS.contains(domain)) return true
            val dot = domain.indexOf('.')
            domain = if (dot >= 0) domain.substring(dot + 1) else null
        }

        if (host == "youtube.com" || host.endsWith(".youtube.com")) {
            val path = uri.path?.lowercase() ?: return false
            for (prefix in YT_BLOCKED_PATH_PREFIXES) {
                if (path.startsWith(prefix)) return true
            }
        }
        return false
    }
}

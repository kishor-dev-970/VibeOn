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
        "smartadserver.com",
        "ad.doubleclick.net",
        "googleads.g.doubleclick.net",
        "pagead.l.google.com",
        "cm.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "static.doubleclick.net",
        "pagead2.googlesyndication.com",
        "pagead.googlesyndication.com",
        "partner.googleadservices.com",
        "googleads.g.doubleclick.net",
        "optimizationguide-pa.google.com",
        "adservice.google.com",
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
        "casalemedia.com",
        "doubleclick.net"
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
        if (BLOCKED_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }) return true
        if (host == "youtube.com" || host.endsWith(".youtube.com")) {
            val path = uri.path?.lowercase() ?: return false
            if (YT_BLOCKED_PATH_PREFIXES.any { path.startsWith(it) }) return true
        }
        return false
    }
}

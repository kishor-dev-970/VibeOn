package com.vibeon.music.core

object Constants {
    const val INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    const val YOUTUBE_MUSIC_API = "https://music.youtube.com/youtubei/v1"
    const val YOUTUBE_API = "https://www.youtube.com/youtubei/v1"
    const val YOUTUBE_BASE = "https://www.youtube.com"
    const val MUSIC_BASE = "https://music.youtube.com"

    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    const val ANDROID_MUSIC_UA = "com.google.android.apps.youtube.music/7.11.1 (Linux; U; Android 15; en_US) gzip"
    const val ANDROID_UA = "com.google.android.youtube/21.26.364 (Linux; U; Android 11; en_US; gzip)"

    const val WEB_REMIX_CLIENT_VERSION = "1.20240101.01.00"
    const val ANDROID_MUSIC_CLIENT_VERSION = "7.11.1"
    const val ANDROID_CLIENT_VERSION = "21.26.364"
    const val ANDROID_SDK_VERSION = 30

    const val SEARCH_PARAM_SONGS = "EgWKAQIIAWoKEAoQCRADEAA%3D%3D"
    const val SEARCH_PARAM_VIDEOS = "EgWKAQIQAWoKEAoQCRADEAA%3D%3D"
    const val SEARCH_PARAM_ALBUMS = "EgWKAQIYAWoKEAoQCRADEAA%3D%3D"
    const val SEARCH_PARAM_ARTISTS = "EgWKAQIKAWoKEAoQCRADEAA%3D%3D"
    const val SEARCH_PARAM_PLAYLISTS = "EgWKAQILAWoKEAoQCRADEAA%3D%3D"

    fun thumbnailUrl(videoId: String, quality: String = "hqdefault") =
        "https://i.ytimg.com/vi/$videoId/$quality.jpg"
}
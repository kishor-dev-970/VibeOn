package com.vibeon.music.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Friends : Screen("friends")
    object NowPlaying : Screen("nowPlaying")
    object Playlist : Screen("playlist/{type}/{browseId}") {
        fun create(browseId: String) = "playlist/PLAYLIST/$browseId"
    }
    object Album : Screen("album/{type}/{browseId}") {
        fun create(browseId: String) = "album/ALBUM/$browseId"
    }
    object Artist : Screen("artist/{type}/{browseId}") {
        fun create(browseId: String) = "artist/ARTIST/$browseId"
    }
    object Friend : Screen("friend/{userId}") {
        fun create(userId: String) = "friend/$userId"
    }
    object Settings : Screen("settings")
}

enum class BottomTab(val route: String, val label: String) {
    Library("library", "Library"),
    Home("home", "Home"),
    Friends("friends", "Friends"),
}
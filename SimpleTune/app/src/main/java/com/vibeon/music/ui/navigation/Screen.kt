package com.vibeon.music.ui.navigation

/**
 * Routes. The three bottom tabs (Library / Home / Friends) are pages inside
 * [Screen.MainShell]; everything else is a pushed destination on top of it, so
 * back always returns to whichever tab the user was on. This mirrors the
 * LastWave-style shell without copying its code.
 */
sealed class Screen(val route: String) {
    object MainShell : Screen("main_shell")
    object Search : Screen("search")
    object Friends : Screen("friends")
    object NowPlaying : Screen("nowPlaying")
    object Discover : Screen("discover")
    object Genres : Screen("genres")
    object ListeningStats : Screen("listeningStats")
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

/** The three swipeable bottom tabs, in the original VibeOn order. */
enum class BottomTab(val label: String) {
    Library("Library"),
    Home("Home"),
    Friends("Friends"),
}

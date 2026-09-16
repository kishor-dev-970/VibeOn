package com.vibeon.music.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.auth.AuthViewModel
import com.vibeon.music.ui.auth.LoginScreen
import com.vibeon.music.ui.browse.BrowseDetailScreen
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.VibeOnTabBar
import com.vibeon.music.ui.friends.FriendDetailScreen
import com.vibeon.music.ui.friends.FriendsScreen
import com.vibeon.music.ui.home.HomeScreen
import com.vibeon.music.ui.library.LibraryScreen
import com.vibeon.music.ui.player.NowPlayingScreen
import com.vibeon.music.ui.search.SearchScreen
import com.vibeon.music.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun AppNavHost(
    playbackManager: PlaybackManager,
    openPlayerRequests: Flow<Unit> = kotlinx.coroutines.flow.emptyFlow(),
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionReady by authViewModel.sessionReady.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()

    when {
        !sessionReady -> GradientBackground { LoadingIndicator() }
        currentUser == null -> LoginScreen(
            busy = busy,
            error = error,
            onClearError = { authViewModel.clearError() },
            onSignIn = { first, last -> authViewModel.signIn(first, last) },
        )
        else -> MainNavHost(
            playbackManager = playbackManager,
            openPlayerRequests = openPlayerRequests,
        )
    }
}

@Composable
private fun MainNavHost(
    playbackManager: PlaybackManager,
    openPlayerRequests: Flow<Unit>,
) {
    val navController = rememberNavController()
    var topRoute by remember { mutableStateOf(Screen.Home.route) }

    LaunchedEffect(Unit) {
        openPlayerRequests.collect {
            navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
        }
    }

    fun goBackToTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openBrowse(type: ItemType, browseId: String) {
        val route = when (type) {
            ItemType.ALBUM -> Screen.Album.create(browseId)
            ItemType.PLAYLIST -> Screen.Playlist.create(browseId)
            else -> Screen.Artist.create(browseId)
        }
        navController.navigate(route)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
        ) {
            composable(Screen.Home.route) {
                topRoute = Screen.Home.route
                HomeScreen(
                    playbackManager = playbackManager,
                    onNavigateToBrowse = { type, browseId -> openBrowse(type, browseId) },
                    onOpenSearch = { navController.navigate(Screen.Search.route) },
                    onOpenNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                )
            }
            composable(Screen.Search.route) {
                topRoute = Screen.Search.route
                SearchScreen(
                    playbackManager = playbackManager,
                    onNavigateToBrowse = { type, browseId -> openBrowse(type, browseId) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Library.route) {
                topRoute = Screen.Library.route
                LibraryScreen(playbackManager = playbackManager)
            }
            composable(Screen.Friends.route) {
                topRoute = Screen.Friends.route
                FriendsScreen(
                    onOpenFriend = { userId -> navController.navigate(Screen.Friend.create(userId)) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                )
            }
            composable(Screen.Album.route) {
                topRoute = Screen.Album.route
                BrowseDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
            }
            composable(Screen.Playlist.route) {
                topRoute = Screen.Playlist.route
                BrowseDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
            }
            composable(Screen.Artist.route) {
                topRoute = Screen.Artist.route
                BrowseDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
            }
            composable(
                Screen.NowPlaying.route,
                enterTransition = { slideInVertically(tween(250)) { it } + fadeIn(tween(150)) },
                exitTransition = { slideOutVertically(tween(250)) { it } + fadeOut(tween(150)) },
            ) {
                topRoute = Screen.NowPlaying.route
                NowPlayingScreen(onDismiss = { navController.popBackStack() })
            }
            composable(
                Screen.Friend.route,
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) },
            ) {
                topRoute = Screen.Friend.route
                FriendDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                topRoute = Screen.Settings.route
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }

        if (topRoute in BottomTab.entries.map { it.route }) {
            val activeTab = if (topRoute == Screen.Home.route) {
                BottomTab.Home
            } else {
                BottomTab.entries.firstOrNull { it.route == topRoute } ?: BottomTab.Home
            }
            VibeOnTabBar(
                currentTab = activeTab,
                onTabSelected = { tab ->
                    goBackToTop(tab.route)
                },
                playbackManager = playbackManager,
                onOpenNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
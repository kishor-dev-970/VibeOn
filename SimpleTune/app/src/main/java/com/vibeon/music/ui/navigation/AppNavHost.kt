package com.vibeon.music.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.vibeon.music.ui.explore.ExploreScreen
import com.vibeon.music.ui.friends.FriendDetailScreen
import com.vibeon.music.ui.friends.FriendsScreen
import com.vibeon.music.ui.home.HomeScreen
import com.vibeon.music.ui.library.LibraryScreen
import com.vibeon.music.ui.player.NowPlayingScreen
import com.vibeon.music.ui.search.SearchScreen
import com.vibeon.music.ui.settings.SettingsScreen
import com.vibeon.music.ui.stats.ListeningStatsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

    LaunchedEffect(Unit) {
        openPlayerRequests.collect {
            navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
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

    NavHost(
        navController = navController,
        startDestination = Screen.MainShell.route,
    ) {
        composable(Screen.MainShell.route) {
            MainShellScreen(
                playbackManager = playbackManager,
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenFriend = { userId -> navController.navigate(Screen.Friend.create(userId)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenDiscover = { navController.navigate(Screen.Discover.route) },
                onOpenListeningStats = { navController.navigate(Screen.ListeningStats.route) },
                onNavigateToBrowse = { type, id -> openBrowse(type, id) },
                onOpenNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                playbackManager = playbackManager,
                onNavigateToBrowse = { type, id -> openBrowse(type, id) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Album.route) {
            BrowseDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
        }
        composable(Screen.Playlist.route) {
            BrowseDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
        }
        composable(Screen.Artist.route) {
            BrowseDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
        }
        composable(
            Screen.NowPlaying.route,
            enterTransition = { slideInVertically(tween(250)) { it } + fadeIn(tween(150)) },
            exitTransition = { slideOutVertically(tween(250)) { it } + fadeOut(tween(150)) },
        ) {
            NowPlayingScreen(onDismiss = { navController.popBackStack() })
        }
        composable(
            Screen.Friend.route,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            FriendDetailScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Discover.route) {
            ExploreScreen(
                mode = "discover",
                onBack = { navController.popBackStack() },
                onNavigateToBrowse = { type, id -> openBrowse(type, id) },
            )
        }
        composable(Screen.Genres.route) {
            ExploreScreen(
                mode = "genres",
                onBack = { navController.popBackStack() },
                onNavigateToBrowse = { type, id -> openBrowse(type, id) },
            )
        }
        composable(Screen.ListeningStats.route) {
            ListeningStatsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainShellScreen(
    playbackManager: PlaybackManager,
    onOpenSearch: () -> Unit,
    onOpenFriend: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenListeningStats: () -> Unit,
    onNavigateToBrowse: (ItemType, String) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val tabs = BottomTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 118.dp),
        ) { page ->
            when (tabs[page]) {
                BottomTab.Library -> LibraryScreen(playbackManager = playbackManager)
                BottomTab.Home -> HomeScreen(
                    playbackManager = playbackManager,
                    onNavigateToBrowse = onNavigateToBrowse,
                    onOpenSearch = onOpenSearch,
                    onOpenNowPlaying = onOpenNowPlaying,
                    onOpenDiscover = onOpenDiscover,
                    onOpenListeningStats = onOpenListeningStats,
                    onOpenSettings = onOpenSettings,
                )
                BottomTab.Friends -> FriendsScreen(
                    onOpenFriend = onOpenFriend,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
        VibeOnTabBar(
            currentTab = tabs[pagerState.currentPage],
            onTabSelected = { tab ->
                scope.launch { pagerState.animateScrollToPage(tabs.indexOf(tab)) }
            },
            playbackManager = playbackManager,
            onOpenNowPlaying = onOpenNowPlaying,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

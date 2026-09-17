package com.vibeon.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.vibeon.music.data.innertube.PoTokenMinter
import com.vibeon.music.data.network.SocialApiClient
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.UpdatePromptHost
import com.vibeon.music.ui.navigation.AppNavHost
import com.vibeon.music.ui.theme.VibeOnTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), ImageLoaderFactory {

    companion object {
        const val EXTRA_OPEN_PLAYER = "com.vibeon.music.EXTRA_OPEN_PLAYER"
    }

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var potMinter: PoTokenMinter

    @Inject
    lateinit var socialApiClient: SocialApiClient

    private val openPlayerRequests = Channel<Unit>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLaunchIntent(intent)
        // Pre-mint the PO token in the background so playback doesn't stall on first tap.
        lifecycleScope.launch { potMinter.get() }
        setContent {
            VibeOnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost(
                        playbackManager = playbackManager,
                        openPlayerRequests = openPlayerRequests.receiveAsFlow(),
                    )
                    UpdatePromptHost(api = socialApiClient)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) {
            intent.removeExtra(EXTRA_OPEN_PLAYER)
            openPlayerRequests.trySend(Unit)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache(MemoryCache.Builder(this).maxSizePercent(0.20).build())
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(200L * 1024 * 1024)
                .build()
        )
        .respectCacheHeaders(false)
        .build()
}
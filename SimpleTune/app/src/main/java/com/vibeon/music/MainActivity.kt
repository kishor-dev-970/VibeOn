package com.vibeon.music

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
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.navigation.AppNavHost
import com.vibeon.music.ui.theme.VibeOnTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), ImageLoaderFactory {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var potMinter: PoTokenMinter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Pre-mint the PO token in the background so playback doesn't stall on first tap.
        lifecycleScope.launch { potMinter.get() }
        setContent {
            VibeOnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost(playbackManager = playbackManager)
                }
            }
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
package com.vibeon.music.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.components.SongRow
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnBorder
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnSecondary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun BrowseDetailScreen(
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    viewModel: BrowseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = VibeOnText)
            }
            when {
                state.loading -> LoadingIndicator()
                state.songs.isEmpty() -> EmptyState("No tracks here yet")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    item {
                        Header(state)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlayAllButton(
                                label = "Play",
                                onClick = { playbackManager.playQueue(state.songs, 0) },
                                colors = listOf(VibeOnPrimary, Color(0xFFE0E0E0)),
                            )
                            PlayAllButton(
                                label = "Shuffle",
                                onClick = { playbackManager.playQueue(state.songs.shuffled(), 0) },
                                colors = listOf(VibeOnBgCardLight, VibeOnBorder),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    itemsIndexed(state.songs, key = { _, s -> s.videoId }) { index, song ->
                        SongRow(
                            song = song,
                            index = index + 1,
                            onClick = { playbackManager.playQueue(state.songs, index) },
                        )
                    }
                    if (state.continuation != null) {
                        item {
                            androidx.compose.material3.TextButton(onClick = { viewModel.loadMore() }) {
                                Text("Load more", color = VibeOnPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: BrowseDetailViewModel.UiState) {
    val title = state.album?.title ?: state.playlist?.title ?: state.artist?.name ?: ""
    val subtitle = state.playlist?.author
        ?: state.album?.artists?.joinToString(", ") { it.name }
        ?: "${state.songs.size} songs"
    val thumbnail = state.album?.thumbnailUrl ?: state.playlist?.thumbnailUrl ?: state.artist?.thumbnailUrl

    Row(verticalAlignment = Alignment.CenterVertically) {
        SongArtwork(
            url = thumbnail,
            modifier = Modifier.size(130.dp),
            cornerShape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = VibeOnText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            if (state.playlist?.songCount != null) {
                Text(
                    text = "${state.playlist.songCount} songs",
                    color = VibeOnTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PlayAllButton(label: String, onClick: () -> Unit, colors: List<Color>) {
    Row(
        modifier = Modifier
            .background(Brush.linearGradient(colors), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (label == "Shuffle") Icons.Rounded.Shuffle else Icons.Rounded.PlayArrow,
            contentDescription = label,
            tint = if (colors.first() == VibeOnPrimary) Color(0xFF000000) else Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (colors.first() == VibeOnPrimary) Color(0xFF000000) else Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
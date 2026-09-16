package com.vibeon.music.ui.player

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val song by viewModel.song.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val position by viewModel.position.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val queue by viewModel.queueSongs.collectAsState()
    val queueIndex by viewModel.queueIndex.collectAsState()
    val error by viewModel.error.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Down", tint = VibeOnTextMuted)
                }
                Text(
                    text = "Now Playing",
                    color = VibeOnTextMuted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(48.dp))
            }

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = VibeOnPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                SongArtwork(
                    url = song?.thumbnailUrl,
                    modifier = Modifier.size(160.dp),
                    cornerShape = RoundedCornerShape(20.dp),
                    backgroundColor = Color.Transparent,
                )
                if (isBuffering) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = VibeOnPrimary,
                    )
                }
            }

            Text(
                text = song?.title ?: "Nothing playing",
                color = VibeOnText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song?.artists?.joinToString(", ") { it.name }?.ifBlank { "Unknown artist" } ?: "—",
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            Slider(
                value = position.toFloat().coerceAtMost(durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { viewModel.onSeek(it.toLong()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = VibeOnPrimary,
                    activeTrackColor = VibeOnPrimary,
                    inactiveTrackColor = VibeOnTextMuted.copy(alpha = 0.3f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(position), color = VibeOnTextMuted, style = MaterialTheme.typography.labelSmall)
                Text(formatTime(durationMs), color = VibeOnTextMuted, style = MaterialTheme.typography.labelSmall)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.onToggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) VibeOnPrimary else VibeOnTextMuted,
                    )
                }
                IconButton(onClick = { viewModel.onPrevious() }) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = VibeOnText,
                        modifier = Modifier.size(34.dp),
                    )
                }
                PlayPauseButton(isPlaying = isPlaying, onClick = { viewModel.onTogglePlay() })
                IconButton(onClick = { viewModel.onNext() }) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = VibeOnText,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = VibeOnTextMuted,
                    )
                }
            }

            if (queue.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Up Next",
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = VibeOnTextMuted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(queue, key = { _, s -> s.videoId }) { index, queued ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index == queueIndex) VibeOnBgCardLight else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                modifier = Modifier.width(22.dp),
                                color = if (index == queueIndex) VibeOnPrimary else VibeOnTextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                            SliverArtwork(url = queued.thumbnailUrl)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = queued.title,
                                modifier = Modifier.weight(1f),
                                color = if (index == queueIndex) VibeOnPrimary else VibeOnText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(VibeOnPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(if (isPlaying) 24.dp else 28.dp),
        )
    }
}

@Composable
private fun SliverArtwork(url: String?) {
    SongArtwork(
        url = url,
        modifier = Modifier.size(36.dp),
        cornerShape = RoundedCornerShape(6.dp),
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
package com.vibeon.music.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.domain.model.FriendStats
import com.vibeon.music.domain.model.NowPlaying
import com.vibeon.music.domain.model.displayName
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.domain.model.Song
import com.vibeon.music.ui.components.Avatar
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.components.StatCard
import com.vibeon.music.ui.theme.VibeOnAccent
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnSecondary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun FriendDetailScreen(
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    viewModel: FriendDetailViewModel = hiltViewModel(),
) {
    val friend by viewModel.friend.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val loading by viewModel.loading.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = VibeOnText)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    friend?.let {
                        Text(
                            text = it.user.displayName(),
                            color = VibeOnText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = it.user.code.ifBlank { "Friend" }.let { code -> "Code: $code" },
                            color = VibeOnTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.width(48.dp))
            }

            when {
                loading && stats == null -> LoadingIndicator()
                stats == null -> EmptyState("Stats unavailable")
                else -> FriendStatsContent(stats!!, viewModel, playbackManager)
            }
        }
    }
}

@Composable
private fun FriendStatsContent(
    stats: FriendStats,
    viewModel: FriendDetailViewModel,
    playbackManager: PlaybackManager,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stats.nowPlaying?.let { nowPlaying ->
            item {
                NowPlayingCard(nowPlaying = nowPlaying, onPlay = {
                    playFriendSong(nowPlaying, playbackManager)
                })
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    value = stats.totalSongs.toString(),
                    label = "Songs played",
                    color = VibeOnPrimary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = stats.totalPlays.toString(),
                    label = "Plays",
                    color = VibeOnSecondary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = viewModel.formatTime(stats.estimatedMinutes),
                    label = "Listened",
                    color = VibeOnAccent,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = "RECENT PLAYS",
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (stats.recentPlays.isEmpty()) {
            item { EmptyState("No plays recorded yet") }
        } else {
            items(stats.recentPlays, key = { "${it.playedAt}-${it.songTitle}" }) { play ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playFriendSong(
                                NowPlaying(
                                    isPlaying = true,
                                    songTitle = play.songTitle,
                                    artistName = play.artistName,
                                    artworkUrl = play.artworkUrl,
                                ),
                                playbackManager,
                            )
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SongArtwork(url = play.artworkUrl, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            play.songTitle,
                            color = VibeOnText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        if (!play.artistName.isNullOrBlank()) {
                            Text(
                                play.artistName ?: "",
                                color = VibeOnTextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(nowPlaying: NowPlaying, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibeOnBgCard, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(
            text = if (nowPlaying.isPlaying) "LISTENING NOW" else "LAST PLAYED",
            color = VibeOnTextMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SongArtwork(url = nowPlaying.artworkUrl, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = nowPlaying.songTitle ?: "Unknown",
                    color = VibeOnText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (!nowPlaying.artistName.isNullOrBlank()) {
                    Text(
                        text = nowPlaying.artistName ?: "",
                        color = VibeOnTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Play audio",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                color = VibeOnAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun playFriendSong(nowPlaying: NowPlaying, playbackManager: PlaybackManager) {
    val videoId = nowPlaying.videoId ?: return
    val song = Song(
        videoId = videoId,
        title = nowPlaying.songTitle ?: "Unknown",
        artists = listOf(com.vibeon.music.domain.model.Artist(name = nowPlaying.artistName ?: "")),
        thumbnailUrl = nowPlaying.artworkUrl,
    )
    runCatching { playbackManager.playSong(song) }
}
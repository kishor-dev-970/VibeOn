package com.vibeon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun MiniPlayer(
    playbackManager: PlaybackManager,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song by playbackManager.currentSong.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val isBuffering by playbackManager.isBuffering.collectAsState()

    if (song == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VibeOnBgCardLight, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenNowPlaying)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(
            url = song?.thumbnailUrl,
            modifier = Modifier.size(42.dp),
            cornerShape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.title ?: "",
                color = VibeOnText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song?.artists?.joinToString(", ") { it.name } ?: "",
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = VibeOnPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            MiniPlayButton(isPlaying = isPlaying, onClick = { playbackManager.togglePlayPause() })
        }
    }
}
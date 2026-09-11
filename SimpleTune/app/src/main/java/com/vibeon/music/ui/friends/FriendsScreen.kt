package com.vibeon.music.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vibeon.music.domain.model.FriendActivity
import com.vibeon.music.domain.model.NowPlaying
import com.vibeon.music.domain.model.displayName
import com.vibeon.music.ui.components.Avatar
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnBorder
import com.vibeon.music.ui.theme.VibeOnPrimaryLight
import com.vibeon.music.ui.theme.VibeOnSuccess
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted
import com.vibeon.music.ui.theme.VibeOnTextSubtle

@Composable
fun FriendsScreen(
    onOpenFriend: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val friends by viewModel.friends.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPolling()
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.People,
                    contentDescription = null,
                    tint = VibeOnPrimaryLight,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "VibeOn",
                    color = VibeOnPrimaryLight,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = VibeOnTextMuted,
                    )
                }
            }
            Text(
                text = "Friends",
                modifier = Modifier.padding(horizontal = 20.dp),
                color = VibeOnText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            val listeningCount = friends.count { it.nowPlaying?.isPlaying == true }
            Text(
                text = when {
                    friends.isEmpty() -> "See what your friends are listening to"
                    listeningCount == 1 -> "1 person is listening now"
                    listeningCount > 1 -> "$listeningCount people are listening now"
                    else -> "Nobody is listening right now"
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))

            when {
                loading && friends.isEmpty() -> LoadingIndicator()
                friends.isEmpty() -> EmptyState("No friends yet. Everyone on VibeOn is a friend.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(friends, key = { it.user.id }) { friend ->
                        FriendCard(
                            friend = friend,
                            onClick = { onOpenFriend(friend.user.id) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FriendCard(friend: FriendActivity, onClick: () -> Unit) {
    val isOnline = isOnline(friend)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibeOnBgCard, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                name = friend.user.displayName(),
                url = friend.user.avatarUrl,
                showOnlineDot = isOnline,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = friend.user.displayName(),
                    color = VibeOnText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (friend.user.code.isNotBlank()) {
                    Text(
                        text = "Code: ${friend.user.code}",
                        color = VibeOnTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        NowPlayingRow(nowPlaying = friend.nowPlaying)
    }
}

@Composable
private fun NowPlayingRow(nowPlaying: NowPlaying?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibeOnBgCardLight, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(
            url = nowPlaying?.artworkUrl,
            modifier = Modifier.size(44.dp),
            cornerShape = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.width(12.dp))
        if (nowPlaying != null && !nowPlaying.songTitle.isNullOrBlank()) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = nowPlaying.songTitle ?: "",
                    color = VibeOnText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
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
            if (nowPlaying.isPlaying) {
                Text(
                    text = "● Live",
                    color = VibeOnSuccess,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = "❙❙ Paused",
                    color = VibeOnTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Text(
                text = "Not listening",
                color = VibeOnTextSubtle,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

fun isOnline(friend: FriendActivity): Boolean {
    if (friend.nowPlaying?.isPlaying == true) return true
    val lastActive = friend.user.lastActive
    if (lastActive != null) {
        val diff = System.currentTimeMillis() - lastActive
        return diff < 2 * 60 * 1000
    }
    if (friend.nowPlaying != null) {
        val updatedAt = friend.nowPlaying.updatedAt
        if (updatedAt != null) {
            val diff = System.currentTimeMillis() - updatedAt
            return diff < 2 * 60 * 1000
        }
    }
    return false
}
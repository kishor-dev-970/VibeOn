package com.vibeon.music.ui.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnPrimaryLight
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LastWave-inspired Library: expressive header, search + generator, tinted hero
 * cards for the device/liked collections, then a playlists list with counts and
 * dates. Every control is backed by VibeOn's own local library data.
 */
@Composable
fun LibraryScreen(
    playbackManager: PlaybackManager,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val counts by viewModel.playlistCounts.collectAsState()
    val selectedId by viewModel.selectedPlaylistId.collectAsState()
    val selectedSongs by viewModel.selectedPlaylistSongs.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }

    val filteredPlaylists = playlists
        .filter { it.name.contains(query, ignoreCase = true) }
        .let { list -> if (sortAscending) list.sortedBy { it.name } else list.sortedByDescending { it.name } }
    val totalTracks = playlists.sumOf { counts[it.id] ?: 0 }

    GradientBackground {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = "Library",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    color = VibeOnText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(VibeOnBgCardLight, RoundedCornerShape(16.dp)),
                    ) {
                        androidx.compose.material3.TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxSize(),
                            singleLine = true,
                            placeholder = {
                                Text("Search playlists...", color = VibeOnTextMuted, style = MaterialTheme.typography.bodyMedium)
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = VibeOnTextMuted, modifier = Modifier.size(18.dp))
                            },
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = VibeOnPrimaryLight,
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = VibeOnText),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(VibeOnBgCardLight)
                            .clickable {
                                val mix = (favorites + recent).distinctBy { it.videoId }
                                if (mix.isEmpty()) {
                                    notice = "Play or like a few songs first"
                                } else {
                                    playbackManager.playQueue(mix.shuffled(), 0)
                                    notice = "Playing a mix of ${mix.size} songs"
                                }
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VibeOnPrimaryLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generator", color = VibeOnText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (notice != null) {
                    Text(
                        text = notice ?: "",
                        color = VibeOnPrimaryLight,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))
                LibraryKicker("SONGS FROM THIS DEVICE")
                LibraryHeroCard(
                    title = "Local songs",
                    subtitle = "${recent.size} songs played on this device",
                    icon = Icons.Rounded.MusicNote,
                    gradient = listOf(Color(0xFF2A3A4A), Color(0xFF171C24)),
                    onClick = {
                        if (recent.isEmpty()) notice = "Nothing played yet" else playbackManager.playQueue(recent, 0)
                    },
                )
                Spacer(Modifier.height(8.dp))
                LibraryKicker("LIKED SONGS")
                LibraryHeroCard(
                    title = "Liked Songs",
                    subtitle = "${favorites.size} songs you love",
                    icon = Icons.Rounded.Favorite,
                    gradient = listOf(Color(0xFF4A2C6D), Color(0xFF211A2E)),
                    onClick = {
                        if (favorites.isEmpty()) notice = "Like a few songs first" else playbackManager.playQueue(favorites.shuffled(), 0)
                    },
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PLAYLISTS",
                            color = VibeOnTextMuted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "${playlists.size} Playlists • $totalTracks Tracks",
                            color = VibeOnTextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = VibeOnPrimaryLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New", color = VibeOnPrimaryLight)
                    }
                    TextButton(onClick = { sortAscending = !sortAscending }) {
                        Text("Sort", color = VibeOnPrimaryLight)
                    }
                }
            }

            if (filteredPlaylists.isEmpty()) {
                item { EmptyState(if (query.isBlank()) "No playlists yet" else "No playlists match \"$query\"") }
            } else {
                items(filteredPlaylists, key = { it.id }) { playlist ->
                    val date = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(Date(playlist.createdAt))
                    val trackCount = counts[playlist.id] ?: 0
                    val expanded = selectedId == playlist.id
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(VibeOnBgCard)
                                .clickable {
                                    viewModel.onPlaylistSelected(if (expanded) null else playlist.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(VibeOnBgCardLight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = VibeOnPrimaryLight)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, color = VibeOnText, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("$date • $trackCount tracks", color = VibeOnTextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = VibeOnTextMuted)
                            }
                        }
                        if (expanded) {
                            if (selectedSongs.isEmpty()) {
                                Text(
                                    text = "This playlist is empty",
                                    color = VibeOnTextMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
                                )
                            } else {
                                selectedSongs.forEach { song ->
                                    com.vibeon.music.ui.components.SongRow(
                                        song = song,
                                        onClick = { playbackManager.playQueue(selectedSongs, selectedSongs.indexOf(song)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun LibraryKicker(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        color = VibeOnTextMuted,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
private fun LibraryHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) {
                Text("Create", color = VibeOnPrimaryLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VibeOnTextMuted)
            }
        },
    )
}

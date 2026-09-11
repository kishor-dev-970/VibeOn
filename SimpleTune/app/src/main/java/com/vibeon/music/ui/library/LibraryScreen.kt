package com.vibeon.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.SectionTitle
import com.vibeon.music.ui.components.SongRow
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun LibraryScreen(
    playbackManager: PlaybackManager,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Library",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                color = VibeOnText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showCreateDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = VibeOnPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("New playlist", color = VibeOnPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                item { SectionTitle("Playlists") }
                if (playlists.isEmpty()) {
                    item { EmptyState("No playlists yet") }
                } else {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = VibeOnPrimary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, color = VibeOnText, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = VibeOnTextMuted)
                            }
                        }
                    }
                }

                item { SectionTitle("Favorites") }
                if (favorites.isEmpty()) {
                    item { EmptyState("Songs you like will show here") }
                } else {
                    items(favorites, key = { "f-${it.videoId}" }) { song ->
                        SongRow(
                            song = song,
                            onClick = { playbackManager.playSong(song) },
                            trailing = {
                                IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                                    Icon(Icons.Rounded.Favorite, contentDescription = "Favorite", tint = VibeOnPrimary)
                                }
                            },
                        )
                    }
                }

                item { SectionTitle("Recently played") }
                if (recent.isEmpty()) {
                    item { EmptyState("Nothing played yet") }
                } else {
                    items(recent, key = { "r-${it.videoId}" }) { song ->
                        SongRow(
                            song = song,
                            onClick = { playbackManager.playSong(song) },
                            trailing = {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = VibeOnTextMuted)
                            },
                        )
                    }
                }
            }
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
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
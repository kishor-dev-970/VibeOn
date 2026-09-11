package com.vibeon.music.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.data.music.SearchFilter
import com.vibeon.music.domain.model.Album
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.domain.model.Playlist
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SectionTitle
import com.vibeon.music.ui.components.SongRow
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.theme.VibeOnBorder
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnSearchBg
import com.vibeon.music.ui.theme.VibeOnText

@Composable
fun SearchScreen(
    playbackManager: PlaybackManager,
    onNavigateToBrowse: (ItemType, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val results by viewModel.results.collectAsState()
    val loading by viewModel.loading.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Search",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                color = VibeOnText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Songs, albums, artists...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = VibeOnPrimary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = VibeOnPrimary)
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VibeOnSearchBg,
                    unfocusedContainerColor = VibeOnSearchBg,
                    focusedBorderColor = VibeOnPrimary,
                    unfocusedBorderColor = VibeOnBorder,
                    focusedTextColor = VibeOnText,
                    unfocusedTextColor = VibeOnText,
                ),
            )

            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchFilter.entries) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { viewModel.onFilterChange(f) },
                        label = {
                            Text(
                                f.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (filter == f) VibeOnPrimary else VibeOnText,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            when {
                query.isBlank() -> EmptyState("Search for music across YouTube Music")
                loading -> LoadingIndicator()
                results.isEmpty -> EmptyState("No results")
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when (filter) {
                        SearchFilter.SONGS -> items(results.songs, key = { "s-${it.videoId}" }) { song ->
                            SongRow(song = song, onClick = { playbackManager.playSong(song) })
                        }
                        SearchFilter.VIDEOS -> items(results.videos, key = { "v-${it.videoId}" }) { video ->
                            SongRow(song = video, onClick = { playbackManager.playSong(video) })
                        }
                        SearchFilter.ALBUMS -> items(results.albums, key = { "a-${it.id}" }) { album ->
                            AlbumRow(album = album, onClick = {
                                if (album.id.isNotBlank()) onNavigateToBrowse(ItemType.ALBUM, album.id)
                            })
                        }
                        SearchFilter.ARTISTS -> items(results.artists, key = { "ar-${it.id}" }) { artist ->
                            ArtistRow(artist = artist, onClick = {
                                if (artist.id.isNotBlank()) onNavigateToBrowse(ItemType.ARTIST, artist.id)
                            })
                        }
                        SearchFilter.PLAYLISTS -> items(results.playlists, key = { "p-${it.id}" }) { playlist ->
                            PlaylistRow(playlist = playlist, onClick = {
                                if (playlist.id.isNotBlank()) onNavigateToBrowse(ItemType.PLAYLIST, playlist.id)
                            })
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(url = album.thumbnailUrl, modifier = Modifier.width(64.dp).height(64.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(album.title, color = VibeOnText, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                album.artists.joinToString(", ") { it.name },
                color = VibeOnText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(
            url = artist.thumbnailUrl,
            modifier = Modifier.width(56.dp).height(56.dp),
            cornerShape = RoundedCornerShape(999.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(artist.name, color = VibeOnText, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(url = playlist.thumbnailUrl, modifier = Modifier.width(64.dp).height(64.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(playlist.title, color = VibeOnText, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (playlist.author.isNotBlank()) {
                Text(
                    playlist.author,
                    color = VibeOnText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}
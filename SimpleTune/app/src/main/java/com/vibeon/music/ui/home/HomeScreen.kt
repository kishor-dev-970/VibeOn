package com.vibeon.music.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.HomeChip
import com.vibeon.music.domain.model.HomeSection
import com.vibeon.music.domain.model.HomeSectionLayout
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.BrowseCard
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SectionTitle
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.components.SongRow
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnPrimaryLight
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playbackManager: PlaybackManager,
    onNavigateToBrowse: (ItemType, String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsState()
    val chips by viewModel.chips.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val continuation by viewModel.continuation.collectAsState()

    GradientBackground {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = VibeOnPrimaryLight,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "VibeOn",
                            color = VibeOnPrimaryLight,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text(
                        text = "Home",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = VibeOnText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp)
                            .background(VibeOnBgCardLight, RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenSearch)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = VibeOnTextMuted,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Search songs, artists",
                            color = VibeOnTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (chips.isNotEmpty()) {
                    item {
                        HomeChipsRow(
                            chips = chips,
                            selected = selectedChip,
                            onSelect = { viewModel.selectChip(it) },
                        )
                    }
                }

                when {
                    loading && sections.isEmpty() -> item { LoadingIndicator() }
                    error != null && sections.isEmpty() -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmptyState(text = error ?: "")
                            Text(
                                text = "Tap to retry",
                                color = VibeOnPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Button(onClick = { viewModel.loadHome() }) {
                                Text("Retry")
                            }
                        }
                    }
                    else -> sections.forEach { section ->
                        item(key = "title_${section.title}") {
                            SectionTitle(section.title)
                        }
                        item(key = "row_${section.title}") {
                            HomeSectionRow(
                                section = section,
                                playbackManager = playbackManager,
                                onNavigateToBrowse = onNavigateToBrowse,
                            )
                        }
                    }
                }

                if (continuation != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = { viewModel.loadMore() }) {
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
private fun HomeSectionRow(
    section: HomeSection,
    playbackManager: PlaybackManager,
    onNavigateToBrowse: (ItemType, String) -> Unit,
) {
    when (section.layout) {
        HomeSectionLayout.GRID -> HomeGridSection(section, onNavigateToBrowse)
        HomeSectionLayout.LIST -> HomeListSection(section, playbackManager, onNavigateToBrowse)
        HomeSectionLayout.CAROUSEL -> {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(section.items.take(10), key = { "${it.type}-${it.title}-${it.browseId}" }) { item ->
                    if (item.type == ItemType.SONG || item.type == ItemType.VIDEO) {
                        val song = Song(
                            videoId = item.videoId ?: "",
                            title = item.title,
                            artists = listOf(),
                            thumbnailUrl = item.thumbnailUrl,
                        )
                        androidx.compose.foundation.layout.Box(modifier = Modifier.width(300.dp)) {
                            SongRow(song = song, onClick = { if (item.videoId != null) playbackManager.playSong(song) })
                        }
                    } else {
                        item.browseId?.let {
                            BrowseCard(
                                item = item,
                                onClick = { onNavigateToBrowse(item.type, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGridSection(
    section: HomeSection,
    onNavigateToBrowse: (ItemType, String) -> Unit,
) {
    val rows = section.items.take(14).chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    item.browseId?.let {
                        Box(Modifier.weight(1f)) {
                            BrowseCard(
                                item = item,
                                onClick = { onNavigateToBrowse(item.type, it) },
                                modifier = Modifier.fillMaxWidth(),
                                expand = true,
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeListSection(
    section: HomeSection,
    playbackManager: PlaybackManager,
    onNavigateToBrowse: (ItemType, String) -> Unit,
) {
    Column {
        section.items.forEach { item ->
            when {
                item.type == ItemType.SONG || item.type == ItemType.VIDEO -> {
                    val song = Song(
                        videoId = item.videoId ?: "",
                        title = item.title,
                        artists = listOf(),
                        thumbnailUrl = item.thumbnailUrl,
                    )
                    SongRow(song = song, onClick = { if (item.videoId != null) playbackManager.playSong(song) })
                }
                item.browseId != null -> BrowseListRow(
                    item = item,
                    onClick = { onNavigateToBrowse(item.type, item.browseId!!) },
                )
            }
        }
    }
}

@Composable
private fun BrowseListRow(item: BrowseItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(url = item.thumbnailUrl, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = VibeOnText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    color = VibeOnTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HomeChipsRow(
    chips: List<HomeChip>,
    selected: String?,
    onSelect: (HomeChip) -> Unit,
) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips) { chip ->
            val isSelected = chip.label == selected
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) VibeOnPrimary else VibeOnBgCardLight,
                        RoundedCornerShape(50),
                    )
                    .clickable(onClick = { onSelect(chip) })
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else VibeOnTextMuted,
                )
            }
        }
    }
}
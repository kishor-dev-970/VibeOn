package com.vibeon.music.ui.home

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.HomeChip
import com.vibeon.music.domain.model.HomeSectionKind
import com.vibeon.music.domain.model.HomeSection
import com.vibeon.music.domain.model.HomeSectionLayout
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.components.BrowseCard
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SongArtwork
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
    onOpenDiscover: () -> Unit,
    onOpenListeningStats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsState()
    val chips by viewModel.chips.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val continuation by viewModel.continuation.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val listeningMinutes by viewModel.listeningMinutes.collectAsState()

    val now = LocalDateTime.now()
    val greeting = when {
        now.hour < 12 -> "Good morning"
        now.hour < 17 -> "Good afternoon"
        now.hour < 21 -> "Good evening"
        else -> "Good night"
    }
    val dateLabel = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
        .uppercase(Locale.ENGLISH)

    GradientBackground {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // Brand pill + quick actions, matching LastWave's compact top bar.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
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
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = VibeOnTextMuted)
                        }
                        IconButton(onClick = onOpenListeningStats) {
                            Icon(Icons.Rounded.BarChart, contentDescription = "Listening stats", tint = VibeOnTextMuted)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = VibeOnTextMuted)
                        }
                    }
                    Text(
                        text = greeting,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = VibeOnText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = dateLabel,
                            color = VibeOnTextMuted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        ListeningTimePill(minutes = listeningMinutes)
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionKicker(text = "MADE FOR YOU")
                    InfiniteRadioCard(onPlay = { viewModel.playInfiniteRadio() })
                    Spacer(Modifier.height(6.dp))
                    SectionKicker(text = "QUICK ACCESS")
                    QuickAccessCard(onOpen = onOpenDiscover)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Tap a vibe to start instant radio",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = VibeOnTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
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
                            EmptyState(text = error ?: "", onRetry = { viewModel.loadHome() })
                        }
                    }
                    else -> sections.forEach { section ->
                        when (section.kind) {
                            HomeSectionKind.TASTEBUILDER -> item(key = "taste_${section.title}") {
                                TasteBuilderCard(
                                    title = section.title,
                                    message = section.strapline,
                                    onGo = onOpenSearch,
                                )
                            }
                            else -> {
                                item(key = "title_${section.title}") {
                                    SectionHeader(
                                        section = section,
                                        onPlayAll = { viewModel.playSection(section) },
                                        onMore = {
                                            section.moreBrowseId?.let {
                                                onNavigateToBrowse(ItemType.PLAYLIST, it)
                                            }
                                        },
                                    )
                                }
                                item(key = "row_${section.title}") {
                                    HomeSectionRow(
                                        section = section,
                                        favorites = favorites,
                                        onPlay = viewModel::playSong,
                                        onToggleLike = viewModel::toggleLike,
                                        onAddToQueue = viewModel::addToQueue,
                                        onNavigateToBrowse = onNavigateToBrowse,
                                    )
                                }
                            }
                        }
                    }
                }

                if (continuation != null) {
                    // Auto-load as the user nears the bottom. LazyColumn only composes this
                    // sentinel when it is about to become visible, and the token key makes
                    // each new page trigger exactly one load.
                    item(key = "home_load_more") {
                        LaunchedEffect(continuation) { viewModel.loadMore() }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (loadingMore) {
                                LoadingIndicator(size = 24)
                            } else {
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
}

@Composable
private fun SectionHeader(
    section: HomeSection,
    onPlayAll: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VibeOnText,
            )
            if (section.strapline.isNotBlank()) {
                Text(
                    text = section.strapline,
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeOnTextMuted,
                )
            }
        }
        if (section.items.any { it.videoId != null }) {
            TextButton(onClick = onPlayAll) {
                Text(text = "Play all", color = VibeOnPrimary)
            }
        }
        if (section.moreBrowseId != null) {
            TextButton(onClick = onMore) {
                Text(text = section.moreLabel, color = VibeOnPrimary)
            }
        }
    }
}

@Composable
private fun TasteBuilderCard(
    title: String,
    message: String,
    onGo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(VibeOnBgCardLight, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibeOnText)
        Spacer(Modifier.height(6.dp))
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = VibeOnTextMuted)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onGo) {
            Text("Let's go")
        }
    }
}

@Composable
private fun HomeSectionRow(
    section: HomeSection,
    favorites: Set<String>,
    onPlay: (BrowseItem) -> Unit,
    onToggleLike: (BrowseItem) -> Unit,
    onAddToQueue: (BrowseItem) -> Unit,
    onNavigateToBrowse: (ItemType, String) -> Unit,
) {
    when (section.layout) {
        HomeSectionLayout.GRID -> HomeGridSection(section, onNavigateToBrowse)
        HomeSectionLayout.LIST -> HomeListSection(section, favorites, onPlay, onToggleLike, onAddToQueue, onNavigateToBrowse)
        HomeSectionLayout.CAROUSEL -> {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(section.items.take(10), key = { "${it.type}-${it.title}-${it.browseId}" }) { item ->
                    if (item.videoId != null && (item.type == ItemType.SONG || item.type == ItemType.VIDEO)) {
                        Box(modifier = Modifier.width(300.dp)) {
                            HomeSongRow(
                                item = item,
                                liked = item.videoId in favorites,
                                onPlay = { onPlay(item) },
                                onToggleLike = { onToggleLike(item) },
                                onAddToQueue = { onAddToQueue(item) },
                                onOpenArtist = { onNavigateToBrowse(ItemType.ARTIST, it) },
                            )
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
    favorites: Set<String>,
    onPlay: (BrowseItem) -> Unit,
    onToggleLike: (BrowseItem) -> Unit,
    onAddToQueue: (BrowseItem) -> Unit,
    onNavigateToBrowse: (ItemType, String) -> Unit,
) {
    Column {
        section.items.forEachIndexed { index, item ->
            when {
                item.videoId != null && (item.type == ItemType.SONG || item.type == ItemType.VIDEO) -> {
                    HomeSongRow(
                        item = item,
                        liked = item.videoId in favorites,
                        onPlay = { onPlay(item) },
                        onToggleLike = { onToggleLike(item) },
                        onAddToQueue = { onAddToQueue(item) },
                        onOpenArtist = { onNavigateToBrowse(ItemType.ARTIST, it) },
                        index = index + 1,
                    )
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
private fun HomeSongRow(
    item: BrowseItem,
    liked: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenArtist: (String) -> Unit,
    index: Int? = null,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null) {
            Text(
                text = index.toString(),
                modifier = Modifier.width(28.dp),
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SongArtwork(url = item.thumbnailUrl, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = VibeOnText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            val artistLine = item.artists.joinToString(", ").ifBlank { item.subtitle }
            val detail = if (item.playCount.isNotBlank()) "$artistLine · ${item.playCount}" else artistLine
            Text(
                text = detail,
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        IconButton(onClick = onToggleLike) {
            Icon(
                imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (liked) "Unlike" else "Like",
                tint = if (liked) VibeOnPrimary else VibeOnTextMuted,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = VibeOnTextMuted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    onClick = { menuOpen = false; onAddToQueue() },
                )
                item.artistIds.firstOrNull()?.let { artistId ->
                    DropdownMenuItem(
                        text = { Text("Go to artist") },
                        onClick = { menuOpen = false; onOpenArtist(artistId) },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        menuOpen = false
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${item.videoId}")
                        }
                        runCatching { context.startActivity(Intent.createChooser(intent, "Share song")) }
                    },
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
            val meta = listOf(item.badge, item.creator, item.playCount)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            // Prefer extracted metadata; the raw subtitle repeats creator/views.
            val subtitle = meta.ifBlank { item.subtitle }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
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
                    color = if (isSelected) Color.White else VibeOnText,
                )
            }
        }
    }
}

@Composable
private fun SectionKicker(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        color = VibeOnTextMuted,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
private fun ListeningTimePill(minutes: Long) {
    val label = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    Row(
        modifier = Modifier
            .background(VibeOnBgCardLight, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Headset,
            contentDescription = null,
            tint = VibeOnPrimaryLight,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = VibeOnText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InfiniteRadioCard(onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(154.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF4A2C6D), Color(0xFF241A33), Color(0xFF15131F)),
                ),
            )
            .clickable(onClick = onPlay)
            .padding(20.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopEnd),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Infinite Radio",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "An endless station shaped by your listening",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Play",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E3A5F), Color(0xFF16263A), Color(0xFF131820)),
                ),
            )
            .clickable(onClick = onOpen)
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Discover Mix",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "New discoveries",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Explore",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

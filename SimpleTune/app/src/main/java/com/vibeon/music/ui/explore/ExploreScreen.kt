package com.vibeon.music.ui.explore

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.domain.model.ExploreTile
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.ui.components.BrowseCard
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun ExploreScreen(
    onNavigateToBrowse: (ItemType, String) -> Unit,
    onBack: () -> Unit,
    mode: String = "discover",
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val title by viewModel.title.collectAsState()
    val inCategory by viewModel.inCategory.collectAsState()

    LaunchedEffect(mode) { viewModel.setMode(mode) }

    GradientBackground {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = if (inCategory) viewModel::backToLanding else onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = VibeOnText)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = VibeOnText,
                    )
                }
            }

            when {
                loading && sections.isEmpty() && tiles.isEmpty() -> item { LoadingIndicator() }
                error != null && sections.isEmpty() && tiles.isEmpty() -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                        EmptyState(text = error ?: "", onRetry = { viewModel.load() })
                    }
                }
                else -> {
                    if (tiles.isNotEmpty()) {
                        item {
                            Text(
                                text = if (inCategory) "More like this" else if (mode == "genres") "Genres" else "Browse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VibeOnText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            MoodTileGrid(tiles = tiles, onOpenTile = { viewModel.openCategory(it) })
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    sections.forEach { section ->
                        item(key = "exp_title_${section.title}") {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VibeOnText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        item(key = "exp_row_${section.title}") {
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(section.items.take(20), key = { "${it.type}-${it.title}-${it.browseId}-${it.videoId}" }) { item ->
                                    val id = item.browseId ?: item.videoId
                                    if (id != null) {
                                        BrowseCard(
                                            item = item,
                                            onClick = {
                                                if (item.videoId != null && item.browseId == null) {
                                                    viewModel.playItem(item)
                                                } else {
                                                    onNavigateToBrowse(item.type, id)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MoodTileGrid(
    tiles: List<ExploreTile>,
    onOpenTile: (com.vibeon.music.domain.model.ExploreTile) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.chunked(3).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTiles.forEach { tile ->
                    MoodTile(tile = tile, onClick = { onOpenTile(tile) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - rowTiles.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MoodTile(
    tile: com.vibeon.music.domain.model.ExploreTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = tile.color ?: 0xFF3A3A3A
    val argb = (0xFF000000 or (base and 0xFFFFFF)).toInt()
    Box(
        modifier = modifier
            .height(74.dp)
            .background(Color(argb), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = tile.label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
    }
}

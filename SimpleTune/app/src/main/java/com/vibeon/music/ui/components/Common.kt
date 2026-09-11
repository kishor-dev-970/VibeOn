package com.vibeon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.domain.model.Song
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnBorder
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnSuccess
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF101010),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, size: Int = 32) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.dp),
            color = VibeOnPrimary,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = VibeOnTextMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = VibeOnText,
    )
}

@Composable
fun SongArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    cornerShape: RoundedCornerShape = RoundedCornerShape(10.dp),
) {
    Box(
        modifier = modifier
            .clip(cornerShape)
            .background(VibeOnBgCardLight)
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    index: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null) {
            Text(
                text = index.toString(),
                modifier = Modifier.width(28.dp),
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        SongArtwork(
            url = song.thumbnailUrl,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = VibeOnText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun BrowseCard(
    item: BrowseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expand: Boolean = false,
) {
    val width = when (item.type) {
        ItemType.SONG, ItemType.VIDEO -> 150.dp
        else -> 140.dp
    }
    Column(
        modifier = modifier
            .then(if (expand) Modifier.fillMaxWidth() else Modifier.width(width))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box {
            SongArtwork(
                url = item.thumbnailUrl,
                modifier = if (expand) {
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                } else {
                    Modifier.size(width, width * 0.75f)
                },
            )
            when (item.type) {
                ItemType.SONG, ItemType.VIDEO -> {
                    Box(
                        modifier = Modifier
                            .size(width * 0.75f, width * 0.6f)
                            .padding(8.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
                else -> Unit
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            color = VibeOnText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun Avatar(
    name: String,
    url: String?,
    size: Int = 48,
    showOnlineDot: Boolean = false,
) {
    Box {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
.background(Brush.linearGradient(listOf(VibeOnBgCard, VibeOnBgCardLight)))
            .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(VibeOnBgCardLight),
                contentAlignment = Alignment.Center,
            ) {
                if (!url.isNullOrBlank()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = initials(name),
                        color = VibeOnText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = androidx.compose.ui.unit.TextUnit(
                            size.dp.value / 2.2f,
                            androidx.compose.ui.unit.TextUnitType.Sp
                        ),
                    )
                }
            }
        }
        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(VibeOnSuccess)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

private fun initials(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        words.size == 1 -> words[0].take(2)
        else -> "?"
    }.uppercase()
}

@Composable
fun MiniPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(VibeOnBgCardLight, VibeOnPrimary)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!isPlaying) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF000000),
                modifier = Modifier.size(26.dp).padding(start = 3.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = "Pause",
                tint = Color(0xFF000000),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VibeOnBgCard)
            .border(1.dp, VibeOnBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = VibeOnTextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
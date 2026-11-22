package com.vibeon.music.ui.player

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import com.vibeon.music.data.settings.AudioQuality
import com.vibeon.music.domain.model.LyricLine
import com.vibeon.music.domain.model.Song
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.LoadingIndicator
import com.vibeon.music.ui.components.SongArtwork
import com.vibeon.music.ui.theme.VibeOnBg
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted
import kotlinx.coroutines.launch

private val GlassFill = Color(0x14FFFFFF)
private val GlassBorder = Color(0x33FFFFFF)
private val OutlineBorder = Color(0x66FFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val song by viewModel.song.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val position by viewModel.position.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val queue by viewModel.queueSongs.collectAsState()
    val queueIndex by viewModel.queueIndex.collectAsState()
    val error by viewModel.error.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val activeLyric by viewModel.activeLyricIndex.collectAsState()
    val shuffleOn by viewModel.shuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()

    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var lastRemoveAt by remember { mutableLongStateOf(0L) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeOnBg),
    ) {
        val heroHeight = maxHeight * 0.5f

        Column(modifier = Modifier.fillMaxSize()) {
            // Full-bleed cover art, like LastWave's player hero.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight),
            ) {
                SongArtwork(
                    url = song?.thumbnailUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerShape = RoundedCornerShape(0.dp),
                    backgroundColor = VibeOnBgCard,
                )
                // Scrim behind the status bar / top controls.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x73000000), Color.Transparent),
                            ),
                        ),
                )
                // Fade the artwork into the page background.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.55f to Color.Transparent,
                                0.8f to VibeOnBg.copy(alpha = 0.6f),
                                1f to VibeOnBg,
                            ),
                        ),
                )
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(46.dp),
                        color = VibeOnText,
                        strokeWidth = 3.dp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(16.dp))

                TrackHeader(
                    song = song,
                    isFavorite = isFavorite,
                    onFavorite = viewModel::onToggleFavorite,
                    onLyrics = { showLyrics = true },
                )

                Spacer(Modifier.height(22.dp))

                ProgressBlock(
                    position = position,
                    durationMs = durationMs,
                    onSeek = viewModel::onSeek,
                )

                Spacer(Modifier.height(10.dp))

                AudioPill(
                    quality = audioQuality,
                    onClick = { showQuality = true },
                )

                Spacer(Modifier.weight(1f))

                TransportRow(
                    isPlaying = isPlaying,
                    onPrevious = viewModel::onPrevious,
                    onTogglePlay = viewModel::onTogglePlay,
                    onNext = viewModel::onNext,
                )

                Spacer(Modifier.height(16.dp))

                UtilityRow(
                    shuffleOn = shuffleOn,
                    repeatOn = repeatMode != Player.REPEAT_MODE_OFF,
                    repeatOne = repeatMode == Player.REPEAT_MODE_ONE,
                    queueCount = queue.size,
                    onShuffle = viewModel::onToggleShuffle,
                    onQueue = { showQueue = true },
                    onRepeat = viewModel::toggleRepeatMode,
                )

                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }

        TopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            onDismiss = onDismiss,
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onQueue = {
                menuOpen = false
                showQueue = true
            },
            onShare = {
                menuOpen = false
                shareLyrics(context, song, lyrics)
            },
            onQuality = {
                menuOpen = false
                showQuality = true
            },
        )

        if (error != null) {
            Text(
                text = error ?: "",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp, start = 24.dp, end = 24.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    if (showLyrics) {
        LyricsSheet(
            song = song,
            lyrics = lyrics,
            activeLyric = activeLyric,
            onRetry = viewModel::retryLyrics,
            onShare = { shareLyrics(context, song, lyrics) },
            onDismiss = { showLyrics = false },
        )
    }

    if (showQueue) {
        QueueSheet(
            queue = queue,
            queueIndex = queueIndex,
            snackbarHostState = snackbarHostState,
            onJump = { index ->
                viewModel.onJumpToQueue(index)
                showQueue = false
            },
            onMove = viewModel::onMoveQueue,
            onRemove = { index ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastRemoveAt > 400L) {
                    lastRemoveAt = now
                    val (removed, snapshot) = viewModel.onRemoveQueue(index)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Removed ${removed?.title ?: "song"}",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onRestoreQueue(snapshot)
                        }
                    }
                }
            },
            onDismiss = { showQueue = false },
        )
    }

    if (showQuality) {
        QualitySheet(
            current = audioQuality,
            onSelect = { quality ->
                viewModel.setAudioQuality(quality)
                showQuality = false
            },
            onDismiss = { showQuality = false },
        )
    }
}

@Composable
private fun TopBar(
    onDismiss: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onQueue: () -> Unit,
    onShare: () -> Unit,
    onQuality: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(
                onClick = onDismiss,
                icon = Icons.Rounded.KeyboardArrowDown,
                description = "Close player",
            )
            Box {
                GlassCircleButton(
                    onClick = { onMenuOpenChange(true) },
                    icon = Icons.Rounded.MoreVert,
                    description = "More options",
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text("Queue") },
                        onClick = onQueue,
                    )
                    DropdownMenuItem(
                        text = { Text("Share lyrics") },
                        onClick = onShare,
                    )
                    DropdownMenuItem(
                        text = { Text("Audio quality") },
                        onClick = onQuality,
                    )
                }
            }
        }
        Text(
            text = "NOW PLAYING",
            modifier = Modifier.align(Alignment.Center),
            color = VibeOnText.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x3D000000))
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = VibeOnText,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TrackHeader(
    song: Song?,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 108.dp),
        ) {
            Text(
                text = song?.title ?: "Nothing playing",
                color = VibeOnText,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song?.artists?.joinToString(", ") { it.name }?.ifBlank { "Unknown artist" } ?: "—",
                color = VibeOnText.copy(alpha = 0.62f),
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlineCircleButton(
                onClick = onFavorite,
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                description = if (isFavorite) "Remove from Liked" else "Add to Liked",
                active = isFavorite,
            )
            OutlineCircleButton(
                onClick = onLyrics,
                icon = Icons.Rounded.FormatQuote,
                description = "Lyrics",
            )
        }
    }
}

@Composable
private fun OutlineCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    active: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) Color(0x26FFFFFF) else Color.Transparent)
            .border(1.dp, OutlineBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) VibeOnText else VibeOnText.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ProgressBlock(
    position: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    Scrubber(
        position = position,
        durationMs = durationMs,
        onSeek = onSeek,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(position),
            color = VibeOnText.copy(alpha = 0.6f),
            fontSize = 13.sp,
        )
        Text(
            text = "-" + formatTime((durationMs - position).coerceAtLeast(0L)),
            color = VibeOnText.copy(alpha = 0.6f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun Scrubber(
    position: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    val liveFraction = (position.toFloat() / duration).coerceIn(0f, 1f)
    val fraction = (dragFraction ?: liveFraction).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { 4.dp.toPx() }
    val trackHeightPx = with(density) { 4.dp.toPx() }
    val thumbHeightPx = with(density) { 20.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width * duration).toLong())
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    },
                    onDragEnd = {
                        dragFraction?.let { onSeek((it * duration).toLong()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                )
            },
    ) {
        val centerY = size.height / 2f
        val radius = trackHeightPx / 2f
        val thumbX = (size.width - thumbWidthPx) * fraction + thumbWidthPx / 2f

        // Thin uniform track, like LastWave's player.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(0f, centerY - radius),
            size = Size(size.width, trackHeightPx),
            cornerRadius = CornerRadius(radius, radius),
        )
        if (fraction > 0f) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(0f, centerY - radius),
                size = Size(thumbX, trackHeightPx),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
        // Small end dot, matching the Material track terminus.
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = radius,
            center = Offset(size.width - radius, centerY),
        )
        // Slim vertical-bar thumb.
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(thumbX - thumbWidthPx / 2f, centerY - thumbHeightPx / 2f),
            size = Size(thumbWidthPx, thumbHeightPx),
            cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f),
        )
    }
}

@Composable
private fun AudioPill(quality: AudioQuality, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassFill)
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = VibeOnText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Audio · ${quality.label()}",
                color = VibeOnText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = VibeOnText.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val pillWidth = (maxWidth - 56.dp * 2 - 14.dp * 2).coerceAtLeast(120.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportCircle(
                icon = Icons.Rounded.SkipPrevious,
                description = "Previous",
                onClick = onPrevious,
            )
            Row(
                modifier = Modifier
                    .width(pillWidth)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x0FFFFFFF))
                    .border(1.dp, OutlineBorder, RoundedCornerShape(28.dp))
                    .clickable(onClick = onTogglePlay),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = VibeOnText,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isPlaying) "Pause" else "Play",
                    color = VibeOnText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TransportCircle(
                icon = Icons.Rounded.SkipNext,
                description = "Next",
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun TransportCircle(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(1.dp, OutlineBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = VibeOnText,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun UtilityRow(
    shuffleOn: Boolean,
    repeatOn: Boolean,
    repeatOne: Boolean,
    queueCount: Int,
    onShuffle: () -> Unit,
    onQueue: () -> Unit,
    onRepeat: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val usable = maxWidth - gap * 2
        val sideWidth = usable * 0.29f
        val queueWidth = usable * 0.42f

        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UtilityPill(
                modifier = Modifier.width(sideWidth).height(44.dp),
                active = shuffleOn,
                onClick = onShuffle,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = if (shuffleOn) "Shuffle on" else "Shuffle off",
                    tint = if (shuffleOn) VibeOnText else VibeOnText.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp),
                )
            }
            UtilityPill(
                modifier = Modifier.width(queueWidth).height(44.dp),
                active = false,
                onClick = onQueue,
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = VibeOnText.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color(0x40FFFFFF)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = queueCount.toString(),
                    color = VibeOnText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            UtilityPill(
                modifier = Modifier.width(sideWidth).height(44.dp),
                active = repeatOn,
                onClick = onRepeat,
            ) {
                Icon(
                    imageVector = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = when {
                        repeatOne -> "Repeat one"
                        repeatOn -> "Repeat all"
                        else -> "Repeat off"
                    },
                    tint = if (repeatOn) VibeOnText else VibeOnText.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun UtilityPill(
    modifier: Modifier = Modifier,
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (active) Color(0x26FFFFFF) else Color(0x0FFFFFFF))
            .border(
                width = 1.dp,
                color = if (active) Color(0x80FFFFFF) else GlassBorder,
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSheet(
    song: Song?,
    lyrics: LyricsUiState,
    activeLyric: Int,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    LaunchedEffect(activeLyric, lyrics.lines.size, lyrics.synced) {
        if (lyrics.synced && activeLyric in lyrics.lines.indices) {
            runCatching { listState.animateScrollToItem(activeLyric, -160) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VibeOnBgCard,
        contentColor = VibeOnText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lyrics",
                        color = VibeOnText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            lyrics.loading -> "Fetching lyrics…"
                            lyrics.synced -> "Time-synced · LRCLIB"
                            lyrics.lines.isNotEmpty() -> "Plain text · LRCLIB"
                            else -> song?.title ?: ""
                        },
                        color = VibeOnTextMuted,
                        fontSize = 12.sp,
                    )
                }
                if (lyrics.lines.isNotEmpty()) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share lyrics",
                            tint = VibeOnTextMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                lyrics.loading -> LoadingIndicator()
                lyrics.lines.isEmpty() -> EmptyState(
                    text = lyrics.message ?: "No lyrics available for this song",
                    onRetry = onRetry,
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        LyricLineRow(
                            line = line,
                            active = lyrics.synced && index == activeLyric,
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun LyricLineRow(line: LyricLine, active: Boolean) {
    Text(
        text = line.text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0x14FFFFFF) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = if (active) 12.dp else 8.dp),
        color = if (active) VibeOnText else VibeOnTextMuted,
        fontSize = if (active) 19.sp else 16.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<Song>,
    queueIndex: Int,
    snackbarHostState: SnackbarHostState,
    onJump: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VibeOnBgCard,
        contentColor = VibeOnText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Up next",
                    color = VibeOnText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${queue.size} songs · drag to reorder",
                    color = VibeOnTextMuted,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(6.dp))

            if (queue.isEmpty()) {
                EmptyState(text = "Nothing in the queue yet")
            } else {
                QueueList(
                    queue = queue,
                    queueIndex = queueIndex,
                    onJump = onJump,
                    onMove = onMove,
                    onRemove = onRemove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueList(
    queue: List<Song>,
    queueIndex: Int,
    onJump: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowHeightPx = with(LocalDensity.current) { 62.dp.toPx() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val latestQueue by rememberUpdatedState(queue)

    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = queue,
            key = { index, item ->
                val occurrence = queue.subList(0, index).count { it.videoId == item.videoId }
                "${item.videoId}#$occurrence"
            },
        ) { index, item ->
            var dismissed by remember { mutableStateOf(false) }
            val dismissState = rememberSwipeToDismissBoxState(
                SwipeToDismissBoxValue.Settled,
                { value: SwipeToDismissBoxValue ->
                    if (value == SwipeToDismissBoxValue.EndToStart && !dismissed) {
                        dismissed = true
                        onRemove(index)
                        true
                    } else {
                        false
                    }
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.padding(vertical = 3.dp),
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val background by androidx.compose.animation.animateColorAsState(
                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                            Color(0xFFB3261E)
                        } else {
                            Color.Transparent
                        },
                        label = "queue-dismiss-background",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(background, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remove",
                            tint = Color.White,
                        )
                    }
                },
                content = {
                    QueueRow(
                        song = item,
                        index = index,
                        isCurrent = index == queueIndex,
                        dragOffset = if (draggingId == item.videoId) dragOffset else 0f,
                        onJump = { onJump(index) },
                        onDragStart = {
                            draggingId = item.videoId
                            dragOffset = 0f
                        },
                        onDrag = { amount ->
                            dragOffset += amount
                            val steps = (dragOffset / rowHeightPx).toInt()
                            if (steps != 0) {
                                val from = latestQueue.indexOfFirst { it.videoId == draggingId }
                                val to = (from + steps).coerceIn(0, latestQueue.lastIndex)
                                if (from >= 0 && to != from) {
                                    onMove(from, to)
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDragEnd = {
                            draggingId = null
                            dragOffset = 0f
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    dragOffset: Float,
    onJump: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffset }
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) Color(0xFF242424) else VibeOnBgCard)
            .clickable(onClick = onJump)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Drag to reorder",
            tint = VibeOnTextMuted,
            modifier = Modifier
                .size(26.dp)
                .pointerInput(song.videoId) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                    )
                },
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = (index + 1).toString(),
            modifier = Modifier.width(22.dp),
            color = if (isCurrent) VibeOnText else VibeOnTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        SongArtwork(
            url = song.thumbnailUrl,
            modifier = Modifier.size(40.dp),
            cornerShape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) VibeOnText else VibeOnText,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                color = VibeOnTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySheet(
    current: AudioQuality,
    onSelect: (AudioQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VibeOnBgCard,
        contentColor = VibeOnText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Audio quality",
                color = VibeOnText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "VibeOn streams the best available audio at this level.",
                color = VibeOnTextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            AudioQuality.entries.forEach { quality ->
                val selected = quality == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color(0x1AFFFFFF) else Color.Transparent)
                        .clickable { onSelect(quality) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quality.label(),
                            color = VibeOnText,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = quality.description(),
                            color = VibeOnTextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = VibeOnText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun AudioQuality.label(): String = when (this) {
    AudioQuality.LOW -> "Low"
    AudioQuality.NORMAL -> "Normal"
    AudioQuality.HIGH -> "High"
}

private fun AudioQuality.description(): String = when (this) {
    AudioQuality.LOW -> "Saves data"
    AudioQuality.NORMAL -> "Balanced"
    AudioQuality.HIGH -> "Best available"
}

private fun shareLyrics(context: Context, song: Song?, lyrics: LyricsUiState) {
    if (song == null || lyrics.lines.isEmpty()) return
    val text = buildString {
        append(song.title)
        append(" — ")
        append(song.artists.joinToString(", ") { it.name })
        append("\n\n")
        lyrics.lines.forEach { append(it.text).append('\n') }
        append("\nShared from VibeOn")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share lyrics"))
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

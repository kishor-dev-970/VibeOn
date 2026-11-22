package com.vibeon.music.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibeon.music.ui.components.EmptyState
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun ListeningStatsScreen(
    onBack: () -> Unit,
    viewModel: ListeningStatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsState()

    GradientBackground {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = VibeOnText)
                    }
                    Icon(Icons.Rounded.BarChart, contentDescription = null, tint = VibeOnPrimary)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = "Listening stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = VibeOnText,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard("Songs", stats.uniqueSongs.toString(), Modifier.weight(1f))
                    StatCard("Liked", stats.favorites.toString(), Modifier.weight(1f))
                    StatCard("Minutes", stats.totalMinutes.toString(), Modifier.weight(1f))
                }
            }

            item {
                Text(
                    text = "Top artists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VibeOnText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
                if (stats.topArtists.isEmpty()) {
                    EmptyState("Play a few songs to see your top artists")
                } else {
                    val max = stats.topArtists.maxOf { it.count }
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        stats.topArtists.forEachIndexed { index, artist ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(VibeOnBgCard, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                            ) {
                                Row {
                                    Text(
                                        text = "${index + 1}. ${artist.name}",
                                        color = VibeOnText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "${artist.count} songs",
                                        color = VibeOnTextMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(VibeOnTextMuted.copy(alpha = 0.2f), RoundedCornerShape(50)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(artist.count.toFloat() / max.coerceAtLeast(1))
                                            .height(4.dp)
                                            .background(VibeOnPrimary, RoundedCornerShape(50)),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(VibeOnBgCard, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(value, color = VibeOnPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, color = VibeOnTextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

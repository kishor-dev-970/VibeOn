package com.vibeon.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.navigation.BottomTab
import com.vibeon.music.ui.theme.VibeOnGlassBg
import com.vibeon.music.ui.theme.VibeOnGlassBgStrong
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnTextMuted

/**
 * LastWave-style frosted navigation bar: a spring-animated pill slides behind
 * the active tab while the pager swaps the page underneath it.
 */
@Composable
fun VibeOnTabBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    playbackManager: PlaybackManager,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = BottomTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
    val indicator by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "vibeon_tab_indicator",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VibeOnGlassBgStrong)
            .padding(horizontal = 10.dp)
            .navigationBarsPadding(),
    ) {
        MiniPlayer(
            playbackManager = playbackManager,
            onOpenNowPlaying = onOpenNowPlaying,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / tabs.size
            GlassSurface(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(26.dp),
                tint = VibeOnGlassBg,
                borderColor = Color.White.copy(alpha = 0.12f),
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = tabWidth * indicator)
                        .padding(6.dp)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .background(VibeOnPrimary.copy(alpha = 0.22f), RoundedCornerShape(20.dp)),
                )
                Row(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    tabs.forEach { tab ->
                        TabItem(
                            tab = tab,
                            selected = currentTab == tab,
                            onClick = { onTabSelected(tab) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val icon = when (tab) {
        BottomTab.Library -> Icons.Rounded.LibraryMusic
        BottomTab.Home -> Icons.Rounded.Home
        BottomTab.Friends -> Icons.Rounded.People
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = if (selected) Color.White else VibeOnTextMuted,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else VibeOnTextMuted,
        )
    }
}

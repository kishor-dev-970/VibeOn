package com.vibeon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibeon.music.player.PlaybackManager
import com.vibeon.music.ui.navigation.BottomTab
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnTabBarBg
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted

@Composable
fun VibeOnTabBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    playbackManager: PlaybackManager,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VibeOnTabBarBg)
            .padding(horizontal = 10.dp)
            .navigationBarsPadding(),
    ) {
        MiniPlayer(
            playbackManager = playbackManager,
            onOpenNowPlaying = onOpenNowPlaying,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VibeOnBgCard, RoundedCornerShape(24.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BottomTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                )
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
            .background(
                if (selected) Color(0xFF2A2A2A) else Color.Transparent,
                RoundedCornerShape(18.dp),
            )
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
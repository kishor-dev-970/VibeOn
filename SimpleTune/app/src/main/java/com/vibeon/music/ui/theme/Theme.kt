package com.vibeon.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = VibeOnPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = VibeOnPrimaryLight,
    secondary = VibeOnSecondary,
    onSecondary = Color(0xFF000000),
    tertiary = VibeOnAccent,
    onTertiary = Color(0xFF000000),
    background = VibeOnBg,
    onBackground = VibeOnText,
    surface = VibeOnBgCard,
    onSurface = VibeOnText,
    surfaceVariant = VibeOnBgCardLight,
    onSurfaceVariant = VibeOnTextMuted,
    error = VibeOnError,
    outline = VibeOnBorder,
)

@Composable
fun VibeOnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = VibeOnTypography,
        content = content,
    )
}
package com.rmatch.football.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RMatchColors = darkColorScheme(
    primary = RMatchAccent,
    onPrimary = RMatchBackground,
    primaryContainer = RMatchAccentDark,
    onPrimaryContainer = RMatchOnDark,
    secondary = RMatchSecondary,
    onSecondary = RMatchBackground,
    background = RMatchBackground,
    onBackground = RMatchOnDark,
    surface = RMatchSurface,
    onSurface = RMatchOnDark,
    surfaceVariant = RMatchSurfaceVariant,
    onSurfaceVariant = RMatchMuted,
    outline = RMatchOutline,
    error = RMatchError,
    onError = RMatchOnDark
)

@Composable
fun RMatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RMatchColors,
        typography = RMatchTypography,
        content = content
    )
}

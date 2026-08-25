package com.rogermichin.rmatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF42D392),
    secondary = Color(0xFF77D7FF),
    tertiary = Color(0xFFFFC857),
    background = Color(0xFF0B1220),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onPrimary = Color.Black,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
)

@Composable
fun RMatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}

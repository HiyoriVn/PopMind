package com.example.popmind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    secondary = Orange, onSecondary = Ink,
    tertiary = TealLight,
    background = Color(0xFFF5FAF8), onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = TealPale, onSurfaceVariant = Color(0xFF496461)
)

private val DarkColors = darkColorScheme(
    primary = TealLight, onPrimary = Night,
    secondary = Orange, onSecondary = Night,
    tertiary = TealLight,
    background = Night, onBackground = Color(0xFFE6F3F0),
    surface = Color(0xFF193431), onSurface = Color(0xFFE6F3F0),
    surfaceVariant = Color(0xFF254541), onSurfaceVariant = Color(0xFFB8D0CC)
)

@Composable
fun PopMindTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography, content = content)
}

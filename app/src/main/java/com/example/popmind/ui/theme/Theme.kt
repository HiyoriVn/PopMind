package com.example.popmind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    primaryContainer = Mint, onPrimaryContainer = Color(0xFF0F766E),
    secondary = Orange, onSecondary = Color.White,
    secondaryContainer = OrangePale, onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Color(0xFF14B8A6), onTertiary = Color.White,
    background = Color(0xFFF8FAFC), onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Slate,
    outline = Color(0xFFCBD5E1), outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFB45309), onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = TealLight, onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFFB923C), onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF7C2D12), onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFF2DD4BF), onTertiary = Color(0xFF042F2E),
    background = Night, onBackground = Color(0xFFF8FAFC),
    surface = DarkCard, onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B), outlineVariant = Color(0xFF334155),
    error = Color(0xFFFBBF24), onError = Color(0xFF422006)
)

private val PopMindShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PopMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        shapes = PopMindShapes,
        content = content
    )
}

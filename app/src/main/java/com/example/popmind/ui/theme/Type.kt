package com.example.popmind.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Dùng SansSerif cho tới khi thêm hai font local được ghi trong docs/design/.../FONTS.md.
val PlusJakartaSans: FontFamily = FontFamily.SansSerif
val BeVietnamPro: FontFamily = FontFamily.SansSerif

private fun heading(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = PlusJakartaSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = (-0.25).sp
)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = BeVietnamPro, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp
)

val Typography = Typography(
    displayLarge = heading(40, 48, FontWeight.Bold),
    displayMedium = heading(36, 44, FontWeight.Bold),
    displaySmall = heading(32, 40, FontWeight.Bold),
    headlineLarge = heading(28, 36, FontWeight.Bold),
    headlineMedium = heading(24, 32),
    headlineSmall = heading(20, 28),
    titleLarge = heading(22, 30),
    titleMedium = heading(18, 26),
    titleSmall = heading(16, 24),
    bodyLarge = body(16, 25),
    bodyMedium = body(14, 22),
    bodySmall = body(12, 18),
    labelLarge = heading(14, 20),
    labelMedium = heading(12, 18),
    labelSmall = heading(11, 16, FontWeight.Medium)
)

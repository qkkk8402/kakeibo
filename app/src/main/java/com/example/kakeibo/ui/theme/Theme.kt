package com.example.kakeibo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF356859),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8D4),
    onPrimaryContainer = Color(0xFF0D2117),
    secondary = Color(0xFF6B5E45),
    secondaryContainer = Color(0xFFF0E1C2),
    error = Color(0xFFB3261E),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE8E8E3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D5BA),
    onPrimary = Color(0xFF123526),
    primaryContainer = Color(0xFF1E4D3B),
    onPrimaryContainer = Color(0xFFD5E8D4),
    secondary = Color(0xFFD8C69F),
    secondaryContainer = Color(0xFF50462F),
    error = Color(0xFFFFB4AB)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun KakeiboTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}

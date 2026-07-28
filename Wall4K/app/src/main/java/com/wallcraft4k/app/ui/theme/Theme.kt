package com.wallcraft4k.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Wallcraft-style: amber/gold accent on a deep navy background.
private val Gold = Color(0xFFF5B301)
private val GoldSoft = Color(0xFFFFD35A)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1200),
    secondary = GoldSoft,
    onSecondary = Color(0xFF1A1200),
    background = Color(0xFF0C111C),
    onBackground = Color(0xFFEDEFF5),
    surface = Color(0xFF121826),
    onSurface = Color(0xFFEDEFF5),
    surfaceVariant = Color(0xFF1B2233),
    onSurfaceVariant = Color(0xFFAAB2C5),
    outline = Color(0xFF2C3547)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB8860B),
    onPrimary = Color.White,
    secondary = Gold,
    background = Color(0xFFF4F5F9),
    onBackground = Color(0xFF12151C),
    surface = Color.White,
    onSurface = Color(0xFF12151C),
    surfaceVariant = Color(0xFFE7E9F0),
    onSurfaceVariant = Color(0xFF4A4E58),
    outline = Color(0xFFD3D6DE)
)

private val AppTypography = Typography()

@Composable
fun Wall4KTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

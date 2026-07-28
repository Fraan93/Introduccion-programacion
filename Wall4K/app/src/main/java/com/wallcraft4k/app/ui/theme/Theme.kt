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

private val Purple = Color(0xFF6C4DF6)
private val PurpleLight = Color(0xFF9B84FF)
private val Accent = Color(0xFF00D0BD)

private val DarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color(0xFF00201C),
    background = Color(0xFF0B0B10),
    onBackground = Color(0xFFECEBF4),
    surface = Color(0xFF14141C),
    onSurface = Color(0xFFECEBF4),
    surfaceVariant = Color(0xFF1E1E28),
    onSurfaceVariant = Color(0xFFB9B7C9),
    outline = Color(0xFF34343F)
)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF16151C),
    surface = Color.White,
    onSurface = Color(0xFF16151C),
    surfaceVariant = Color(0xFFEBE9F5),
    onSurfaceVariant = Color(0xFF4A4954),
    outline = Color(0xFFD5D3E0)
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

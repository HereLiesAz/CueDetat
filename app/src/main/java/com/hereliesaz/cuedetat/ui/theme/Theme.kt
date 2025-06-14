package com.hereliesaz.cuedetat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppDarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = MutedTeal,
    tertiary = MutedGray,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    outline = MutedGray.copy(alpha = 0.5f),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    tertiaryContainer = MutedTeal.copy(alpha = 0.3f),
    onTertiaryContainer = Color.White,
    secondaryContainer = AccentGold.copy(alpha = 0.3f),
    onSecondaryContainer = Color.White,

    )

private val AppLightColorScheme = lightColorScheme(
    primary = DarkerAccentGold,
    secondary = MutedTeal,
    tertiary = MutedGray,
    background = Color(0xFFF7F2F9),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    outline = MutedGray.copy(alpha = 0.5f),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    tertiaryContainer = MutedTeal.copy(alpha = 0.3f),
    onTertiaryContainer = Color.Black,
    secondaryContainer = AccentGold.copy(alpha = 0.3f),
    onSecondaryContainer = Color.Black,
)

@Composable
fun CueDetatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb() // Make status bar transparent
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
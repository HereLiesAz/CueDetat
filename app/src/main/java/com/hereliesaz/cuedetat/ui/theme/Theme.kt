package com.hereliesaz.cuedetat.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// The one and only theme for the app, built from the "Gilded 8-Ball" palette.
private val AppColorScheme = darkColorScheme(
    primary = G8_Primary,
    onPrimary = G8_OnPrimary,
    primaryContainer = G8_PrimaryContainer,
    onPrimaryContainer = G8_OnPrimaryContainer,
    secondary = G8_Secondary,
    onSecondary = G8_OnSecondary,
    secondaryContainer = G8_SecondaryContainer,
    onSecondaryContainer = G8_OnSecondaryContainer,
    tertiary = G8_Tertiary,
    onTertiary = G8_OnTertiary,
    tertiaryContainer = G8_TertiaryContainer,
    onTertiaryContainer = G8_OnTertiaryContainer,
    error = G8_Error,
    onError = G8_OnError,
    background = G8_Background,
    onBackground = G8_OnBackground,
    surface = G8_Surface,
    onSurface = G8_OnSurface,
    outline = G8_Outline
)

@Composable
fun CueDetatTheme(
    dynamicColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    // This theme is now independent of the system's light/dark mode.
    // It will use the dynamic scheme if one is generated, otherwise it defaults
    // to our custom "Gilded 8-Ball" dark theme.
    val colorScheme = dynamicColorScheme ?: AppColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Since it's always a dark theme, isAppearanceLightStatusBars is always false.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
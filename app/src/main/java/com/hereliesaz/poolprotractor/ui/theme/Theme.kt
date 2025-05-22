package com.hereliesaz.poolprotractor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,       // Light Purple for Dark Theme Primary
    secondary = PurpleGrey80, // Light PurpleGrey for Dark Theme Secondary
    tertiary = Pink80,        // Light Pink for Dark Theme Tertiary
    onSurface = Color(0xFFE6E1E5) // A light color for text on dark surfaces
    // ... other M3 colors like outline, error etc. should also be light for dark theme
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,       // Darker Purple for Light Theme Primary
    secondary = PurpleGrey40, // Darker PurpleGrey for Light Theme Secondary
    tertiary = Pink40,        // Darker Pink for Light Theme Tertiary
    onSurface = Color(0xFF1C1B1F) // A dark color for text on light surfaces
    // ... other M3 colors like outline etc. should be dark for light theme

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F), // Explicitly set here for clarity
    */
)


@Composable
fun PoolProtractorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
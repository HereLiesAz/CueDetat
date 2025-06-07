package com.hereliesaz.poolprotractor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = expressive_dark_primary,
    onPrimary = expressive_dark_onPrimary,
    primaryContainer = expressive_dark_primaryContainer,
    onPrimaryContainer = expressive_dark_onPrimaryContainer,
    secondary = expressive_dark_secondary,
    onSecondary = expressive_dark_onSecondary,
    secondaryContainer = expressive_dark_secondaryContainer,
    onSecondaryContainer = expressive_dark_onSecondaryContainer,
    tertiary = expressive_dark_tertiary,
    onTertiary = expressive_dark_onTertiary,
    tertiaryContainer = expressive_dark_tertiaryContainer,
    onTertiaryContainer = expressive_dark_onTertiaryContainer,
    error = expressive_dark_error,
    onError = expressive_dark_onError,
    background = expressive_dark_background,
    onBackground = expressive_dark_onBackground,
    surface = expressive_dark_surface,
    onSurface = expressive_dark_onSurface,
    outline = expressive_dark_outline
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = expressive_light_primary,
    onPrimary = expressive_light_onPrimary,
    primaryContainer = expressive_light_primaryContainer,
    onPrimaryContainer = expressive_light_onPrimaryContainer,
    secondary = expressive_light_secondary,
    onSecondary = expressive_light_onSecondary,
    secondaryContainer = expressive_light_secondaryContainer,
    onSecondaryContainer = expressive_light_onSecondaryContainer,
    tertiary = expressive_light_tertiary,
    onTertiary = expressive_light_onTertiary,
    tertiaryContainer = expressive_light_tertiaryContainer,
    onTertiaryContainer = expressive_light_onTertiaryContainer,
    error = expressive_light_error,
    onError = expressive_light_onError,
    background = expressive_light_background,
    onBackground = expressive_light_onBackground,
    surface = expressive_light_surface,
    onSurface = expressive_light_onSurface,
    outline = expressive_light_outline
)


@Composable
fun PoolProtractorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce the expressive theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}

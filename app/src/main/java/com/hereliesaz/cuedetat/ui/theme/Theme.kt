// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/theme/Theme.kt

package com.hereliesaz.cuedetat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

// --- Color Schemes ---

private val DarkColorScheme = darkColorScheme(
    primary = SulfurDust,
    onPrimary = Gray700,
    primaryContainer = Yellow500,
    onPrimaryContainer = Yellow100,
    inversePrimary = Yellow400,
    secondary = Mint200,
    onSecondary = Gray700,
    secondaryContainer = Mint600,
    onSecondaryContainer = Mint100,
    tertiary = Blue400,
    onTertiary = Gray700,
    tertiaryContainer = Blue600,
    onTertiaryContainer = Blue100,
    background = Gray700,
    onBackground = Gray100,
    surface = Gray600,
    onSurface = Gray100,
    surfaceVariant = Gray600,
    onSurfaceVariant = Gray200,
    surfaceTint = SulfurDust,
    inverseSurface = Gray100,
    inverseOnSurface = Gray700,
    error = Red400,
    onError = Gray100,
    errorContainer = Red600,
    onErrorContainer = Red100,
    outline = Gray500,
    outlineVariant = Gray400,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = Gray100,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue700,
    inversePrimary = Blue300,
    secondary = Green400,
    onSecondary = Gray100,
    secondaryContainer = Green100,
    onSecondaryContainer = Green700,
    tertiary = Purple400,
    onTertiary = Gray100,
    tertiaryContainer = Purple100,
    onTertiaryContainer = Purple700,
    background = Gray100,
    onBackground = Gray700,
    surface = Purple100,
    onSurface = Gray700,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray600,
    surfaceTint = Blue500,
    inverseSurface = Gray700,
    inverseOnSurface = Gray100,
    error = Red400,
    onError = Gray100,
    errorContainer = Red100,
    onErrorContainer = Red700,
    outline = Gray300,
    outlineVariant = Gray400,
    scrim = Color.Black
)

/**
 * Utility function to adjust the luminance (lightness) of a color by a fixed factor.
 * Used for the "Glow Stick" or dimmer effects.
 *
 * @param factor Positive values lighten, negative values darken.
 */
private fun Color.adjustLuminance(factor: Float): Color {
    if (factor == 0f || this == Color.Transparent) return this
    val hsl = FloatArray(3)
    try {
        ColorUtils.colorToHSL(this.toArgb(), hsl)
        // Clamp lightness between 0.0 and 1.0.
        hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    } catch (e: IllegalArgumentException) {
        return this
    }
}

/**
 * Main Theme Composable.
 *
 * Handles:
 * - Dynamic Color (Material You) on Android 12+.
 * - Light/Dark mode switching.
 * - Global luminance adjustment (dimming).
 * - Status bar styling.
 *
 * @param darkTheme Whether to use dark mode.
 * @param dynamicColor Whether to use dynamic system colors.
 * @param luminanceAdjustment Global brightness offset (-0.4 to 0.4).
 * @param content The composable content to wrap.
 */
@Composable
fun CueDetatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    luminanceAdjustment: Float = 0f,
    content: @Composable () -> Unit
) {
    // Select base scheme.
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply luminance adjustment if needed.
    val colorScheme = if (luminanceAdjustment != 0f) {
        baseColorScheme.copy(
            background = baseColorScheme.background.adjustLuminance(luminanceAdjustment),
            surface = baseColorScheme.surface.adjustLuminance(luminanceAdjustment),
            onBackground = baseColorScheme.onBackground.adjustLuminance(luminanceAdjustment),
            onSurface = baseColorScheme.onSurface.adjustLuminance(luminanceAdjustment),
            outline = baseColorScheme.outline.adjustLuminance(luminanceAdjustment)
        )
    } else {
        baseColorScheme
    }

    // Update System Status Bar.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Provide MaterialTheme to children.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

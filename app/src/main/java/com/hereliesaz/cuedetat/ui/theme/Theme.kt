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
import androidx.core.graphics.ColorUtils // For HSL manipulation, ensure this dependency is in build.gradle
import androidx.core.view.WindowCompat

// Define your base light and dark color schemes for the APP UI (menus, sliders, etc.)
// These will NOT be affected by the luminance slider or the light/dark toggle for drawn elements.

// Helper function to adjust luminance of a color (used by PaintCache, but good to have accessible)
fun Color.adjustLuminanceHelper(factor: Float): Color {
    if (factor == 0f || this == Color.Transparent) return this
    val hsl = FloatArray(3)
    try {
        ColorUtils.colorToHSL(this.toArgb(), hsl)
        hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f) // Adjust L component (luminance)
        return Color(ColorUtils.HSLToColor(hsl))
    } catch (e: IllegalArgumentException) {
        // Happens if color is transparent or can't be converted
        return this // Return original if conversion fails
    }
}


val AppControlDarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = AcidPatina,
    tertiary = MutedGray,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E), // For cards, dialogs
    surfaceVariant = Color(0xFF2C2C2E), // For drawer background, etc.
    onPrimary = Color.Black,
    onSecondary = Color.Black, // Assuming AcidPatina is light enough
    onTertiary = Color.White,  // MutedGray is mid, White text
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFCFCFCF),
    outline = MutedGray.copy(alpha = 0.5f),
    primaryContainer = DarkerAccentGold.copy(alpha = 0.8f),
    onPrimaryContainer = Color.White,
    secondaryContainer = AcidPatina.copy(alpha = 0.3f),
    onSecondaryContainer = Color.White, // Ensure good contrast
    tertiaryContainer = MutedGray.copy(alpha = 0.3f),
    onTertiaryContainer = Color.White, // Ensure good contrast
    error = WarningRed,
    onError = Color.White,
    errorContainer = WarningRed.copy(alpha = 0.2f),
    onErrorContainer = Color.White,
    scrim = Color.Black.copy(alpha = 0.7f) // Darker scrim for better dialog/drawer separation
)

val AppControlLightColorScheme = lightColorScheme(
    primary = DarkerAccentGold,
    secondary = RustedEmber,
    tertiary = OilSlick,
    background = Color(0xFFFDFCFD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECECEC), // Slightly darker light variant for drawer
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF3A3A3A), // Darker text on light surface variant
    outline = MutedGray.copy(alpha = 0.7f),
    primaryContainer = AccentGold.copy(alpha = 0.9f),
    onPrimaryContainer = Color.Black,
    secondaryContainer = RustedEmber.copy(alpha = 0.2f),
    onSecondaryContainer = Color.Black,
    tertiaryContainer = OilSlick.copy(alpha = 0.2f),
    onTertiaryContainer = Color.Black,
    error = WarningRed,
    onError = Color.White,
    errorContainer = WarningRed.copy(alpha = 0.15f),
    onErrorContainer = Color.Black,
    scrim = Color.Black.copy(alpha = 0.4f) // Lighter scrim for light theme
)

@Composable
fun CueDetatTheme(
    // App's UI controls will use system settings by default for dark/light mode
    useSystemSettings: Boolean = true,
    darkThemeUserOverride: Boolean? = null, // Allows direct override for testing or future app setting
    content: @Composable () -> Unit
) {
    val systemIsCurrentlyDark = isSystemInDarkTheme()

    val useDarkUIColorScheme =
        darkThemeUserOverride ?: (if (useSystemSettings) systemIsCurrentlyDark else true)
    // Default to dark if not using system and no override

    val colorSchemeForUIControls = if (useDarkUIColorScheme) {
        AppControlDarkColorScheme
    } else {
        AppControlLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !useDarkUIColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorSchemeForUIControls,
        typography = Typography, // Assumes Typography is defined in Type.kt
        shapes = Shapes,       // Assumes Shapes is defined in Shape.kt
        content = content
    )
}
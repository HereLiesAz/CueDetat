// FILE: app\src\main\java\com\hereliesaz\cuedetat\ui\theme\Theme.kt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// DarkColorScheme is not actively used due to MyApplication forcing light mode,
// but defined for completeness or future changes.
private val DarkColorScheme = darkColorScheme(
    primary = AppYellow,
    onPrimary = AppBlack,
    primaryContainer = AppDarkGray,
    onPrimaryContainer = AppYellow,
    secondary = AppWhite, // A bright secondary for dark theme
    onSecondary = AppBlack,
    secondaryContainer = Color(0xFF4A4A4A), // Darker gray for containers
    onSecondaryContainer = AppYellow,
    tertiary = AppMediumGray,
    onTertiary = AppWhite,
    background = AppBlack,
    onBackground = AppYellow,
    surface = AppBlack,
    onSurface = AppYellow,
    surfaceVariant = Color(0xFF2A2A2A), // Even darker variant for surfaces
    onSurfaceVariant = AppLightGrayTextOnDark,
    outline = AppMediumGray,
    error = AppErrorRed,
    onError = AppBlack
)

private val LightColorScheme = lightColorScheme(
    primary = AppBlack,                 // Main interactive element, text on yellow
    onPrimary = AppYellow,              // Text/icons on black primary element
    primaryContainer = AppDarkGray,     // Containers for primary actions (e.g., Reset FAB)
    onPrimaryContainer = AppYellow,     // Icons/text on primaryContainer

    secondary = AppBlack,               // Accent element, could also be a less dominant color
    onSecondary = AppYellow,            // Text/icons on secondary element
    secondaryContainer = AppDarkGray,   // Containers for secondary actions (e.g., Help, Zoom FABs)
    onSecondaryContainer = AppYellow,   // Icons/text on secondaryContainer

    tertiary = AppMediumGray,           // Less emphasized element
    onTertiary = AppWhite,              // Text/icons on tertiary (gray)
    tertiaryContainer = Color(0xFFE0E0E0), // Light gray container for tertiary if needed
    onTertiaryContainer = AppBlack,

    background = AppYellow,             // Main app background
    onBackground = AppBlack,            // Text on yellow background

    surface = AppYellow,                // Surfaces like cards, dialogs (can be slightly different)
    onSurface = AppBlack,               // Text on yellow surfaces
    surfaceVariant = AppDarkYellow,     // For element like inactive slider track on yellow background
    onSurfaceVariant = AppBlack,        // Text on surfaceVariant

    outline = AppBlack,                 // Outlines for components

    error = AppErrorRed,                // Error color
    onError = AppBlack,                 // Text/icons on error color
    errorContainer = Color(0xFFFFCDD2), // Light red for error container background
    onErrorContainer = AppBlack         // Text on error container
)


@Composable
fun PoolProtractorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Still here, but MyApplication overrides
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our theme
    content: @Composable () -> Unit
) {
    // MyApplication forces Light Theme (AppCompatDelegate.MODE_NIGHT_NO)
    // So, we will primarily use LightColorScheme.
    // The darkTheme parameter from isSystemInDarkTheme() will be false.
    val effectiveDarkTheme = false // Hardcode or ensure this reflects MyApplication's choice

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // This path should ideally not be taken if dynamicColor is false or due to MyApplication
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkColorScheme // Fallback if dynamic is off or unsupported
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
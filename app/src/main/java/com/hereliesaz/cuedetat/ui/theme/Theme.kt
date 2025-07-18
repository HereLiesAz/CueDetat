// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/theme/Theme.kt

package com.hereliesaz.cuedetat.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// CORRECTED: The theme has been reverted to its original, stable state.
private val DarkColorScheme = darkColorScheme(
    primary = OracleBlue,
    secondary = AcidPatina,
    tertiary = RebelYellow,
    background = Color.Black,
    surface = RogueUmber,
    onPrimary = IcedOpal,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = IcedOpal,
    onSurface = IcedOpal
)

@Composable
fun CueDetatTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // The status bar color is now correctly derived from the theme.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
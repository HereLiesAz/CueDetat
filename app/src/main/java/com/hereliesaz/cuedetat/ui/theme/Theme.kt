// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/theme/Theme.kt
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

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = AcidPatina,
    tertiary = MutedGray,
    background = Color(0xFF121212),
    surface = SteelAsh,
    onPrimary = RogueUmber,
    onSecondary = RogueUmber,
    onTertiary = RogueUmber,
    onBackground = ColdAsh,
    onSurface = ColdAsh,
    surfaceVariant = SteelAsh,
    onSurfaceVariant = GunmetalFog,
    outline = MutedGray.copy(alpha = 0.5f),
    error = WarningRed,
    scrim = SmokeyGrunge.copy(alpha = 0.8f)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkerAccentGold,
    secondary = RustedEmber,
    tertiary = OilSlick,
    background = StaticClay,
    surface = IcedOpal,
    onPrimary = IcedOpal,
    onSecondary = IcedOpal,
    onTertiary = IcedOpal,
    onBackground = ScorchedUmber,
    onSurface = ScorchedUmber,
    surfaceVariant = IcedOpal,
    onSurfaceVariant = OilSlick,
    outline = MutedGray.copy(alpha = 0.7f),
    error = WarningRed,
    scrim = StaticClay.copy(alpha = 0.8f)
)

@Composable
fun CueDetatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
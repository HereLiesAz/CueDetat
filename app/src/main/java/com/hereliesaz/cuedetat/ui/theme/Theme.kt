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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = DarkCharcoal,
    primaryContainer = MutedGray,
    onPrimaryContainer = DarkCharcoal,
    inversePrimary = SteelAsh,
    secondary = RogueUmber,
    onSecondary = IcedOpal,
    secondaryContainer = RogueUmber,
    onSecondaryContainer = IcedOpal,
    tertiary = ColdAsh,
    onTertiary = IcedOpal,
    tertiaryContainer = SteelAsh,
    onTertiaryContainer = IcedOpal,
    background = GunmetalFog,
    onBackground = MutedGray,
    surface = DarkCharcoal,
    onSurface = IcedOpal,
    surfaceVariant = SmokeyGrunge,
    onSurfaceVariant = MutedGray,
    surfaceTint = DarkCharcoal,
    inverseSurface = IcedOpal,
    inverseOnSurface = DarkCharcoal,
    error = RustedEmber,
    onError = IcedOpal,
    errorContainer = OilSlick,
    onErrorContainer = RustedEmber,
    outline = StaticClay,
    outlineVariant = IcedOpal,
    scrim = OilSlick,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkerAccentGold,
    onPrimary = IcedOpal,
    primaryContainer = RustedEmber,
    onPrimaryContainer = IcedOpal,
    inversePrimary = OilSlick,
    secondary = StaticClay,
    onSecondary = IcedOpal,
    secondaryContainer = IcedOpal,
    onSecondaryContainer = StaticClay,
    tertiary = IcedOpal,
    onTertiary = ScorchedUmber,
    tertiaryContainer = ScorchedUmber,
    onTertiaryContainer = IcedOpal,
    background = IcedOpal,
    onBackground = OilSlick,
    surface = IcedOpal,
    onSurface = OilSlick,
    surfaceVariant = StaticClay,
    onSurfaceVariant = IcedOpal,
    surfaceTint = IcedOpal,
    inverseSurface = OilSlick,
    inverseOnSurface = IcedOpal,
    error = RustedEmber,
    onError = IcedOpal,
    errorContainer = MutedGray,
    onErrorContainer = RustedEmber,
    outline = StaticClay,
    outlineVariant = OilSlick,
    scrim = OilSlick,
)

@Composable
fun CueDetatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
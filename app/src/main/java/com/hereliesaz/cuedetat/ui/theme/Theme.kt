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
    primary = AcidPatina,
    onPrimary = Color.Black,
    primaryContainer = OxidizedBrass,
    onPrimaryContainer = Color.Black,
    secondary = RustedEmber,
    onSecondary = Color.White,
    secondaryContainer = TacticalRust,
    onSecondaryContainer = Color.White,
    tertiary = TargetGold,
    onTertiary = Color.Black,
    tertiaryContainer = MellowYellow,
    onTertiaryContainer = Color.Black,
    background = SmokeyGrunge,
    onBackground = ColdAsh,
    surface = OilSlick,
    onSurface = ColdAsh,
    surfaceVariant = SteelAsh,
    onSurfaceVariant = AshlineFog,
    error = WarningRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CharredOlive,
    onPrimary = Color.White,
    primaryContainer = AcidPatina,
    onPrimaryContainer = Color.Black,
    secondary = PyrrhicRust,
    onSecondary = Color.White,
    secondaryContainer = RustedEmber,
    onSecondaryContainer = Color.White,
    tertiary = BurntBrass,
    onTertiary = Color.White,
    tertiaryContainer = TargetGold,
    onTertiaryContainer = Color.Black,
    background = StaticClay,
    onBackground = Color.Black,
    surface = IcedOpal,
    onSurface = Color.Black,
    surfaceVariant = GunmetalFog,
    onSurfaceVariant = Color.White,
    error = WarningRed,
    onError = Color.White
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

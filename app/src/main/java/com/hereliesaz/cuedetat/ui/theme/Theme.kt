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

private val DarkColorScheme = darkColorScheme(
    primary = SulfurDust,
    secondary = Mint200,
    tertiary = Blue400,
    background = Gray700,
    surface = Gray600,
    onPrimary = Gray700,
    onSecondary = Gray700,
    onTertiary = Gray700,
    onBackground = Gray100,
    onSurface = Gray100,
    surfaceVariant = Gray600,
    onSurfaceVariant = Gray200,
    error = Red400,
    onError = Gray100,
    outline = Gray500,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    secondary = Green400,
    tertiary = Purple400,
    background = Gray100,
    surface = Purple100,
    onPrimary = Gray100,
    onSecondary = Gray100,
    onTertiary = Gray100,
    onBackground = Gray700,
    onSurface = Gray700,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray600,
    error = Red400,
    onError = Gray100,
    outline = Gray300,
)

private fun Color.adjustLuminance(factor: Float): Color {
    if (factor == 0f || this == Color.Transparent) return this
    val hsl = FloatArray(3)
    try {
        ColorUtils.colorToHSL(this.toArgb(), hsl)
        hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    } catch (e: IllegalArgumentException) {
        return this
    }
}


@Composable
fun CueDetatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    luminanceAdjustment: Float = 0f,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
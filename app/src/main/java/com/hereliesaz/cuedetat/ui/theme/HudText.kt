package com.hereliesaz.cuedetat.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

/**
 * Text drawn straight over the live camera feed (the readouts, the rotation label, the prompts).
 * The feed can be any brightness, so these are white with a dark halo rather than the theme's
 * `onSurface`, which is near-black in the light scheme and vanished against a dark felt.
 */
val HudTextShadow = Shadow(color = Color.Black.copy(alpha = 0.8f), offset = Offset(0f, 1f), blurRadius = 6f)

/** [this] as HUD text: white at [alpha] with the halo. */
fun TextStyle.asHud(alpha: Float = 1f): TextStyle =
    copy(color = Color.White.copy(alpha = alpha), shadow = HudTextShadow)

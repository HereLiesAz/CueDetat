package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import com.hereliesaz.cuedetat.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shown in the bottom-start corner when AR mode is active and a table scan exists.
 * Pulses to indicate the tracking pipeline is running.
 *
 * @param hasDepth  True when ARCore Depth API is providing real depth data.
 * @param distanceMeters  Table distance from the most recent depth plane, or null.
 */
@Composable
fun ArTrackingBadge(
    modifier: Modifier = Modifier,
    hasDepth: Boolean = false,
    distanceMeters: Float? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_pulse")
    // Held as State and read only inside drawBehind: the pulse redraws the dot each frame
    // without recomposing the badge (and rebuilding its label) 60 times a second.
    val dotAlpha = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .drawBehind { drawCircle(color = TRACKING_GREEN.copy(alpha = dotAlpha.value)) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        val label = when {
            !hasDepth -> stringResource(R.string.ar_badge_tracking)
            distanceMeters != null -> stringResource(R.string.ar_badge_depth_distance, distanceMeters)
            else -> stringResource(R.string.ar_badge_depth)
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private val TRACKING_GREEN = Color(0xFF4CAF50)

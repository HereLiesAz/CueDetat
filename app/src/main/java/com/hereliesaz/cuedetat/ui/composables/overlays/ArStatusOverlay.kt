package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.domain.ArSetupStep
import com.hereliesaz.cuedetat.domain.TableScanModel

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
    val dotAlpha by infiniteTransition.animateFloat(
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
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = dotAlpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        val label = buildString {
            append("AR Tracking")
            if (hasDepth) {
                append(" · Depth")
                if (distanceMeters != null) append(" %.1fm".format(distanceMeters))
            }
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Shown as a centered overlay during AR_SETUP mode.
 * Guides the user through the three setup steps with live progress.
 */
@Composable
fun ArSetupPrompt(
    visible: Boolean,
    lockedHsvColor: FloatArray?,
    tableScanModel: TableScanModel?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aligning Table", color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Searching for the felt...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


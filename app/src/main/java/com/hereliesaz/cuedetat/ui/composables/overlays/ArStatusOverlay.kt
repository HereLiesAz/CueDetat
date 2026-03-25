package com.hereliesaz.cuedetat.ui.composables.overlays

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
    val arSetupStep = when {
        lockedHsvColor == null -> ArSetupStep.PICK_COLOR
        tableScanModel == null -> ArSetupStep.SCAN_TABLE
        else -> ArSetupStep.VERIFY
    }

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
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AR Setup", color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                WizardStep(
                    number = "1",
                    text = "Lock felt color — tap the table surface",
                    state = if (lockedHsvColor != null) WizardStepState.DONE else WizardStepState.ACTIVE
                )
                Spacer(modifier = Modifier.height(8.dp))
                WizardStep(
                    number = "2",
                    text = "Point camera at the table",
                    state = when {
                        tableScanModel != null -> WizardStepState.DONE
                        lockedHsvColor != null -> WizardStepState.ACTIVE
                        else -> WizardStepState.PENDING
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                WizardStep(
                    number = "3",
                    text = "Verifying alignment…",
                    state = when {
                        arSetupStep == ArSetupStep.VERIFY -> WizardStepState.ACTIVE
                        else -> WizardStepState.PENDING
                    }
                )
            }
        }
    }
}

private enum class WizardStepState { PENDING, ACTIVE, DONE }

@Composable
private fun WizardStep(number: String, text: String, state: WizardStepState) {
    val (bgColor, textColor, numberText) = when (state) {
        WizardStepState.DONE    -> Triple(Color(0xFF1B5E20), Color(0xFFA5D6A7), "✓")
        WizardStepState.ACTIVE  -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), Color.White, number)
        WizardStepState.PENDING -> Triple(Color(0xFF333333), Color(0xFF777777), number)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(numberText, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = if (state == WizardStepState.PENDING) Color(0xFF777777) else Color.White,
            fontSize = 13.sp,
            textDecoration = if (state == WizardStepState.DONE) TextDecoration.LineThrough else null,
            lineHeight = 18.sp
        )
    }
}

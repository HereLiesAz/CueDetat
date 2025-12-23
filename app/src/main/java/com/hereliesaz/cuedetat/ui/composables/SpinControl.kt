package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SpinControl(
    modifier: Modifier = Modifier,
    onSpinChanged: (Offset) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val controlSize = 100.dp
    val thumbSize = 20.dp

    Box(
        modifier = modifier
            .size(controlSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onSpinChanged(Offset.Zero)
                    }
                ) { change, _ ->
                    val sizePx = size.width.toFloat()
                    val radius = sizePx / 2f
                    val dragPosition = change.position - Offset(radius, radius)
                    val distance = sqrt(dragPosition.x * dragPosition.x + dragPosition.y * dragPosition.y)

                    val clampedDistance = min(distance, radius)
                    val angle = atan2(dragPosition.y, dragPosition.x)

                    thumbOffset = Offset(
                        x = clampedDistance * cos(angle),
                        y = clampedDistance * sin(angle)
                    )

                    // Normalize the offset to a [-1, 1] range for the ViewModel
                    val normalizedOffset = Offset(
                        x = thumbOffset.x / radius,
                        y = thumbOffset.y / radius
                    )
                    onSpinChanged(normalizedOffset)
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/SpinControl.kt
package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.theme.spinPathColors
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SpinControl(
    modifier: Modifier = Modifier,
    centerPosition: PointF,
    selectedSpinOffset: Offset?,
    lingeringSpinOffset: Offset?,
    spinPathAlpha: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    val radius = 60.dp

    Canvas(modifier = modifier
        .size(radius * 2)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { },
                onDragEnd = {
                    val finalOffset = selectedSpinOffset ?: Offset.Zero
                    onEvent(MainScreenEvent.SpinDragEnd(finalOffset))
                }
            ) { change, dragAmount ->
                change.consume()
                val currentOffset = selectedSpinOffset ?: Offset.Zero
                val newOffset = currentOffset + dragAmount
                onEvent(MainScreenEvent.SpinDrag(newOffset))
            }
        }
    ) {
        val circleRadiusPx = radius.toPx()
        val center = Offset(circleRadiusPx, circleRadiusPx)

        for (i in spinPathColors.indices) {
            val startAngle = (i.toFloat() / spinPathColors.size) * 360f
            val sweepAngle = 360f / spinPathColors.size
            drawArc(
                color = spinPathColors[i],
                startAngle = startAngle - 90,
                sweepAngle = sweepAngle,
                useCenter = true,
                alpha = 0.5f
            )
        }

        drawCircle(
            color = Color.White,
            radius = circleRadiusPx,
            style = Stroke(width = 2.dp.toPx())
        )

        val finalOffset = selectedSpinOffset ?: lingeringSpinOffset
        val alpha = if (selectedSpinOffset != null) 1f else spinPathAlpha

        if (finalOffset != null) {
            val angle = atan2(finalOffset.y, finalOffset.x)
            val indicatorRadius = circleRadiusPx * 0.8f
            val indicatorPosition = Offset(
                center.x + indicatorRadius * cos(angle),
                center.y + indicatorRadius * sin(angle)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = 10.dp.toPx(),
                center = indicatorPosition
            )
        }
    }
}
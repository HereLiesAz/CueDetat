// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/MasseControl.kt

package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlin.math.atan2

@Composable
fun MasseControl(
    modifier: Modifier = Modifier,
    elevationAngle: Float, // 0 to 90 degrees
    impactOffset: PointF?,
    onElevationChanged: (Float) -> Unit,
    onImpactChanged: (PointF) -> Unit,
    onImpactEnded: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = "Top", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Impact Color Wheel
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onImpactChanged(PointF(offset.x, offset.y))
                        },
                        onDragEnd = { onImpactEnded() },
                        onDragCancel = { onImpactEnded() }
                    ) { change, _ ->
                        onImpactChanged(PointF(change.position.x, change.position.y))
                        change.consume()
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(radius, radius)

            val numArcs = 72
            val arcAngle = 360f / numArcs
            for (i in 0 until numArcs) {
                val startAngle = i * arcAngle
                val colorSampleAngle = startAngle + (arcAngle / 2)
                val color = SpinColorUtils.getColorFromAngleAndDistance(colorSampleAngle, 1.0f)

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = arcAngle,
                    useCenter = true
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            impactOffset?.let {
                drawCircle(
                    color = Color.Black,
                    radius = 6.dp.toPx(),
                    center = Offset(it.x, it.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(it.x, it.y),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Side-view Pool Stick for Elevation
        Canvas(
            modifier = Modifier
                .size(width = 200.dp, height = 150.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val tipX = 20.dp.toPx()
                        val tipY = size.height - 20.dp.toPx()

                        val dx = change.position.x - tipX
                        val dy = tipY - change.position.y // Invert Y so upward drag is positive angle

                        if (dx > 0) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            angle = angle.coerceIn(0f, 90f)
                            onElevationChanged(angle)
                        }
                        change.consume()
                    }
                }
        ) {
            val tipX = 20.dp.toPx()
            val tipY = size.height - 20.dp.toPx()
            val stickLength = 160.dp.toPx()

            // Draw a baseline for reference (the table surface)
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(tipX - 20f, tipY),
                end = Offset(tipX + stickLength, tipY),
                strokeWidth = 2.dp.toPx()
            )

            // Draw the cue stick
            withTransform({
                translate(left = tipX, top = tipY)
                rotate(degrees = -elevationAngle, pivot = Offset.Zero) // Negative visual rotation points it up
            }) {
                val stickPath = Path().apply {
                    moveTo(0f, -2f) // Tip top
                    lineTo(stickLength, -6f) // Butt top
                    lineTo(stickLength, 6f) // Butt bottom
                    lineTo(0f, 2f) // Tip bottom
                    close()
                }
                drawPath(path = stickPath, color = Color.White)

                // Ferrule/Tip detail
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(0f, -2f),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4f)
                )
            }

            // Draw pivot point indicator
            drawCircle(
                color = WarningRed,
                radius = 4.dp.toPx(),
                center = Offset(tipX, tipY)
            )
        }
    }
}
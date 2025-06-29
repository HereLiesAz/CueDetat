package com.hereliesaz.cuedetat.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Jetpack Compose implementation of a protractor overlay.
 * This replaces the legacy ProtractorOverlayView with a modern, declarative component.
 * It is responsible for drawing protractor-like guides on the screen.
 *
 * @param modifier Modifier for layout.
 * @param angleDegrees The angle to display on the protractor.
 * @param center The screen coordinate for the center of the protractor.
 * @param color The color of the protractor lines and text.
 */
@Composable
fun ProtractorOverlay(
    modifier: Modifier = Modifier,
    angleDegrees: Float,
    center: Offset,
    color: Color = Color.Yellow
) {
    // The Canvas composable is the modern, correct way to perform custom 2D drawing.
    Canvas(modifier = modifier) {
        val radius = 120.dp.toPx()
        val angleRadians = Math.toRadians(angleDegrees.toDouble() - 90) // Adjust for canvas coordinates

        // Calculate the endpoint for the line indicating the angle
        val lineEnd = Offset(
            x = center.x + radius * cos(angleRadians).toFloat(),
            y = center.y + radius * sin(angleRadians).toFloat()
        )

        // Draw the main line
        drawLine(
            color = color,
            start = center,
            end = lineEnd,
            strokeWidth = 2.dp.toPx()
        )

        // Draw tick marks for a more protractor-like feel
        val ticks = mutableListOf<Offset>()
        for (i in 0..180 step 10) {
            val tickAngleRad = Math.toRadians(i.toDouble() - 180) // Draw a semicircle
            val tickStartRadius = radius * 0.95f
            val tickEndRadius = radius * 1.05f
            ticks.add(
                Offset(
                    x = center.x + tickStartRadius * cos(tickAngleRad).toFloat(),
                    y = center.y + tickStartRadius * sin(tickAngleRad).toFloat()
                )
            )
            ticks.add(
                Offset(
                    x = center.x + tickEndRadius * cos(tickAngleRad).toFloat(),
                    y = center.y + tickEndRadius * sin(tickAngleRad).toFloat()
                )
            )
        }
        drawPoints(
            points = ticks,
            pointMode = PointMode.Lines,
            color = color.copy(alpha = 0.7f),
            strokeWidth = 1.5.dp.toPx()
        )

        // Draw the angle text using the underlying native canvas for more text control
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                this.color = color.hashCode()
                textSize = 18.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText(
                "${angleDegrees.toInt()}°",
                lineEnd.x,
                lineEnd.y + 24.dp.toPx(), // Position text below the line end
                textPaint
            )
        }
    }
}
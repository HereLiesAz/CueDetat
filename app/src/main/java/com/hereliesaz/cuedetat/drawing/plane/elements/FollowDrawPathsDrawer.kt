package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Paint
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppPaints
import kotlin.math.cos
import kotlin.math.sin

class FollowDrawPathsDrawer {

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        basePathLength: Float,          // Base length for these paths
        tangentLineAngleRad: Double    // Angle of the Tangent Line (solid deflection line)
    ) {
        if (!appState.isInitialized || basePathLength <= 0.01f) {
            return
        }

        val cueCenterX = appState.cueCircleCenter.x
        val cueCenterY = appState.cueCircleCenter.y

        val pathDrawActualLength = basePathLength * config.PATH_DRAW_LENGTH_FACTOR

        // --- Follow Path ---
        // Endpoint deviates from tangent by FOLLOW_EFFECT_DEVIATION_DEGREES
        val followDeviationRad = Math.toRadians(config.FOLLOW_EFFECT_DEVIATION_DEGREES.toDouble())
        val followEndAngle = tangentLineAngleRad + followDeviationRad // Apply deviation
        drawSingleCurve(
            canvas, cueCenterX, cueCenterY, pathDrawActualLength,
            tangentLineAngleRad,    // Starts tangent to this
            followEndAngle,         // Curves towards this end angle
            config.CURVE_CONTROL_POINT_FACTOR, // Use correct config constant
            appPaints.followPathPaint          // Use correct paint name
        )

        // --- Draw Path ---
        // Endpoint deviates from tangent by DRAW_EFFECT_DEVIATION_DEGREES
        val drawDeviationRad = Math.toRadians(config.DRAW_EFFECT_DEVIATION_DEGREES.toDouble())
        val drawEndAngle = tangentLineAngleRad + drawDeviationRad // Apply deviation
        drawSingleCurve(
            canvas, cueCenterX, cueCenterY, pathDrawActualLength,
            tangentLineAngleRad,    // Starts tangent to this
            drawEndAngle,           // Curves towards this end angle
            config.CURVE_CONTROL_POINT_FACTOR, // Use correct config constant
            appPaints.drawPathPaint            // Use correct paint name
        )
    }

    private fun drawSingleCurve(
        canvas: Canvas,
        startX: Float, startY: Float,           // P0
        pathLength: Float,
        startTangentAngleRad: Double,           // Initial tangent at P0
        endTargetAngleRad: Double,              // Angle the curve should be pointing at P2
        controlPointFactor: Float,              // Received from config
        paint: Paint
    ) {
        val path = Path()
        path.moveTo(startX, startY) // P0

        val endX = startX + (pathLength * cos(endTargetAngleRad)).toFloat() // P2.x
        val endY = startY + (pathLength * sin(endTargetAngleRad)).toFloat() // P2.y

        // P1: Control Point. Placed along the startTangentAngleRad.
        val controlDist = pathLength * controlPointFactor
        val controlX = startX + (controlDist * cos(startTangentAngleRad)).toFloat()
        val controlY = startY + (controlDist * sin(startTangentAngleRad)).toFloat()

        path.quadTo(controlX, controlY, endX, endY)
        canvas.drawPath(path, paint)
    }
}
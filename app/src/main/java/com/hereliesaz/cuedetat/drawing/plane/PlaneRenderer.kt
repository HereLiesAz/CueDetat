package com.hereliesaz.cuedetat.drawing.plane

import android.graphics.Canvas
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords
import com.hereliesaz.cuedetat.geometry.models.DeflectionLineParams
import com.hereliesaz.cuedetat.drawing.plane.elements.* // Imports all element drawers
import com.hereliesaz.cuedetat.drawing.plane.labels.*   // Imports all label drawers
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import kotlin.math.atan2
import kotlin.math.abs

class PlaneRenderer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val cueCircleDrawer = CueCircleDrawer()
    private val targetCircleDrawer = TargetCircleDrawer()
    private val protractorAnglesDrawer = ProtractorAnglesDrawer(viewWidthProvider, viewHeightProvider)
    private val deflectionLinesDrawer = DeflectionLinesDrawer()
    private val shotGuideLineDrawer = ShotGuideLineDrawer()
    private val followDrawPathsDrawer = FollowDrawPathsDrawer() // Using the correct drawer name

    private val projectedShotTextDrawer = ProjectedShotTextDrawer(textLayoutHelper)
    private val tangentLineTextDrawer = TangentLineTextDrawer(textLayoutHelper) // Label for the Tangent Line itself
    private val cueBallPathTextDrawer = CueBallPathTextDrawer(textLayoutHelper) // Label for the other (dotted) deflection line
    private val pocketAimTextDrawer = PocketAimTextDrawer(textLayoutHelper)


    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        aimingLineCoords: AimingLineLogicalCoords,
        deflectionParams: DeflectionLineParams,
        useErrorColor: Boolean
    ) {
        targetCircleDrawer.draw(canvas, appState, appPaints)
        cueCircleDrawer.draw(canvas, appState, appPaints, useErrorColor)
        protractorAnglesDrawer.draw(canvas, appState, appPaints, config)
        shotGuideLineDrawer.draw(canvas, appPaints, aimingLineCoords)

        val protractorAngle = appState.protractorRotationAngle
        val dxCueToTarget = appState.targetCircleCenter.x - appState.cueCircleCenter.x
        val dyCueToTarget = appState.targetCircleCenter.y - appState.cueCircleCenter.y
        val angleCueToTargetRad = atan2(dyCueToTarget.toDouble(), dxCueToTarget.toDouble())

        var angleToTargetDeg = Math.toDegrees(angleCueToTargetRad).toFloat()
        if (angleToTargetDeg < 0) angleToTargetDeg += 360f
        var effectiveProtractorAimDeg = (protractorAngle + 180f) % 360f
        if (effectiveProtractorAimDeg < 0) effectiveProtractorAimDeg += 360f
        var angleDiff = abs(effectiveProtractorAimDeg - angleToTargetDeg)
        if (angleDiff > 180) angleDiff = 360 - angleDiff
        val isStraightShot = angleDiff < 1.5f

        val showDeflectionAndFollowDrawLines = !isStraightShot &&
                deflectionParams.cueToTargetDistance > 0.1f &&
                !useErrorColor

        if (showDeflectionAndFollowDrawLines) {
            // This draws the main solid/dotted TANGENT lines
            deflectionLinesDrawer.draw(canvas, appState, appPaints, deflectionParams, false)

            // Determine the angle of the VISUALLY SOLID deflection line (this is the Tangent Line)
            val alphaDeg = appState.protractorRotationAngle
            val epsilon = 0.5f
            val isPositiveDeflectionVectorSolid = (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) ||
                    (alphaDeg <= epsilon || alphaDeg >= 360f - epsilon)

            val tangentLineAngleRad: Double = if (isPositiveDeflectionVectorSolid) {
                atan2(deflectionParams.unitPerpendicularY.toDouble(), deflectionParams.unitPerpendicularX.toDouble())
            } else {
                atan2(-deflectionParams.unitPerpendicularY.toDouble(), -deflectionParams.unitPerpendicularX.toDouble())
            }

            val basePathLength = deflectionParams.visualDrawLength // Use this as base for scaling path length

            followDrawPathsDrawer.draw(
                canvas, appState, appPaints, config,
                basePathLength,
                tangentLineAngleRad
                // The FollowDrawPathsDrawer itself will use config values for specific deflection angles
            )
        }

        if (appState.areHelperTextsVisible) {
            canvas.save()
            val textPlaneYLift = -appState.currentLogicalRadius * 0.15f / appState.zoomFactor.coerceAtLeast(0.3f)
            canvas.translate(0f, textPlaneYLift)
            projectedShotTextDrawer.draw(canvas, appState, appPaints, config, aimingLineCoords)
            if (showDeflectionAndFollowDrawLines) { // Only show these labels if lines are visible
                tangentLineTextDrawer.draw(canvas, appState, appPaints, config, deflectionParams)
                cueBallPathTextDrawer.draw(canvas, appState, appPaints, config, deflectionParams)
            }
            pocketAimTextDrawer.draw(canvas, appState, appPaints, config)
            canvas.restore()
        }
    }
}
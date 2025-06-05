package com.hereliesaz.cuedetat.drawing.plane

import android.graphics.Canvas
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords
import com.hereliesaz.cuedetat.geometry.models.DeflectionLineParams
import com.hereliesaz.cuedetat.drawing.plane.elements.PlaneCueBallDrawer
import com.hereliesaz.cuedetat.drawing.plane.elements.PlaneTargetBallDrawer
import com.hereliesaz.cuedetat.drawing.plane.elements.ProtractorAnglesDrawer
import com.hereliesaz.cuedetat.drawing.plane.elements.DeflectionLinesDrawer
import com.hereliesaz.cuedetat.drawing.plane.elements.ShotGuideLineDrawer
import com.hereliesaz.cuedetat.drawing.plane.elements.FollowDrawPathsDrawer
import com.hereliesaz.cuedetat.drawing.plane.labels.*
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import kotlin.math.atan2
import kotlin.math.abs
import android.graphics.PointF

class PlaneRenderer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val planeCueBallDrawer = PlaneCueBallDrawer()
    private val planeTargetBallDrawer = PlaneTargetBallDrawer()
    private val protractorAnglesDrawer = ProtractorAnglesDrawer(viewWidthProvider, viewHeightProvider)
    private val deflectionLinesDrawer = DeflectionLinesDrawer()
    private val shotGuideLineDrawer = ShotGuideLineDrawer()
    private val followDrawPathsDrawer = FollowDrawPathsDrawer()

    private val projectedShotTextDrawer = ProjectedShotTextDrawer(textLayoutHelper)
    private val tangentLineTextDrawer = TangentLineTextDrawer(textLayoutHelper)
    private val cueBallPathTextDrawer = CueBallPathTextDrawer(textLayoutHelper)
    private val pocketAimTextDrawer = PocketAimTextDrawer(textLayoutHelper)


    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        aimingLineCoords: AimingLineLogicalCoords,
        deflectionParams: DeflectionLineParams,
        useErrorColor: Boolean,
        actualCueBallScreenCenter: PointF? // This is the screen position of the actual cue ball (unadjusted Y)
    ) {
        // The protractor plane visuals (circles, lines, etc.) are now always drawn
        // if appState is initialized and balls are selected (or manually placed defaults)

        // Only draw if both selected balls exist (even if manually placed)
        if (appState.selectedCueBall == null || appState.selectedTargetBall == null) return

        planeTargetBallDrawer.draw(canvas, appState, appPaints)
        planeCueBallDrawer.draw(canvas, appState, appPaints, useErrorColor)
        protractorAnglesDrawer.draw(canvas, appState, appPaints, config)
        shotGuideLineDrawer.draw(canvas, appPaints, aimingLineCoords, actualCueBallScreenCenter)

        // Deflection and Follow/Draw lines/labels are conditional on AIMING mode
        // and a non-straight shot.
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

        // Show deflection/follow/draw lines and their labels ONLY in AIMING mode AND if not a straight shot
        val showDeflectionAndFollowDrawLines = appState.currentMode == SelectionMode.AIMING &&
                !isStraightShot &&
                deflectionParams.cueToTargetDistance > 0.1f &&
                !useErrorColor

        if (showDeflectionAndFollowDrawLines) {
            deflectionLinesDrawer.draw(canvas, appState, appPaints, deflectionParams, false)

            val alphaDeg = appState.protractorRotationAngle
            val epsilon = 0.5f
            val isPositiveDeflectionVectorSolid = (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) ||
                    (alphaDeg <= epsilon || alphaDeg >= 360f - epsilon)

            val tangentLineAngleRad: Double = if (isPositiveDeflectionVectorSolid) {
                atan2(deflectionParams.unitPerpendicularY.toDouble(), deflectionParams.unitPerpendicularX.toDouble())
            } else {
                atan2(-deflectionParams.unitPerpendicularY.toDouble(), -deflectionParams.unitPerpendicularX.toDouble())
            }

            val basePathLength = deflectionParams.visualDrawLength

            followDrawPathsDrawer.draw(
                canvas, appState, appPaints, config,
                basePathLength,
                tangentLineAngleRad
            )
        }

        // Draw plane-specific helper texts only if helper texts are visible
        // And for some labels, only in AIMING mode.
        if (appState.areHelperTextsVisible) {
            canvas.save()
            // Text Y lift depends on base logical radius and zoom for consistency with plane elements
            val textPlaneYLift = -appState.logicalBallRadius * appState.zoomFactor * 0.15f / appState.zoomFactor.coerceAtLeast(0.3f)
            canvas.translate(0f, textPlaneYLift)
            projectedShotTextDrawer.draw(canvas, appState, appPaints, config, aimingLineCoords)
            if (showDeflectionAndFollowDrawLines) { // Only show these labels if lines are visible
                tangentLineTextDrawer.draw(canvas, appState, appPaints, config, deflectionParams)
                cueBallPathTextDrawer.draw(canvas, appState, appPaints, config, deflectionParams)
            }
            // Pocket aim text only visible in AIMING mode
            if (appState.currentMode == SelectionMode.AIMING) {
                pocketAimTextDrawer.draw(canvas, appState, appPaints, config)
            }
            canvas.restore()
        }
    }
}
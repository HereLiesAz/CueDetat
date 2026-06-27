package com.hereliesaz.cuedetat.ui.composables.overlays

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.aznavrail.tutorial.AzGuideShape
import com.hereliesaz.aznavrail.tutorial.AzPathCmd
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.warpedBy

/**
 * Resolves the in-camera elements that the AzNavRail 10.18 guidance framework spotlights during the
 * tutorial. Each function maps the current [CueDetatState] to an [AzGuideShape] in window-space pixels
 * (or null ⇒ text-only callout). The geometry mirrors the math that the old TutorialOverlay used for its
 * pulsing highlights; the framework now draws the dim + spotlight itself via these shapes.
 */

private fun activeMatrix(state: CueDetatState): Matrix? =
    if (state.isBeginnerViewLocked) state.logicalPlaneMatrix else state.pitchMatrix

private fun activeTps(state: CueDetatState) =
    if (state.cameraMode == CameraMode.LITE_AR || state.isBeginnerViewLocked) null else state.lensWarpTps

private fun circleAround(
    state: CueDetatState,
    logicalCenter: PointF,
    logicalRadius: Float,
    radiusScale: Float = 1f,
): AzGuideShape? {
    val matrix = activeMatrix(state) ?: return null
    val warped = logicalCenter.warpedBy(activeTps(state))
    val screen = DrawingUtils.mapPoint(warped, matrix)
    val info = DrawingUtils.getPerspectiveRadiusAndLift(warped, logicalRadius, state, matrix)
    val liftedY = if (state.isBeginnerViewLocked) screen.y else screen.y - info.lift
    return AzGuideShape.Circle(cx = screen.x, cy = liftedY, radius = info.radius * radiusScale, padding = 8f)
}

fun tutorialTargetBallShape(state: CueDetatState): AzGuideShape? =
    circleAround(state, state.protractorUnit.center, state.protractorUnit.radius)

fun tutorialGhostBallShape(state: CueDetatState): AzGuideShape? =
    circleAround(state, state.protractorUnit.ghostCueBallCenter, state.protractorUnit.radius, radiusScale = 0.15f)

fun tutorialCueBallShape(state: CueDetatState): AzGuideShape? {
    val ball = state.onPlaneBall ?: return null
    return circleAround(state, ball.center, ball.radius)
}

fun tutorialZoomSliderShape(state: CueDetatState): AzGuideShape? {
    if (state.viewWidth <= 0 || state.viewHeight <= 0) return null
    return AzGuideShape.Rect(
        left = state.viewWidth - 64f * state.screenDensity,
        top = state.viewHeight * 0.2f,
        width = 60f * state.screenDensity,
        height = state.viewHeight * 0.6f,
        padding = 8f,
    )
}

fun tutorialAimingLineShape(state: CueDetatState): AzGuideShape? {
    val matrix = activeMatrix(state) ?: return null
    val tps = activeTps(state)

    val camMat = state.cameraMatrix
    val distMat = state.distCoeffs
    val camArray = if (camMat != null && !camMat.empty())
        DoubleArray(camMat.total().toInt()).also { camMat.get(0, 0, it) } else null
    val distArray = if (distMat != null && !distMat.empty())
        DoubleArray(distMat.total().toInt()).also { distMat.get(0, 0, it) } else null
    val hasDistortion = camArray != null && distArray != null && camArray.size == 9

    val rawPath = state.aimingLineBankPath
        ?: listOf(state.protractorUnit.ghostCueBallCenter, state.protractorUnit.center)
    val logicalPath = rawPath.map { it.warpedBy(tps) }
    if (logicalPath.size < 2) return null

    val cmds = ArrayList<AzPathCmd>()
    val pt = FloatArray(2)
    val segments = 12
    var first = true
    for (i in 0 until logicalPath.size - 1) {
        val s = logicalPath[i]
        val e = logicalPath[i + 1]
        for (j in 0..segments) {
            val t = j.toFloat() / segments
            pt[0] = s.x + (e.x - s.x) * t
            pt[1] = s.y + (e.y - s.y) * t
            matrix.mapPoints(pt)
            val fx: Float
            val fy: Float
            if (hasDistortion) {
                val d = DrawingUtils.applyBarrelDistortion(pt[0], pt[1], camArray!!, distArray!!)
                fx = d.x; fy = d.y
            } else {
                fx = pt[0]; fy = pt[1]
            }
            if (first) {
                cmds.add(AzPathCmd.MoveTo(fx, fy)); first = false
            } else {
                cmds.add(AzPathCmd.LineTo(fx, fy))
            }
        }
    }
    if (cmds.isEmpty()) return null
    return AzGuideShape.Path(commands = cmds, padding = 24f)
}

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Handles all drawing operations for the protractor overlay.
 * It is a stateless renderer that draws based on the provided OverlayState and PaintCache.
 */
class OverlayRenderer {

    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val baseGhostBallTextSize = 30f
    private val minGhostBallTextSize = 15f
    private val maxGhostBallTextSize = 60f

    /**
     * The main drawing method. Orchestrates the rendering of different components.
     */
    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // --- Consolidated Warning Logic ---
        val (isCueOnFarSide, isPhysicalOverlap) = calculateWarningConditions(state)
        val isDeflectionDominantAngle =
            (state.rotationAngle > 90.5f && state.rotationAngle < 269.5f)

        // A shot is "impossible" or a "warning" if any of these conditions are true.
        val isImpossibleShot = isCueOnFarSide || isPhysicalOverlap || isDeflectionDominantAngle

        // --- Draw Protractor Plane (in pitched perspective) ---
        canvas.save()
        canvas.concat(state.pitchMatrix)

        drawAimingLine(canvas, state, paints, isImpossibleShot)
        drawBalls(canvas, state, paints, isImpossibleShot)
        drawTangentLines(canvas, state, paints, isImpossibleShot) // Pass unified flag
        drawProtractorLines(canvas, state, paints)

        canvas.restore() // Restore from pitch matrix

        // --- Draw Ghost Balls and Text (in screen space) ---
        drawGhostBalls(canvas, state, paints, isImpossibleShot)
    }

    private fun calculateWarningConditions(state: OverlayState): Pair<Boolean, Boolean> {
        val logicalDistanceBetweenCenters =
            distance(state.cueCircleCenter, state.targetCircleCenter)
        val isPhysicalOverlap = logicalDistanceBetweenCenters < (state.logicalRadius * 2) - 0.1f

        var isCueOnFarSide = false
        if (state.hasInverseMatrix) {
            val screenAimPoint = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
            val logicalAimPoint = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAimPoint, screenAimPoint)

            val aimDirX = state.cueCircleCenter.x - logicalAimPoint[0]
            val aimDirY = state.cueCircleCenter.y - logicalAimPoint[1]
            val magAimDirSq = aimDirX * aimDirX + aimDirY * aimDirY

            if (magAimDirSq > 0.0001f) {
                val magAimDir = sqrt(magAimDirSq)
                val normAimDirX = aimDirX / magAimDir
                val normAimDirY = aimDirY / magAimDir

                val vecScreenToTargetX = state.targetCircleCenter.x - logicalAimPoint[0]
                val vecScreenToTargetY = state.targetCircleCenter.y - logicalAimPoint[1]
                val distTargetProj =
                    vecScreenToTargetX * normAimDirX + vecScreenToTargetY * normAimDirY
                isCueOnFarSide = magAimDir > distTargetProj && distTargetProj > 0
            }
        }
        return Pair(isCueOnFarSide, isPhysicalOverlap)
    }

    // UPDATED: Now accepts the isImpossibleShot flag to change color.
    private fun drawAimingLine(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        isImpossibleShot: Boolean
    ) {
        if (!state.hasInverseMatrix) return

        val screenAimPoint = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
        val logicalAimPoint = FloatArray(2)
        state.inversePitchMatrix.mapPoints(logicalAimPoint, screenAimPoint)

        val sx = logicalAimPoint[0]
        val sy = logicalAimPoint[1]
        val cx = state.cueCircleCenter.x
        val cy = state.cueCircleCenter.y
        val dx = cx - sx
        val dy = cy - sy
        val mag = sqrt(dx * dx + dy * dy)

        if (mag > 0.001f) {
            val ndx = dx / mag
            val ndy = dy / mag
            val extendFactor = max(state.viewWidth, state.viewHeight) * 5f
            val ex = cx + ndx * extendFactor
            val ey = cy + ndy * extendFactor // <--- ADD THIS LINE TO CALCULATE EY

            // USER REQUEST: Use a muted red for the cue sight line on impossible shots.
            val nearPaint =
                if (isImpossibleShot) paints.warningPaintRed3 else paints.aimingAssistNearPaint
            val farPaint =
                if (isImpossibleShot) paints.warningPaintRed3 else paints.aimingAssistFarPaint

            canvas.drawLine(sx, sy, cx, cy, nearPaint)
            canvas.drawLine(cx, cy, ex, ey, farPaint)
        }
    }


    // UPDATED: Now uses the isImpossibleShot flag.
    private fun drawBalls(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        isImpossibleShot: Boolean
    ) {
        // Target ball does not change color.
        canvas.drawCircle(
            state.targetCircleCenter.x,
            state.targetCircleCenter.y,
            state.logicalRadius,
            paints.targetCirclePaint
        )
        canvas.drawCircle(
            state.targetCircleCenter.x,
            state.targetCircleCenter.y,
            state.logicalRadius / 5f,
            paints.centerMarkPaint
        )

        // USER REQUEST: Use a muted red for the 2D cue ball on impossible shots.
        val cuePaint = if (isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint

        canvas.drawCircle(
            state.cueCircleCenter.x,
            state.cueCircleCenter.y,
            state.logicalRadius,
            cuePaint
        )
        canvas.drawCircle(
            state.cueCircleCenter.x,
            state.cueCircleCenter.y,
            state.logicalRadius / 5f,
            paints.centerMarkPaint
        )
    }

    // UPDATED: Now uses the isImpossibleShot flag.
    private fun drawTangentLines(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        isImpossibleShot: Boolean
    ) {
        val dx = state.targetCircleCenter.x - state.cueCircleCenter.x
        val dy = state.targetCircleCenter.y - state.cueCircleCenter.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag < 0.001f) return

        val extend = max(state.viewWidth, state.viewHeight) * 1.5f
        val deflectionDirX = -dy / mag
        val deflectionDirY = dx / mag

        // If shot is impossible, both tangent lines are dotted. Otherwise, one is solid.
        val rightPaint =
            if (isImpossibleShot || state.rotationAngle <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
        val leftPaint =
            if (isImpossibleShot || state.rotationAngle > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint

        canvas.drawLine(
            state.cueCircleCenter.x,
            state.cueCircleCenter.y,
            state.cueCircleCenter.x + deflectionDirX * extend,
            state.cueCircleCenter.y + deflectionDirY * extend,
            rightPaint
        )
        canvas.drawLine(
            state.cueCircleCenter.x,
            state.cueCircleCenter.y,
            state.cueCircleCenter.x - deflectionDirX * extend,
            state.cueCircleCenter.y - deflectionDirY * extend,
            leftPaint
        )
    }

    private fun drawProtractorLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        canvas.save()
        canvas.translate(state.targetCircleCenter.x, state.targetCircleCenter.y)
        canvas.rotate(state.rotationAngle)
        val lineLength = max(state.viewWidth, state.viewHeight) * 2f

        PROTRACTOR_ANGLES.forEach { angle ->
            val rad = Math.toRadians(angle.toDouble())
            val endX = (lineLength * sin(rad)).toFloat()
            val endY = (lineLength * cos(rad)).toFloat()

            if (angle == 0f) {
                canvas.drawLine(0f, 0f, endX, endY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -endX, -endY, paints.shotPathLinePaint)
            } else {
                canvas.drawLine(0f, 0f, endX, endY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -endX, -endY, paints.protractorLinePaint)
                val negRad = Math.toRadians(-angle.toDouble())
                val negEndX = (lineLength * sin(negRad)).toFloat()
                val negEndY = (lineLength * cos(negRad)).toFloat()
                canvas.drawLine(0f, 0f, negEndX, negEndY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -negEndX, -negEndY, paints.protractorLinePaint)
            }
        }
        canvas.restore()
    }

    // UPDATED: Now uses the isImpossibleShot flag.
    private fun drawGhostBalls(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        isImpossibleShot: Boolean
    ) {
        val pTGC = mapPoint(state.targetCircleCenter, state.pitchMatrix)
        val pCGC = mapPoint(state.cueCircleCenter, state.pitchMatrix)

        val tR = mapPoint(
            PointF(
                state.targetCircleCenter.x + state.logicalRadius,
                state.targetCircleCenter.y
            ), state.pitchMatrix
        )
        val tT = mapPoint(
            PointF(
                state.targetCircleCenter.x,
                state.targetCircleCenter.y - state.logicalRadius
            ), state.pitchMatrix
        )
        val gTSR = max(distance(pTGC, tR), distance(pTGC, tT))

        val cR = mapPoint(
            PointF(state.cueCircleCenter.x + state.logicalRadius, state.cueCircleCenter.y),
            state.pitchMatrix
        )
        val cT = mapPoint(
            PointF(state.cueCircleCenter.x, state.cueCircleCenter.y - state.logicalRadius),
            state.pitchMatrix
        )
        val gCSR = max(distance(pCGC, cR), distance(pCGC, cT))

        val targetGhostCenterY = pTGC.y - gTSR
        val cueGhostCenterY = pCGC.y - gCSR

        canvas.drawCircle(pTGC.x, targetGhostCenterY, gTSR, paints.targetGhostBallOutlinePaint)

        // USER REQUEST: Use a muted red for the 3D cue ball on impossible shots.
        val cueGhostPaint =
            if (isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
        canvas.drawCircle(pCGC.x, cueGhostCenterY, gCSR, cueGhostPaint)

        val sightArmLength = gCSR * 0.6f
        canvas.drawLine(
            pCGC.x - sightArmLength,
            cueGhostCenterY,
            pCGC.x + sightArmLength,
            cueGhostCenterY,
            paints.aimingSightPaint
        )
        canvas.drawLine(
            pCGC.x,
            cueGhostCenterY - sightArmLength,
            pCGC.x,
            cueGhostCenterY + sightArmLength,
            paints.aimingSightPaint
        )
        canvas.drawCircle(pCGC.x, cueGhostCenterY, sightArmLength * 0.15f, paints.aimingSightPaint)

        if (state.areHelpersVisible) {
            drawGhostBallText(
                canvas,
                state,
                paints,
                pTGC.x,
                targetGhostCenterY,
                gTSR,
                "Target Ball"
            )
            drawGhostBallText(canvas, state, paints, pCGC.x, cueGhostCenterY, gCSR, "Cue Ball")
        }
    }

    private fun drawGhostBallText(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        x: Float,
        y: Float,
        radius: Float,
        text: String
    ) {
        val currentTextSize = (baseGhostBallTextSize * state.zoomFactor).coerceIn(
            minGhostBallTextSize,
            maxGhostBallTextSize
        )
        paints.ghostBallTextPaint.textSize = currentTextSize
        val textMetrics = paints.ghostBallTextPaint.fontMetrics
        val textPadding = 5f * state.zoomFactor.coerceAtLeast(0.5f)
        val visualTop = y - radius
        val baseline = visualTop - textPadding - textMetrics.descent
        canvas.drawText(text, x, baseline, paints.ghostBallTextPaint)
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

    private fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
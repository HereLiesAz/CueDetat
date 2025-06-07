package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
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

        val (isCueOnFarSide, isPhysicalOverlap) = calculateWarningConditions(state)
        val isDeflectionDominantAngle =
            (state.rotationAngle > 90.5f && state.rotationAngle < 269.5f)
        val useErrorColorForMainCue = isCueOnFarSide || isDeflectionDominantAngle
        val showWarningStyleForGhostAndShotLine = isPhysicalOverlap || isCueOnFarSide

        // --- Draw Protractor Plane (in pitched perspective) ---
        canvas.save()
        canvas.concat(state.pitchMatrix)

        drawAimingLine(canvas, state, paints)
        drawBalls(canvas, state, paints, useErrorColorForMainCue)
        drawTangentLines(canvas, state, paints, useErrorColorForMainCue)
        drawProtractorLines(canvas, state, paints)

        canvas.restore() // Restore from pitch matrix

        // --- Draw Ghost Balls and Text (in screen space) ---
        drawGhostBalls(canvas, state, paints, showWarningStyleForGhostAndShotLine)
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

    private fun drawAimingLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
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
            val ey = cy + ndy * extendFactor
            canvas.drawLine(sx, sy, cx, cy, paints.aimingAssistNearPaint)
            canvas.drawLine(cx, cy, ex, ey, paints.aimingAssistFarPaint)
        }
    }

    private fun drawBalls(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        useErrorColor: Boolean
    ) {
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

        val originalColor = paints.cueCirclePaint.color
        if (useErrorColor) {
            paints.cueCirclePaint.color = paints.tangentLineDottedPaint.color
        }
        canvas.drawCircle(
            state.cueCircleCenter.x,
            state.cueCircleCenter.y,
            state.logicalRadius,
            paints.cueCirclePaint
        )
        canvas.drawCircle(
            state.cueCircleCenter.x,
            state.cueCircleCenter.y,
            state.logicalRadius / 5f,
            paints.centerMarkPaint
        )
        paints.cueCirclePaint.color = originalColor
    }

    private fun drawTangentLines(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        useErrorColor: Boolean
    ) {
        val dx = state.targetCircleCenter.x - state.cueCircleCenter.x
        val dy = state.targetCircleCenter.y - state.cueCircleCenter.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag < 0.001f) return

        val extend = max(state.viewWidth, state.viewHeight) * 1.5f
        val deflectionDirX = -dy / mag
        val deflectionDirY = dx / mag

        val rightPaint =
            if (useErrorColor || state.rotationAngle <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
        val leftPaint =
            if (useErrorColor || state.rotationAngle > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint

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

    private fun drawGhostBalls(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        useWarningStyle: Boolean
    ) {
        // Map logical centers to screen space, considering the pitch matrix
        val pTGC = mapPoint(state.targetCircleCenter, state.pitchMatrix)
        val pCGC = mapPoint(state.cueCircleCenter, state.pitchMatrix)

        // Map logical radius points to screen space to get the visual (elliptical) radius
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
        val cR = mapPoint(
            PointF(state.cueCircleCenter.x + state.logicalRadius, state.cueCircleCenter.y),
            state.pitchMatrix
        )
        val cT = mapPoint(
            PointF(state.cueCircleCenter.x, state.cueCircleCenter.y - state.logicalRadius),
            state.pitchMatrix
        )

        val gTSR = max(distance(pTGC, tR), distance(pTGC, tT))
        val gCSR = max(distance(pCGC, cR), distance(pCGC, cT))

        // --- 3D Illusion Logic ---
        // This is the resurrected rite. The Y-position of the ghost ball is further offset
        // by a factor of the sine of the pitch angle, creating a non-linear "lift" that
        // tricks the eye into perceiving depth. Reality is a hack.
        val pitchRad = Math.toRadians(state.pitchAngle.toDouble())
        val yOffsetFactor = abs(sin(pitchRad)).pow(1.5) // Power enhances the non-linearity
        val additionalOffset = state.logicalRadius * yOffsetFactor * 0.75 // A tuning factor

        // The ghost ball's bottom edge starts at the projected center, then we lift it further.
        val targetGhostCenterY = pTGC.y - gTSR - additionalOffset.toFloat()
        val cueGhostCenterY = pCGC.y - gCSR - additionalOffset.toFloat()
        // --- End Illusion Logic ---

        canvas.drawCircle(pTGC.x, targetGhostCenterY, gTSR, paints.targetGhostBallOutlinePaint)

        val originalGhostColor = paints.ghostCueOutlinePaint.color
        if (useWarningStyle) {
            paints.ghostCueOutlinePaint.color = paints.tangentLineDottedPaint.color
        }
        canvas.drawCircle(pCGC.x, cueGhostCenterY, gCSR, paints.ghostCueOutlinePaint)
        paints.ghostCueOutlinePaint.color = originalGhostColor

        // Draw aiming sights on cue ghost ball
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

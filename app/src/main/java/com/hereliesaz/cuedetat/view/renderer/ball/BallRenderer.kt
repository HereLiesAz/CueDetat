// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.BankingBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.ObstacleBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.util.createGlowPaint
import com.hereliesaz.cuedetat.view.renderer.warpedBy
import kotlin.math.hypot

class BallRenderer {

    private val textRenderer = BallTextRenderer()

    fun draw(canvas: Canvas, state: CueDetatState, paints: PaintCache, typeface: Typeface?, labels: Map<String, String>) {
        val tps = if (state.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) null else state.lensWarpTps
        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        if (isBeginnerLocked) {
            // Replaced by granular calls in OverlayRenderer for Beginner mode
            return
        }

        // --- PASS 1: BODIES (Bases, Glows, Outlines, Detection Rings) ---
        
        // 1a. Draw Main Balls (Banking or Protractor)
        if (state.isBankingMode) {
            state.onPlaneBall?.let { ball ->
                drawBallBody(canvas, ball, BankingBall(), state, paints, tps)
            }
        } else {
            val protractor = state.protractorUnit
            drawBallBody(canvas, protractor, TargetBall(), state, paints, tps)

            if (!state.isMasseModeActive || state.masseConnectsTarget) {
                val ghostCenter = if (state.isMasseModeActive) state.masseGhostBallCenter else null
                val ghostBall = object : LogicalCircular {
                    override val center = ghostCenter ?: protractor.ghostCueBallCenter
                    override val radius = protractor.radius
                }
                drawBallBody(canvas, ghostBall, GhostCueBall(), state, paints, tps)
            }

            state.onPlaneBall?.let { ball ->
                drawBallBody(canvas, ball, ActualCueBall(), state, paints, tps)
            }
        }

        // 1b. Draw Obstacle Balls
        state.obstacleBalls.forEach { obstacle ->
            drawBallBody(canvas, obstacle, ObstacleBall(), state, paints, tps)
        }

        // 1c. Draw Detection UI (Snapped rings, passive glows, tap targets)
        drawDetectionRings(canvas, state, paints, tps)

        // 1d. (Debug) Bounding Boxes - drawn above bodies but below text/centers
        drawBoundingBoxes(canvas, state, paints)


        // --- PASS 2: LABELS (Text) ---
        drawAllLabels(canvas, state, paints, typeface, labels)


        // --- PASS 3: CENTERS (Dots, Crosshairs, Highest point) ---
        
        // 3a. Draw Main Ball Centers
        if (state.isBankingMode) {
            state.onPlaneBall?.let { ball ->
                drawBallCenter(canvas, ball, BankingBall(), state, paints, tps)
            }
        } else {
            val protractor = state.protractorUnit
            drawBallCenter(canvas, protractor, TargetBall(), state, paints, tps)

            if (!state.isMasseModeActive || state.masseConnectsTarget) {
                val ghostCenter = if (state.isMasseModeActive) state.masseGhostBallCenter else null
                val ghostBall = object : LogicalCircular {
                    override val center = ghostCenter ?: protractor.ghostCueBallCenter
                    override val radius = protractor.radius
                }
                drawBallCenter(canvas, ghostBall, GhostCueBall(), state, paints, tps)
            }

            state.onPlaneBall?.let { ball ->
                drawBallCenter(canvas, ball, ActualCueBall(), state, paints, tps)
            }
        }

        // 3b. Draw Obstacle Ball Centers
        state.obstacleBalls.forEach { obstacle ->
            drawBallCenter(canvas, obstacle, ObstacleBall(), state, paints, tps)
        }
    }

    fun drawBeginnerStaticCircles(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val tps = if (state.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) null else state.lensWarpTps
        forEachBeginnerBall(state) { ball, config ->
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return@forEachBeginnerBall
            val drawCenter = ball.center.warpedBy(tps)
            val logicalScreenPos = DrawingUtils.mapPoint(drawCenter, logicalBallMatrix)
            val mappedEdge = DrawingUtils.mapPoint(PointF(drawCenter.x + ball.radius, drawCenter.y), logicalBallMatrix)
            val exactScreenRadius = hypot((mappedEdge.x - logicalScreenPos.x).toDouble(), (mappedEdge.y - logicalScreenPos.y).toDouble()).toFloat()

            val glowPaint = createGlowPaint(config.glowColor, config.glowWidth, state, paints, blurType = android.graphics.BlurMaskFilter.Blur.OUTER)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.strokeColor.toArgb()
                strokeWidth = 14f
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val screenInnerRadius = exactScreenRadius - (14f / 2f)

            canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, exactScreenRadius, glowPaint)
            canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, screenInnerRadius, strokePaint)
        }
    }

    fun drawBeginnerBubbleElements(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val tps = if (state.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) null else state.lensWarpTps
        forEachBeginnerBall(state) { ball, config ->
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return@forEachBeginnerBall
            val drawCenter = ball.center.warpedBy(tps)
            val logicalScreenPos = DrawingUtils.mapPoint(drawCenter, logicalBallMatrix)
            val mappedEdge = DrawingUtils.mapPoint(PointF(drawCenter.x + ball.radius, drawCenter.y), logicalBallMatrix)
            val exactScreenRadius = hypot((mappedEdge.x - logicalScreenPos.x).toDouble(), (mappedEdge.y - logicalScreenPos.y).toDouble()).toFloat()

            // Calculate bubble position
            val sensitivity = 6.0f
            val screenOffsetX = -state.currentOrientation.roll * sensitivity
            val screenOffsetY = state.currentOrientation.pitch * sensitivity
            val offsetDistance = hypot(screenOffsetX, screenOffsetY)
            val finalOffsetX = if (offsetDistance > exactScreenRadius) screenOffsetX * (exactScreenRadius / offsetDistance) else screenOffsetX
            val finalOffsetY = if (offsetDistance > exactScreenRadius) screenOffsetY * (exactScreenRadius / offsetDistance) else screenOffsetY
            val bubbleCenter = PointF(logicalScreenPos.x + finalOffsetX, logicalScreenPos.y + finalOffsetY)

            // Circle Fill
            val translucentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.strokeColor.toArgb()
                alpha = (config.opacity * 255 * 0.15f).toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, exactScreenRadius, translucentFillPaint)

            // Bubble Dot
            val dotRadius = exactScreenRadius * 0.1f
            val bubbleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, dotRadius, bubbleDotPaint)
        }
    }

    fun drawBeginnerStaticCenters(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val tps = if (state.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) null else state.lensWarpTps
        forEachBeginnerBall(state) { ball, _ ->
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return@forEachBeginnerBall
            val drawCenter = ball.center.warpedBy(tps)
            val logicalScreenPos = DrawingUtils.mapPoint(drawCenter, logicalBallMatrix)
            val mappedEdge = DrawingUtils.mapPoint(PointF(drawCenter.x + ball.radius, drawCenter.y), logicalBallMatrix)
            val exactScreenRadius = hypot((mappedEdge.x - logicalScreenPos.x).toDouble(), (mappedEdge.y - logicalScreenPos.y).toDouble()).toFloat()

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            val dotRadius = exactScreenRadius * 0.1f
            canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, dotRadius, dotPaint)
        }
    }

    fun drawBeginnerLabels(canvas: Canvas, state: CueDetatState, paints: PaintCache, typeface: Typeface?, labels: Map<String, String>) {
        drawAllLabels(canvas, state, paints, typeface, labels)
    }

    private inline fun forEachBeginnerBall(state: CueDetatState, action: (LogicalCircular, BallsConfig) -> Unit) {
        val protractor = state.protractorUnit
        action(protractor, TargetBall())

        if (!state.isMasseModeActive || state.masseConnectsTarget) {
            val ghostCenter = if (state.isMasseModeActive) state.masseGhostBallCenter else null
            val ghostBall = object : LogicalCircular {
                override val center = ghostCenter ?: protractor.ghostCueBallCenter
                override val radius = protractor.radius
            }
            action(ghostBall, GhostCueBall())
        }

        state.onPlaneBall?.let { action(it, ActualCueBall()) }
        state.obstacleBalls.forEach { action(it, ObstacleBall()) }
    }

    private fun drawDetectionRings(canvas: Canvas, state: CueDetatState, paints: PaintCache, tps: com.hereliesaz.cuedetat.domain.TpsWarpData?) {
        val detectedBalls = (state.visionData?.genericBalls ?: emptyList()) + (state.visionData?.customBalls ?: emptyList())
        val snappedPaint = Paint(paints.targetCirclePaint).apply {
            color = SulfurDust.toArgb()
            style = Paint.Style.STROKE
            alpha = 150
        }

        // Snapped Ball Indicators
        val allLogicalBalls = (listOfNotNull(state.onPlaneBall, state.protractorUnit) + state.obstacleBalls)
        allLogicalBalls.forEach { logicalBall ->
            val isSnapped = detectedBalls.any { detected ->
                hypot((logicalBall.center.x - detected.x).toDouble(), (logicalBall.center.y - detected.y).toDouble()) < 5.0
            }
            if (isSnapped) {
                state.pitchMatrix?.let { matrix ->
                    val drawCenter = logicalBall.center.warpedBy(tps)
                    canvas.save()
                    canvas.concat(matrix)
                    canvas.drawCircle(drawCenter.x, drawCenter.y, logicalBall.radius * 0.5f, snappedPaint)
                    canvas.restore()
                }
            }
        }

        // Passive Detection Glow (Selection mode NONE)
        if (state.tableScanModel != null && state.ballSelectionPhase == BallSelectionPhase.NONE) {
            val candidates = state.snapCandidates ?: emptyList()
            state.pitchMatrix?.let { matrix ->
                candidates.forEach { candidate ->
                    val glowPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 14f
                        color = android.graphics.Color.WHITE
                        alpha = if (candidate.isConfirmed) 180 else 40
                        maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                    val ringPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                        color = android.graphics.Color.WHITE
                        alpha = if (candidate.isConfirmed) 200 else 80
                    }
                    val drawCenter = candidate.detectedPoint.warpedBy(tps)
                    canvas.save()
                    canvas.concat(matrix)
                    val r = state.protractorUnit.radius * 1.3f
                    canvas.drawCircle(drawCenter.x, drawCenter.y, r, glowPaint)
                    canvas.drawCircle(drawCenter.x, drawCenter.y, r, ringPaint)
                    canvas.restore()
                }
            }
        }

        // Tap-target rings (During selection phases)
        if (state.ballSelectionPhase != BallSelectionPhase.NONE) {
            val candidates = state.snapCandidates?.filter { it.isConfirmed } ?: emptyList()
            val candidateRingPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 5f
                color = when (state.ballSelectionPhase) {
                    BallSelectionPhase.AWAITING_CUE -> Color(0xFFFFEB3B).toArgb()
                    else -> Color(0xFF4FC3F7).toArgb()
                }
                alpha = 200
            }
            val candidateGlowPaint = Paint(candidateRingPaint).apply {
                strokeWidth = 14f
                alpha = 60
                maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            state.pitchMatrix?.let { matrix ->
                candidates.forEach { candidate ->
                    val drawCenter = candidate.detectedPoint.warpedBy(tps)
                    canvas.save()
                    canvas.concat(matrix)
                    val r = state.protractorUnit.radius
                    canvas.drawCircle(drawCenter.x, drawCenter.y, r * 1.3f, candidateGlowPaint)
                    canvas.drawCircle(drawCenter.x, drawCenter.y, r * 1.3f, candidateRingPaint)
                    canvas.restore()
                }
            }
        }
    }

    private fun drawBallBody(
        canvas: Canvas,
        ball: LogicalCircular,
        config: BallsConfig,
        state: CueDetatState,
        paints: PaintCache,
        tps: com.hereliesaz.cuedetat.domain.TpsWarpData?
    ) {
        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        if (isBeginnerLocked) {
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return
            val logicalScreenPos = DrawingUtils.mapPoint(ball.center, logicalBallMatrix)
            val mappedEdge = DrawingUtils.mapPoint(PointF(ball.center.x + ball.radius, ball.center.y), logicalBallMatrix)
            val exactScreenRadius = hypot((mappedEdge.x - logicalScreenPos.x).toDouble(), (mappedEdge.y - logicalScreenPos.y).toDouble()).toFloat()

            // Calculate bubble position
            val sensitivity = 6.0f
            val screenOffsetX = -state.currentOrientation.roll * sensitivity
            val screenOffsetY = state.currentOrientation.pitch * sensitivity
            val offsetDistance = hypot(screenOffsetX, screenOffsetY)
            val finalOffsetX = if (offsetDistance > exactScreenRadius) screenOffsetX * (exactScreenRadius / offsetDistance) else screenOffsetX
            val finalOffsetY = if (offsetDistance > exactScreenRadius) screenOffsetY * (exactScreenRadius / offsetDistance) else screenOffsetY
            val bubbleCenter = PointF(logicalScreenPos.x + finalOffsetX, logicalScreenPos.y + finalOffsetY)

            // Paints
            val translucentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.strokeColor.toArgb()
                alpha = (config.opacity * 255 * 0.15f).toInt()
                style = Paint.Style.FILL
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.strokeColor.toArgb()
                strokeWidth = 14f
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val glowPaint = createGlowPaint(config.glowColor, config.glowWidth, state, paints, blurType = android.graphics.BlurMaskFilter.Blur.OUTER)
            val screenInnerRadius = exactScreenRadius - (14f / 2f)

            // Order for Beginner: Bubble fill is the LOWEST.
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, exactScreenRadius, translucentFillPaint)
            canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, exactScreenRadius, glowPaint)
            canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, screenInnerRadius, strokePaint)

        } else {
            // Expert/Dynamic Mode logic for Bodies
            val positionMatrix = state.pitchMatrix ?: return
            val sizeMatrix = state.sizeCalculationMatrix ?: positionMatrix
            val drawCenter = ball.center.warpedBy(tps)
            val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(drawCenter, ball.radius, state, sizeMatrix)
            val logicalScreenPos = DrawingUtils.mapPoint(drawCenter, positionMatrix)
            val yPosLifted = logicalScreenPos.y - radiusInfo.lift

            val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode, false)
            val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom)

            val logicalStrokePaint = Paint(paints.targetCirclePaint).apply {
                strokeWidth = config.strokeWidth / zoomFactor
                color = config.strokeColor.toArgb()
                alpha = (config.opacity * 255).toInt()
            }
            val isWarning = (state.isGeometricallyImpossible || state.isObstructed) && config is GhostCueBall
            val strokePaint = Paint(paints.targetCirclePaint).apply {
                color = if (isWarning) paints.warningPaint.color else config.strokeColor.toArgb()
                strokeWidth = config.strokeWidth
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val glowPaint = createGlowPaint(
                baseGlowColor = if (isWarning) Color(paints.warningPaint.color) else config.glowColor,
                baseGlowWidth = config.glowWidth,
                state = state,
                paints = paints
            )

            // Draw "On-Table" logical outline
            canvas.save()
            canvas.concat(positionMatrix)
            canvas.drawCircle(drawCenter.x, drawCenter.y, ball.radius, logicalStrokePaint)
            canvas.restore()

            // Draw "Lifted" visual body
            canvas.drawCircle(logicalScreenPos.x, yPosLifted, radiusInfo.radius, glowPaint)
            canvas.drawCircle(logicalScreenPos.x, yPosLifted, radiusInfo.radius, strokePaint)
        }
    }

    private fun drawBallCenter(
        canvas: Canvas,
        ball: LogicalCircular,
        config: BallsConfig,
        state: CueDetatState,
        paints: PaintCache,
        tps: com.hereliesaz.cuedetat.domain.TpsWarpData?
    ) {
        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        if (isBeginnerLocked) {
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return
            val logicalScreenPos = DrawingUtils.mapPoint(ball.center, logicalBallMatrix)
            val mappedEdge = DrawingUtils.mapPoint(PointF(ball.center.x + ball.radius, ball.center.y), logicalBallMatrix)
            val exactScreenRadius = hypot((mappedEdge.x - logicalScreenPos.x).toDouble(), (mappedEdge.y - logicalScreenPos.y).toDouble()).toFloat()

            // Bubble physics repeated for position
            val sensitivity = 6.0f
            val screenOffsetX = -state.currentOrientation.roll * sensitivity
            val screenOffsetY = state.currentOrientation.pitch * sensitivity
            val offsetDistance = hypot(screenOffsetX, screenOffsetY)
            val finalOffsetX = if (offsetDistance > exactScreenRadius) screenOffsetX * (exactScreenRadius / offsetDistance) else screenOffsetX
            val finalOffsetY = if (offsetDistance > exactScreenRadius) screenOffsetY * (exactScreenRadius / offsetDistance) else screenOffsetY
            val bubbleCenter = PointF(logicalScreenPos.x + finalOffsetX, logicalScreenPos.y + finalOffsetY)

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            val dotRadius = exactScreenRadius * 0.1f
            val bubbleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            // Draw center points on top of text
            canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, dotRadius, dotPaint)
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, dotRadius, bubbleDotPaint)

        } else {
            // Expert/Dynamic Mode logic for Centers
            val positionMatrix = state.pitchMatrix ?: return
            val drawCenter = ball.center.warpedBy(tps)
            val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(drawCenter, ball.radius, state, state.sizeCalculationMatrix ?: positionMatrix)
            val logicalScreenPos = DrawingUtils.mapPoint(drawCenter, positionMatrix)
            val yPosLifted = logicalScreenPos.y - radiusInfo.lift

            val dotPaint = Paint(paints.fillPaint).apply { color = android.graphics.Color.WHITE }
            val dotRadius = ball.radius * 0.1f

            // 1. Draw "On-Table" center dot
            canvas.save()
            canvas.concat(positionMatrix)
            canvas.drawCircle(drawCenter.x, drawCenter.y, dotRadius, dotPaint)
            canvas.restore()

            // 2. Draw "Lifted" visual center UI (Dot or Crosshair)
            val centerPaint = Paint(paints.fillPaint).apply { color = config.centerColor.toArgb() }
            val crosshairPaint = Paint(paints.targetCirclePaint).apply {
                color = config.centerColor.toArgb()
                strokeWidth = config.strokeWidth
            }
            val centerSize = radiusInfo.radius * config.centerSize

            when (config.centerShape) {
                CenterShape.NONE -> {}
                CenterShape.DOT -> canvas.drawCircle(logicalScreenPos.x, yPosLifted, centerSize, centerPaint)
                CenterShape.CROSSHAIR -> {
                    val circleRadius = centerSize * 0.4f
                    crosshairPaint.style = Paint.Style.STROKE
                    canvas.drawCircle(logicalScreenPos.x, yPosLifted, circleRadius, crosshairPaint)
                    canvas.drawLine(logicalScreenPos.x + circleRadius, yPosLifted, logicalScreenPos.x + centerSize, yPosLifted, crosshairPaint)
                    canvas.drawLine(logicalScreenPos.x - circleRadius, yPosLifted, logicalScreenPos.x - centerSize, yPosLifted, crosshairPaint)
                    canvas.drawLine(logicalScreenPos.x, yPosLifted + circleRadius, logicalScreenPos.x, yPosLifted + centerSize, crosshairPaint)
                    canvas.drawLine(logicalScreenPos.x, yPosLifted - circleRadius, logicalScreenPos.x, yPosLifted - centerSize, crosshairPaint)
                }
            }
        }
    }

    private fun drawBoundingBoxes(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val visionData = state.visionData ?: return
        if (visionData.detectedBoundingBoxes.isEmpty() || visionData.sourceImageWidth == 0) return

        val paint = Paint(paints.cvResultPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 150
        }

        val rotation = visionData.sourceImageRotation.toFloat()
        val srcWidth = visionData.sourceImageWidth.toFloat()
        val srcHeight = visionData.sourceImageHeight.toFloat()
        val canvasWidth = canvas.width.toFloat()
        val canvasHeight = canvas.height.toFloat()

        val matrix = Matrix()
        val srcRect = RectF(0f, 0f, srcWidth, srcHeight)
        val destRect = RectF(0f, 0f, canvasWidth, canvasHeight)
        matrix.setRectToRect(srcRect, destRect, Matrix.ScaleToFit.FILL)
        matrix.postRotate(rotation, canvasWidth / 2f, canvasHeight / 2f)


        visionData.detectedBoundingBoxes.forEach { box ->
            val boxRect = RectF(box)
            matrix.mapRect(boxRect)
            canvas.drawRect(boxRect, paint)
        }
    }

    private fun drawAllLabels(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?,
        labels: Map<String, String>
    ) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        state.onPlaneBall?.let {
            val (labelKey, config) = if (state.isBankingMode) {
                "bankingBall" to LabelConfig.bankingBall
            } else {
                "actualCueBall" to LabelConfig.actualCueBall
            }
            val label = labels[labelKey] ?: labelKey
            textRenderer.draw(canvas, textPaint, it, label, config, state)
        }

        if (!state.isBankingMode) {
            val targetLabel = labels["targetBall"] ?: "Target Ball"
            textRenderer.draw(
                canvas,
                textPaint,
                state.protractorUnit,
                targetLabel,
                LabelConfig.targetBall,
                state
            )

            val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked
            val ghostCueText = if (isBeginnerLocked) {
                labels["ghostCueInstruction"] ?: "Aim the cue ball at\nthe center of the blue circle."
            } else {
                labels["ghostCueBall"] ?: "Ghost Cue Ball"
            }

            textRenderer.draw(canvas, textPaint, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, ghostCueText, LabelConfig.ghostCueBall, state, drawBelow = true)
        }

        state.obstacleBalls.forEachIndexed { index, obstacle ->
            val obstacleLabel = labels["obstacle"]?.format(index + 1) ?: "Obstacle ${index + 1}"
            textRenderer.draw(
                canvas,
                textPaint,
                obstacle,
                obstacleLabel,
                LabelConfig.obstacleBall,
                state
            )
        }
    }
}

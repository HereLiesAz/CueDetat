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
import com.hereliesaz.cuedetat.view.renderer.util.drawGlowCircle
import com.hereliesaz.cuedetat.view.renderer.warpedBy
import kotlin.math.hypot
import androidx.core.graphics.withMatrix

class BallRenderer {

    private val textRenderer = BallTextRenderer()

    // Reused per-frame to avoid GC pressure in the bounding-box draw path,
    // which runs on every camera frame.
    private val boundingBoxMatrix = Matrix()
    private val boundingBoxScratch = RectF()

    // Hoisted Paints — previously allocated inside draw methods on every redraw. Each is
    // fully reconfigured on every use (reset()+flags, or set(base) to copy a cached paint),
    // so behavior is identical to the old `Paint(...).apply { }` and theme-driven color
    // changes on the base paints still propagate.
    private val beginnerCircleStroke = Paint()
    private val beginnerBubbleFill = Paint()
    private val beginnerBubbleDot = Paint()
    private val beginnerCenterDot = Paint()
    private val snappedRingPaint = Paint()
    private val passiveGlowPaint = Paint()
    private val passiveRingPaint = Paint()
    private val candidateRingPaint = Paint()
    private val candidateGlowPaint = Paint()
    private val bodyLogicalStroke = Paint()
    private val bodyStroke = Paint()
    private val bodyBeginnerFill = Paint()
    private val bodyBeginnerStroke = Paint()
    private val centerBeginnerDot = Paint()
    private val centerBeginnerBubbleDot = Paint()
    private val centerOnTableDot = Paint()
    private val centerLiftedDot = Paint()
    private val centerCrosshair = Paint()
    private val boundingBoxPaint = Paint()

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

            val strokePaint = beginnerCircleStroke.apply {
                reset()
                isAntiAlias = true
                color = config.strokeColor.toArgb()
                strokeWidth = 14f
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val screenInnerRadius = exactScreenRadius - (14f / 2f)

            drawGlowCircle(canvas, logicalScreenPos.x, logicalScreenPos.y, exactScreenRadius, config.glowColor, state, paints)
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
            val translucentFillPaint = beginnerBubbleFill.apply {
                reset()
                isAntiAlias = true
                color = config.strokeColor.toArgb()
                alpha = (config.opacity * 255 * 0.15f).toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, exactScreenRadius, translucentFillPaint)

            // Bubble Dot
            val dotRadius = exactScreenRadius * 0.1f
            val bubbleDotPaint = beginnerBubbleDot.apply {
                reset()
                isAntiAlias = true
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

            val dotPaint = beginnerCenterDot.apply {
                reset()
                isAntiAlias = true
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
        val detectedBalls = (state.visionData?.genericBalls ?: emptyList()) + 
                           (state.visionData?.balls?.map { it.position } ?: emptyList())
        val snappedPaint = snappedRingPaint.apply {
            set(paints.targetCirclePaint)
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
                    canvas.withMatrix(matrix) {
                        drawCircle(
                            drawCenter.x,
                            drawCenter.y,
                            logicalBall.radius * 0.5f,
                            snappedPaint
                        )
                    }
                }
            }
        }

        // Passive Detection Glow (Selection mode NONE)
        if (state.tableScanModel != null && state.ballSelectionPhase == BallSelectionPhase.NONE) {
            val candidates = state.snapCandidates ?: emptyList()
            state.pitchMatrix?.let { matrix ->
                candidates.forEach { candidate ->
                    val glowPaint = passiveGlowPaint.apply {
                        reset()
                        style = Paint.Style.STROKE
                        strokeWidth = 14f
                        color = android.graphics.Color.WHITE
                        alpha = if (candidate.isConfirmed) 180 else 40
                        maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                    val ringPaint = passiveRingPaint.apply {
                        reset()
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                        color = android.graphics.Color.WHITE
                        alpha = if (candidate.isConfirmed) 200 else 80
                    }
                    val drawCenter = candidate.detectedPoint.warpedBy(tps)
                    canvas.withMatrix(matrix) {
                        val r = state.protractorUnit.radius * 1.3f
                        drawCircle(drawCenter.x, drawCenter.y, r, glowPaint)
                        drawCircle(drawCenter.x, drawCenter.y, r, ringPaint)
                    }
                }
            }
        }

        // Tap-target rings (During selection phases)
        if (state.ballSelectionPhase != BallSelectionPhase.NONE) {
            val candidates = state.snapCandidates?.filter { it.isConfirmed } ?: emptyList()
            val candidateRingPaint = this.candidateRingPaint.apply {
                reset()
                style = Paint.Style.STROKE
                strokeWidth = 5f
                color = when (state.ballSelectionPhase) {
                    BallSelectionPhase.AWAITING_CUE -> Color(0xFFFFEB3B).toArgb()
                    else -> Color(0xFF4FC3F7).toArgb()
                }
                alpha = 200
            }
            val candidateGlowPaint = this.candidateGlowPaint.apply {
                set(candidateRingPaint)
                strokeWidth = 14f
                alpha = 60
                maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            state.pitchMatrix?.let { matrix ->
                candidates.forEach { candidate ->
                    val drawCenter = candidate.detectedPoint.warpedBy(tps)
                    canvas.withMatrix(matrix) {
                        val r = state.protractorUnit.radius
                        drawCircle(drawCenter.x, drawCenter.y, r * 1.3f, candidateGlowPaint)
                        drawCircle(drawCenter.x, drawCenter.y, r * 1.3f, candidateRingPaint)
                    }
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
            val translucentFillPaint = bodyBeginnerFill.apply {
                reset()
                isAntiAlias = true
                color = config.strokeColor.toArgb()
                alpha = (config.opacity * 255 * 0.15f).toInt()
                style = Paint.Style.FILL
            }
            val strokePaint = bodyBeginnerStroke.apply {
                reset()
                isAntiAlias = true
                color = config.strokeColor.toArgb()
                strokeWidth = 14f
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val screenInnerRadius = exactScreenRadius - (14f / 2f)

            // Order for Beginner: Bubble fill is the LOWEST.
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, exactScreenRadius, translucentFillPaint)
            drawGlowCircle(canvas, logicalScreenPos.x, logicalScreenPos.y, exactScreenRadius, config.glowColor, state, paints)
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

            val logicalStrokePaint = bodyLogicalStroke.apply {
                set(paints.targetCirclePaint)
                strokeWidth = config.strokeWidth / zoomFactor
                color = config.strokeColor.toArgb()
                alpha = (config.opacity * 255).toInt()
            }
            val isWarning = (state.isGeometricallyImpossible || state.isObstructed) && config is GhostCueBall
            val strokePaint = bodyStroke.apply {
                set(paints.targetCirclePaint)
                color = if (isWarning) paints.warningPaint.color else config.strokeColor.toArgb()
                strokeWidth = config.strokeWidth
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val glowColor = if (isWarning) Color(paints.warningPaint.color) else config.glowColor

            // Draw "On-Table" logical outline
            canvas.withMatrix(positionMatrix) {
                drawCircle(drawCenter.x, drawCenter.y, ball.radius, logicalStrokePaint)
            }

            // Draw "Lifted" visual body
            drawGlowCircle(canvas, logicalScreenPos.x, yPosLifted, radiusInfo.radius, glowColor, state, paints)
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

            val dotPaint = centerBeginnerDot.apply {
                reset()
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            val dotRadius = exactScreenRadius * 0.1f
            val bubbleDotPaint = centerBeginnerBubbleDot.apply {
                reset()
                isAntiAlias = true
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

            val dotPaint = centerOnTableDot.apply { set(paints.fillPaint); color = android.graphics.Color.WHITE }
            val dotRadius = ball.radius * 0.1f

            // 1. Draw "On-Table" center dot
            canvas.withMatrix(positionMatrix) {
                drawCircle(drawCenter.x, drawCenter.y, dotRadius, dotPaint)
            }

            // 2. Draw "Lifted" visual center UI (Dot or Crosshair)
            val centerPaint = centerLiftedDot.apply { set(paints.fillPaint); color = config.centerColor.toArgb() }
            val crosshairPaint = centerCrosshair.apply {
                set(paints.targetCirclePaint)
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
        if (visionData.sourceImageWidth == 0) return

        // Every detected ball must show a bounding box. If a ball is missing
        // one (legacy path), synthesise a square box around its image-space
        // centre using the median ball box size so the user gets a visible
        // marker on EVERY ball the ML or CV pipeline produced.
        //
        // Hot path: this runs on every CV frame. Use a sequence for the
        // median pass to avoid the intermediate filter / map / filter list
        // allocations, and bail to the legacy detectedBoundingBoxes list
        // before touching the per-ball loop when there are no typed balls.
        val balls = visionData.balls
        val sides: List<Int> = balls.asSequence()
            .mapNotNull { it.boundingBox }
            .map { minOf(it.width(), it.height()) }
            .filter { it > 0 }
            .sorted()
            .toList()
        val fallbackHalfSide = if (sides.isEmpty()) 0 else sides[sides.size / 2] / 2

        val typedBoxesWithFallback: List<Pair<android.graphics.Rect, com.hereliesaz.cuedetat.data.BallType>> =
            balls.mapNotNull { ball ->
                val box = ball.boundingBox ?: if (fallbackHalfSide > 0) {
                    android.graphics.Rect(
                        (ball.position.x - fallbackHalfSide).toInt(),
                        (ball.position.y - fallbackHalfSide).toInt(),
                        (ball.position.x + fallbackHalfSide).toInt(),
                        (ball.position.y + fallbackHalfSide).toInt(),
                    )
                } else null
                box?.let { it to ball.type }
            }

        val boxes: List<Pair<android.graphics.Rect, com.hereliesaz.cuedetat.data.BallType>> =
            if (typedBoxesWithFallback.isNotEmpty()) {
                typedBoxesWithFallback
            } else {
                visionData.detectedBoundingBoxes.map { it to com.hereliesaz.cuedetat.data.BallType.UNKNOWN }
            }
        if (boxes.isEmpty()) return

        val rotation = visionData.sourceImageRotation.toFloat()
        val srcWidth = visionData.sourceImageWidth.toFloat()
        val srcHeight = visionData.sourceImageHeight.toFloat()
        val canvasWidth = canvas.width.toFloat()
        val canvasHeight = canvas.height.toFloat()

        // CameraBackground uses PreviewView.ScaleType.FILL_CENTER, which is
        // centre-crop with rotation. Reconstruct the same transform here so
        // bounding boxes land on top of the visible balls instead of being
        // stretched by FILL. Reuse the same Matrix instance every frame.
        val rotatedW = if (rotation % 180f == 0f) srcWidth else srcHeight
        val rotatedH = if (rotation % 180f == 0f) srcHeight else srcWidth
        val scale = maxOf(canvasWidth / rotatedW, canvasHeight / rotatedH)

        boundingBoxMatrix.reset()
        boundingBoxMatrix.postTranslate(-srcWidth / 2f, -srcHeight / 2f)
        boundingBoxMatrix.postRotate(rotation)
        boundingBoxMatrix.postScale(scale, scale)
        boundingBoxMatrix.postTranslate(canvasWidth / 2f, canvasHeight / 2f)

        val paint = boundingBoxPaint.apply {
            set(paints.cvResultPaint)
            style = Paint.Style.STROKE
            strokeWidth = 6f
            alpha = 255
        }

        boxes.forEach { (box, type) ->
            paint.color = colorForBallType(type).toArgb()
            boundingBoxScratch.set(box)
            boundingBoxMatrix.mapRect(boundingBoxScratch)
            canvas.drawRect(boundingBoxScratch, paint)
        }
    }

    private fun colorForBallType(type: com.hereliesaz.cuedetat.data.BallType): Color = when (type) {
        // Solids vs stripes are deliberately on opposite sides of the wheel so
        // they can't be confused at a glance against the green felt.
        com.hereliesaz.cuedetat.data.BallType.SOLID -> Color(0xFFFFC107)   // amber
        com.hereliesaz.cuedetat.data.BallType.STRIPE -> Color(0xFF00E5FF)  // cyan
        com.hereliesaz.cuedetat.data.BallType.CUE -> Color(0xFFFFFFFF)     // white
        com.hereliesaz.cuedetat.data.BallType.EIGHT -> Color(0xFFE040FB)   // magenta (8-ball is black; magenta reads against the felt)
        // Unclassified balls are drawn in a noticeable color so a misfire of
        // classifyBallType() is visible instead of looking like background gray.
        com.hereliesaz.cuedetat.data.BallType.UNKNOWN -> Color(0xFFFF6D00) // orange
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

// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.line.AimingLine
import com.hereliesaz.cuedetat.view.config.line.BankLine1
import com.hereliesaz.cuedetat.view.config.line.BankLine2
import com.hereliesaz.cuedetat.view.config.line.BankLine3
import com.hereliesaz.cuedetat.view.config.line.BankLine4
import com.hereliesaz.cuedetat.view.config.line.ShotGuideLine
import com.hereliesaz.cuedetat.view.config.line.TangentLine
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.config.ui.ProtractorGuides
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.renderer.warpedBy
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.util.createGlowPaint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val textRenderer = LineTextRenderer()
    private val protractorAngles = floatArrayOf(10f, 20f, 30f, 40f, 50f, 60f, 70f, 80f)

    fun drawLogicalLines(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?,
        activeMatrix: Matrix
    ) {
        var camArray: DoubleArray? = null
        var distArray: DoubleArray? = null

        val camMat = state.cameraMatrix
        val distMat = state.distCoeffs
        if (camMat != null && !camMat.empty() && distMat != null && !distMat.empty()) {
            camArray = DoubleArray(camMat.total().toInt())
            camMat.get(0, 0, camArray)
            distArray = DoubleArray(distMat.total().toInt())
            distMat.get(0, 0, distArray)
        }

        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints, activeMatrix, camArray, distArray)
        } else {
            drawProtractorLines(canvas, state, paints, typeface, activeMatrix, camArray, distArray)
            drawProtractorGuides(canvas, state, paints, activeMatrix, camArray, distArray)
        }
    }

    fun drawBeginnerForeground(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?,
        activeMatrix: Matrix
    ) {
        var camArray: DoubleArray? = null
        var distArray: DoubleArray? = null

        val camMat = state.cameraMatrix
        val distMat = state.distCoeffs
        if (camMat != null && !camMat.empty() && distMat != null && !distMat.empty()) {
            camArray = DoubleArray(camMat.total().toInt())
            camMat.get(0, 0, camArray)
            distArray = DoubleArray(distMat.total().toInt())
            distMat.get(0, 0, distArray)
        }

        // IMPORTANT: We must wrap these in a matrix block because LineRenderer's
        // internal methods (drawFadingLine, drawBankablePath) assume the canvas
        // is ALREADY transformed by activeMatrix so they can concat the inverse
        // and draw in screen space without double projection.
        canvas.save()
        canvas.concat(activeMatrix)
        drawTangentLines(canvas, state, paints, activeMatrix, camArray, distArray, typeface)
        drawAimingLines(canvas, state, paints, activeMatrix, camArray, distArray, typeface)
        canvas.restore()
    }

    private fun drawProtractorLines(
        canvas: Canvas, state: CueDetatState,
        paints: PaintCache, typeface: Typeface?, activeMatrix: Matrix,
        camArray: DoubleArray?, distArray: DoubleArray?
    ) {
        val shotLineAnchor = state.shotLineAnchor ?: return
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotGuideLineConfig = ShotGuideLine()

        val shotLineIsWarning = state.isGeometricallyImpossible || state.isTiltBeyondLimit || state.isObstructed
        val shotLinePaint = Paint(paints.shotLinePaint).apply {
            color = if (shotLineIsWarning) paints.warningPaint.color else shotGuideLineConfig.strokeColor.toArgb()
            strokeWidth = shotGuideLineConfig.strokeWidth
        }
        val shotLineGlow = createGlowPaint(
            baseGlowColor = if (shotLineIsWarning) Color(paints.warningPaint.color) else shotGuideLineConfig.glowColor,
            baseGlowWidth = shotGuideLineConfig.glowWidth,
            state = state,
            paints = paints
        )
        val obstructionPaint = Paint(paints.pathObstructionPaint).apply {
            strokeWidth = state.protractorUnit.radius * 2
        }

        val shotGuideDirection = normalize(PointF(ghostCueCenter.x - shotLineAnchor.x, ghostCueCenter.y - shotLineAnchor.y))

        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        if (state.table.isVisible || state.obstacleBalls.isNotEmpty()) {
            if (isBeginnerLocked) {
                // Skip pathways in locked
            } else {
                drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, obstructionPaint, null, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
            }

            state.aimingLineBankPath?.let {
                drawBankablePath(canvas, it, obstructionPaint, null, isPocketed = false, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
            }
        }

        if (isBeginnerLocked) {
            // Skip inner lines in Pass 1; they will be drawn in drawBeginnerForeground in Pass 4
        } else {
            drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, shotLinePaint, shotLineGlow, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
            drawTangentLines(canvas, state, paints, activeMatrix, camArray, distArray, typeface)
            drawAimingLines(canvas, state, paints, activeMatrix, camArray, distArray, typeface)
        }

        drawSpinPaths(canvas, state, paints, activeMatrix, camArray, distArray)

        if (state.areHelpersVisible && state.experienceMode != ExperienceMode.BEGINNER) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawAimingLines(canvas: Canvas, state: CueDetatState, paints: PaintCache, activeMatrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?, typeface: Typeface?) {
        val aimingLineConfig = AimingLine()
        val isPocketed = state.aimedPocketIndex != null
        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        val baseAimingColor = if (isPocketed) SulfurDust else aimingLineConfig.strokeColor
        val aimingStrokeWidth = if (isBeginnerLocked) aimingLineConfig.strokeWidth * 4f else aimingLineConfig.strokeWidth

        val aimingLinePaint = Paint(paints.targetCirclePaint).apply {
            color = baseAimingColor.toArgb()
            strokeWidth = aimingStrokeWidth
        }
        val aimingLineGlow = createGlowPaint(
            baseGlowColor = aimingLineConfig.glowColor,
            baseGlowWidth = if (isBeginnerLocked) aimingLineConfig.glowWidth * 2f else aimingLineConfig.glowWidth,
            state = state,
            paints = paints
        )

        val obstructionPaint = Paint(paints.pathObstructionPaint).apply {
            strokeWidth = state.protractorUnit.radius * 2
        }

        val tps = state.lensWarpTps
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center

        val rawPath = state.aimingLineBankPath ?: listOf(ghostCueCenter, targetCenter)
        val path = rawPath.map { it.warpedBy(tps) }

        if (state.table.isVisible || state.obstacleBalls.isNotEmpty()) {
            drawBankablePath(canvas, path, obstructionPaint, null, isPocketed = false, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
        }

        val textToDraw = if (isBeginnerLocked) "Aim this line at the pocket." else null
        drawBankablePath(canvas, path, aimingLinePaint, aimingLineGlow, isPocketed, state, paints, activeMatrix, camArray, distArray, isBeginnerLocked, textToDraw, typeface)
    }

    private fun drawTangentLines(canvas: Canvas, state: CueDetatState, paints: PaintCache, activeMatrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?, typeface: Typeface?) {
        val tangentLineConfig = TangentLine()
        val isPocketed = state.tangentAimedPocketIndex != null
        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        val baseTangentColor = if (isPocketed) WarningRed else tangentLineConfig.strokeColor
        val tangentStrokeWidth = if (isBeginnerLocked) tangentLineConfig.strokeWidth * 4f else tangentLineConfig.strokeWidth

        val tangentSolidPaint = Paint(paints.tangentLineSolidPaint).apply {
            color = baseTangentColor.toArgb()
            strokeWidth = tangentStrokeWidth
        }
        val tangentDottedPaint = Paint(paints.tangentLineDottedPaint).apply {
            color = tangentLineConfig.strokeColor.toArgb()
            strokeWidth = tangentStrokeWidth
            alpha = (tangentLineConfig.opacity * 255).toInt()
        }
        val tangentGlow = createGlowPaint(
            baseGlowColor = tangentLineConfig.glowColor,
            baseGlowWidth = if (isBeginnerLocked) tangentLineConfig.glowWidth * 2f else tangentLineConfig.glowWidth,
            state = state,
            paints = paints
        )

        val tps = state.lensWarpTps
        val rawStart = state.protractorUnit.ghostCueBallCenter
        val towards = state.protractorUnit.center
        val dxToTarget = towards.x - rawStart.x
        val dyToTarget = towards.y - rawStart.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget < 0.001f) return

        val tangentDx = -dyToTarget / magToTarget
        val tangentDy = dxToTarget / magToTarget

        val start = rawStart.warpedBy(tps)

        if (isBeginnerLocked) {
            drawFadingLine(canvas, start, normalize(PointF(tangentDx, tangentDy)), tangentSolidPaint, tangentGlow, state, paints, activeMatrix, camArray, distArray, true, "Tangent Line", typeface)
            drawFadingLine(canvas, start, normalize(PointF(-tangentDx, -tangentDy)), tangentSolidPaint, tangentGlow, state, paints, activeMatrix, camArray, distArray, true, "Tangent Line", typeface)
            return
        }

        if (state.isStraightShot) {
            drawFadingLine(canvas, start, normalize(PointF(tangentDx, tangentDy)), tangentDottedPaint, tangentGlow, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
            drawFadingLine(canvas, start, normalize(PointF(-tangentDx, -tangentDy)), tangentDottedPaint, tangentGlow, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
        } else {
            val rawActivePath = state.tangentLineBankPath ?: listOf(rawStart, PointF(rawStart.x + tangentDx * 5000f * state.tangentDirection, rawStart.y + tangentDy * 5000f * state.tangentDirection))
            val activePath = rawActivePath.map { it.warpedBy(tps) }
            drawBankablePath(canvas, activePath, tangentSolidPaint, tangentGlow, isPocketed, state, paints, activeMatrix, camArray, distArray, false, null, typeface)

            val inactiveDirection = normalize(PointF(tangentDx * -state.tangentDirection, tangentDy * -state.tangentDirection))
            drawFadingLine(canvas, start, inactiveDirection, tangentDottedPaint, tangentGlow, state, paints, activeMatrix, camArray, distArray, false, null, typeface)
        }
    }

    private fun drawSpinPaths(canvas: Canvas, state: CueDetatState, paints: PaintCache, activeMatrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?) {
        val paths = state.spinPaths ?: return
        if (paths.isEmpty()) return

        val inverseMatrix = Matrix().apply { activeMatrix.invert(this) }
        val alpha = (255 * state.spinPathsAlpha).toInt()
        val spinPathPaint = Paint(paints.shotLinePaint).apply { strokeWidth = 4f }
        val spinGlowPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 8f }

        canvas.save()
        canvas.concat(inverseMatrix)

        paths.forEach { (color, points) ->
            if (points.size < 2) return@forEach

            val screenPath = Path()
            points.forEachIndexed { index, pt ->
                val screenPt = DrawingUtils.mapPoint(pt, activeMatrix)
                val finalPt = if (camArray != null && distArray != null && camArray.size == 9) {
                    DrawingUtils.applyBarrelDistortion(screenPt.x, screenPt.y, camArray, distArray)
                } else screenPt

                if (index == 0) screenPath.moveTo(finalPt.x, finalPt.y)
                else screenPath.lineTo(finalPt.x, finalPt.y)
            }

            spinPathPaint.color = color.toArgb()
            spinPathPaint.alpha = alpha
            spinGlowPaint.color = color.toArgb()
            spinGlowPaint.alpha = (color.alpha * 100 * state.spinPathsAlpha).toInt().coerceIn(0, 255)

            canvas.drawPath(screenPath, spinGlowPaint)
            canvas.drawPath(screenPath, spinPathPaint)
        }
        canvas.restore()
    }

    private fun drawBankingLines(canvas: Canvas, state: CueDetatState, paints: PaintCache, activeMatrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?) {
        val rawPath = state.bankShotPath ?: return
        if (rawPath.size < 2) return

        val tps = state.lensWarpTps
        val path = rawPath.map { it.warpedBy(tps) }
        val inverseMatrix = Matrix().apply { activeMatrix.invert(this) }

        val isPocketed = state.pocketedBankShotPocketIndex != null

        if (isPocketed) {
            val whitePaint = Paint(paints.shotLinePaint).apply { color = Color.White.toArgb() }
            val whiteGlowPaint = createGlowPaint(Color.White, 12f, state, paints)
            drawPath(canvas, path, whitePaint, whiteGlowPaint, activeMatrix, camArray, distArray)
        } else {
            val bankLineConfigs = listOf(BankLine1(), BankLine2(), BankLine3(), BankLine4())
            val finalSegmentIndex = path.size - 2

            canvas.save()
            canvas.concat(inverseMatrix)
            for (i in 0 until finalSegmentIndex) {
                val start = path[i]
                val rawEnd = path[i + 1]
                val end = getSafeLogicalPoint(start, rawEnd, activeMatrix) ?: continue

                val config = bankLineConfigs.getOrElse(i) { bankLineConfigs.last() }

                val linePaint = Paint(paints.bankLine1Paint).apply { color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth }
                val glowPaint = createGlowPaint(config.glowColor, config.glowWidth, state, paints)

                val segmentPath = DrawingUtils.buildDistortedLinePath(start, end, activeMatrix, camArray, distArray)
                canvas.drawPath(segmentPath, glowPaint)
                canvas.drawPath(segmentPath, linePaint)
            }
            canvas.restore()

            if (path.size > 1) {
                val start = path[finalSegmentIndex]
                val end = path.last()
                val direction = normalize(PointF(end.x - start.x, end.y - start.y))
                val config = bankLineConfigs.getOrElse(finalSegmentIndex) { bankLineConfigs.last() }

                val linePaint = Paint(paints.bankLine1Paint).apply { color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth }
                val glowPaint = createGlowPaint(config.glowColor, config.glowWidth, state, paints)

                drawFadingLine(canvas, start, direction, linePaint, glowPaint, state, paints, activeMatrix, camArray, distArray, false, null, null)
            }
        }
    }

    private fun drawProtractorGuides(canvas: Canvas, state: CueDetatState, paints: PaintCache, activeMatrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?) {
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center
        val config = ProtractorGuides()
        val labelConfig = LabelConfig.angleGuide

        val guidePaint = Paint(paints.angleGuidePaint).apply {
            color = config.strokeColor.toArgb()
            strokeWidth = config.strokeWidth
            alpha = (config.opacity * 255).toInt()
        }
        val textPaint = Paint(paints.textPaint).apply {
            alpha = (config.opacity * 255).toInt()
            textSize = 30f
        }

        protractorAngles.forEach { angle ->
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, angle, guidePaint, state, paints, activeMatrix, camArray, distArray)
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, -angle, guidePaint, state, paints, activeMatrix, camArray, distArray)
            if (state.areHelpersVisible && state.experienceMode != ExperienceMode.BEGINNER) {
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, angle, textPaint, state.protractorUnit.radius)
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, -angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun drawBankablePath(
        canvas: Canvas,
        path: List<PointF>,
        primaryPaint: Paint,
        glowPaint: Paint?,
        isPocketed: Boolean,
        state: CueDetatState,
        paints: PaintCache,
        activeMatrix: Matrix,
        camArray: DoubleArray?,
        distArray: DoubleArray?,
        drawTriangles: Boolean,
        textToDraw: String?,
        typeface: Typeface?
    ) {
        if (path.size < 2) return
        val finalSegmentIndex = path.size - 2
        val inverseMatrix = Matrix().apply { activeMatrix.invert(this) }

        for (i in 0..finalSegmentIndex) {
            val start = path[i]
            val rawEnd = path[i + 1]
            val isLastSegment = i == finalSegmentIndex

            if (isLastSegment) {
                val direction = normalize(PointF(rawEnd.x - start.x, rawEnd.y - start.y))
                drawFadingLine(canvas, start, direction, primaryPaint, glowPaint, state, paints, activeMatrix, camArray, distArray, drawTriangles, textToDraw, typeface)
            } else {
                val end = getSafeLogicalPoint(start, rawEnd, activeMatrix) ?: continue
                val segmentPath = DrawingUtils.buildDistortedLinePath(start, end, activeMatrix, camArray, distArray)
                canvas.save()
                canvas.concat(inverseMatrix)
                glowPaint?.let { canvas.drawPath(segmentPath, it) }
                canvas.drawPath(segmentPath, primaryPaint)
                canvas.restore()
            }
        }
    }

    private fun drawPath(canvas: Canvas, path: List<PointF>, paint: Paint, glowPaint: Paint? = null, activeMatrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?) {
        if (path.size < 2) return
        val inverseMatrix = Matrix().apply { activeMatrix.invert(this) }

        canvas.save()
        canvas.concat(inverseMatrix)
        for (i in 0 until path.size - 1) {
            val start = path[i]
            val rawEnd = path[i + 1]
            val end = getSafeLogicalPoint(start, rawEnd, activeMatrix) ?: continue

            val segmentPath = DrawingUtils.buildDistortedLinePath(start, end, activeMatrix, camArray, distArray)
            glowPaint?.let { canvas.drawPath(segmentPath, it) }
            canvas.drawPath(segmentPath, paint)
        }
        canvas.restore()
    }

    private fun drawAngleGuide(
        canvas: Canvas,
        center: PointF,
        referencePoint: PointF,
        angleDegrees: Float,
        paint: Paint,
        state: CueDetatState,
        paints: PaintCache,
        activeMatrix: Matrix,
        camArray: DoubleArray?,
        distArray: DoubleArray?
    ) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val direction = normalize(PointF(cos(finalAngleRad).toFloat(), sin(finalAngleRad).toFloat()))
        drawFadingLine(canvas, center, direction, paint, null, state, paints, activeMatrix, camArray, distArray, false, null, null)
    }

    private fun drawFadingLine(
        canvas: Canvas,
        start: PointF,
        direction: PointF,
        paint: Paint,
        glowPaint: Paint?,
        state: CueDetatState,
        paints: PaintCache,
        activeMatrix: Matrix,
        camArray: DoubleArray?,
        distArray: DoubleArray?,
        drawTriangles: Boolean,
        textToDraw: String? = null,
        typeface: Typeface? = null
    ) {
        val inverseMatrix = Matrix().apply { activeMatrix.invert(this) }

        val tableLength = state.table.logicalHeight
        val totalLength = tableLength * 4.0f
        val fadeStartDistance = tableLength * 1.2f

        val rawEnd = PointF(start.x + direction.x * totalLength, start.y + direction.y * totalLength)
        val rawFadeStart = PointF(start.x + direction.x * fadeStartDistance, start.y + direction.y * fadeStartDistance)

        val end = getSafeLogicalPoint(start, rawEnd, activeMatrix) ?: return
        val fadeStart = getSafeLogicalPoint(start, rawFadeStart, activeMatrix) ?: end

        val path = DrawingUtils.buildDistortedLinePath(start, end, activeMatrix, camArray, distArray)

        val screenFadeStart = DrawingUtils.mapPoint(fadeStart, activeMatrix)
        val screenEnd = DrawingUtils.mapPoint(end, activeMatrix)

        val finalFadeStart = if (camArray != null && distArray != null && camArray.size == 9) {
            DrawingUtils.applyBarrelDistortion(screenFadeStart.x, screenFadeStart.y, camArray, distArray)
        } else screenFadeStart

        val finalEnd = if (camArray != null && distArray != null && camArray.size == 9) {
            DrawingUtils.applyBarrelDistortion(screenEnd.x, screenEnd.y, camArray, distArray)
        } else screenEnd

        canvas.save()
        canvas.concat(inverseMatrix)

        val layer = canvas.saveLayer(null, null)
        try {
            val measure = android.graphics.PathMeasure(path, false)
            val pathLen = measure.length

            // Collect all points on the path that are physically inside the screen view
            val visiblePts = mutableListOf<FloatArray>()
            var d = 0f
            val pos = FloatArray(2)
            val tan = FloatArray(2)

            val margin = 80f
            while(d <= pathLen) {
                if (measure.getPosTan(d, pos, tan)) {
                    if (pos[0] in margin..(state.viewWidth - margin) && pos[1] in margin..(state.viewHeight - margin)) {
                        visiblePts.add(floatArrayOf(pos[0], pos[1], tan[0], tan[1], d))
                    }
                }
                d += 10f
            }

            // DRAW TRIANGLES FIRST (Rendered BELOW the line)
            if (drawTriangles && visiblePts.isNotEmpty()) {
                val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = paint.color // EXACT SAME COLOR AS THE LINE
                    setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
                }

                // Pick 3 evenly spaced points strictly from the visible segment
                val startIdx = (visiblePts.size * 0.2f).toInt().coerceAtMost(visiblePts.size - 1)
                val endIdx = (visiblePts.size * 0.8f).toInt().coerceAtMost(visiblePts.size - 1)
                val validPts = visiblePts.subList(startIdx, endIdx + 1)

                if (validPts.isNotEmpty()) {
                    val step = (validPts.size / 3).coerceAtLeast(1)
                    var drawn = 0
                    for (i in 0 until validPts.size step step) {
                        if (drawn >= 3) break
                        val pt = validPts[i]
                        val angle = Math.toDegrees(atan2(pt[3].toDouble(), pt[2].toDouble())).toFloat()

                        canvas.save()
                        canvas.translate(pt[0], pt[1])
                        canvas.rotate(angle)

                        // Real, solid triangle pointing exactly down the path
                        val triPath = Path().apply {
                            moveTo(35f, 0f)
                            lineTo(-25f, 25f)
                            lineTo(-25f, -25f)
                            close()
                        }
                        canvas.drawPath(triPath, trianglePaint)
                        canvas.restore()
                        drawn++
                    }
                }
            }

            // DRAW THE LINE OVER THE TRIANGLES
            glowPaint?.let { canvas.drawPath(path, it) }
            canvas.drawPath(path, paint)

            // DRAW THE CYAN TEXT ON TOP
            if (textToDraw != null && state.areHelpersVisible && visiblePts.isNotEmpty()) {
                val midPt = visiblePts[visiblePts.size / 2]
                var angle = Math.toDegrees(atan2(midPt[3].toDouble(), midPt[2].toDouble())).toFloat()
                var yOffset = -35f
                if (midPt[2] < 0) {
                    angle += 180f
                    yOffset = 45f
                }

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    textSize = 50f // FIXED SIZE
                    color = android.graphics.Color.parseColor("#00E5FF") // Cyan
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
                }

                canvas.save()
                canvas.translate(midPt[0], midPt[1])
                canvas.rotate(angle)
                canvas.drawText(textToDraw, 0f, yOffset, textPaint)
                canvas.restore()
            }

            // APPLY FADE OUT GRADIENT
            val gradient = LinearGradient(
                finalFadeStart.x, finalFadeStart.y,
                finalEnd.x, finalEnd.y,
                intArrayOf(Color.White.toArgb(), Color.Transparent.toArgb()),
                null,
                Shader.TileMode.CLAMP
            )
            paints.gradientMaskPaint.shader = gradient
            paints.gradientMaskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

            canvas.drawRect(-5000f, -5000f, 10000f, 10000f, paints.gradientMaskPaint)
        } finally {
            paints.gradientMaskPaint.shader = null
            paints.gradientMaskPaint.xfermode = null
            canvas.restoreToCount(layer)
            canvas.restore()
        }
    }

    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }

    private fun getSafeLogicalPoint(start: PointF, end: PointF, matrix: Matrix): PointF? {
        val v = FloatArray(9)
        matrix.getValues(v)
        val g = v[Matrix.MPERSP_0]
        val h = v[Matrix.MPERSP_1]
        val i = v[Matrix.MPERSP_2]

        val w1 = g * start.x + h * start.y + i
        val w2 = g * end.x + h * end.y + i

        val epsilon = 0.0001f

        if (w1 > epsilon && w2 > epsilon) {
            return end
        }

        if (w1 <= epsilon && w2 <= epsilon) {
            return null
        }

        if (w1 > epsilon && w2 <= epsilon) {
            val t = (epsilon - w1) / (w2 - w1)

            return PointF(
                start.x + t * (end.x - start.x),
                start.y + t * (end.y - start.y)
            )
        }

        return null
    }
}
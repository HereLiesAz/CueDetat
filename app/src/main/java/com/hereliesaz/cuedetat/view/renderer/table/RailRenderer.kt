// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.ui.theme.MonteCarlo
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Diamonds
import com.hereliesaz.cuedetat.view.config.table.Rail
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.util.createGlowPaint
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.pow

class RailRenderer {
    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val diamondSizeFactor = 0.5f
    private val railConfig = Rail()
    private val diamondConfig = Diamonds()
    private val textRenderer = LineTextRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (!state.table.isVisible || state.table.corners.size < 4) return

        val railLinePaint = paints.tableOutlinePaint
        val railLineGlowPaint = createGlowPaint(
            baseGlowColor = railConfig.glowColor,
            baseGlowWidth = railConfig.glowWidth,
            state = state
        )
        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = diamondConfig.fillColor.toArgb()
            alpha = (diamondConfig.opacity * 255).toInt()
        }

        val railOffsetAmount = LOGICAL_BALL_RADIUS * railVisualOffsetFromEdgeFactor
        val pocketRadius = LOGICAL_BALL_RADIUS * 1.8f

        val corners = state.table.corners

        val offsetCorners = corners.mapIndexed { index, corner ->
            // Inward normal for segment from previous corner to current corner
            val normal1 = normalize(PointF(corners[(index + 3) % 4].y - corner.y, corner.x - corners[(index + 3) % 4].x))
            // Inward normal for segment from current corner to next corner
            val nextCorner = corners[(index + 1) % 4]
            val normal2 = normalize(PointF(corner.y - nextCorner.y, nextCorner.x - corner.x))
            PointF(
                corner.x - (normal1.x + normal2.x) * railOffsetAmount,
                corner.y - (normal1.y + normal2.y) * railOffsetAmount
            )
        }

        // --- Draw Rail Segments ---
        val railSegments = listOf(
            getPointAlongLine(offsetCorners[0], offsetCorners[1], pocketRadius) to getPointAlongLine(offsetCorners[1], offsetCorners[0], pocketRadius),
            getPointAlongLine(offsetCorners[3], offsetCorners[2], pocketRadius) to getPointAlongLine(offsetCorners[2], offsetCorners[3], pocketRadius),
            getPointAlongLine(offsetCorners[0], offsetCorners[3], pocketRadius * 1.5f) to getPointAlongLine(offsetCorners[0], offsetCorners[3], state.table.logicalHeight / 2f - pocketRadius),
            getPointAlongLine(offsetCorners[3], offsetCorners[0], pocketRadius * 1.5f) to getPointAlongLine(offsetCorners[3], offsetCorners[0], state.table.logicalHeight / 2f - pocketRadius),
            getPointAlongLine(offsetCorners[1], offsetCorners[2], pocketRadius * 1.5f) to getPointAlongLine(offsetCorners[1], offsetCorners[2], state.table.logicalHeight / 2f - pocketRadius),
            getPointAlongLine(offsetCorners[2], offsetCorners[1], pocketRadius * 1.5f) to getPointAlongLine(offsetCorners[2], offsetCorners[1], state.table.logicalHeight / 2f - pocketRadius)
        )

        railSegments.forEach { (start, end) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, railLineGlowPaint)
            canvas.drawLine(start.x, start.y, end.x, end.y, railLinePaint)
        }

        // --- Draw Diamonds ---
        val diamondRadius = LOGICAL_BALL_RADIUS * diamondSizeFactor
        val diamondOffset = 50f
        val normals = state.table.normals

        for (i in 1..3) {
            val fraction = i / 4.0f
            val top = interpolate(offsetCorners[0], offsetCorners[1], fraction)
            val bottom = interpolate(offsetCorners[3], offsetCorners[2], fraction)

            drawDiamond(
                canvas,
                PointF(top.x + normals[0].x * diamondOffset, top.y + normals[0].y * diamondOffset),
                diamondRadius,
                diamondPaint
            )
            drawDiamond(
                canvas,
                PointF(
                    bottom.x + normals[2].x * diamondOffset,
                    bottom.y + normals[2].y * diamondOffset
                ),
                diamondRadius,
                diamondPaint
            )
        }

        for (i in 1..3) {
            // Upper half of side rails
            val leftTop = interpolate(offsetCorners[0], offsetCorners[3], i / 8.0f)
            val rightTop = interpolate(offsetCorners[1], offsetCorners[2], i / 8.0f)
            drawDiamond(
                canvas,
                PointF(
                    leftTop.x + normals[3].x * diamondOffset,
                    leftTop.y + normals[3].y * diamondOffset
                ),
                diamondRadius,
                diamondPaint
            )
            drawDiamond(
                canvas,
                PointF(
                    rightTop.x + normals[1].x * diamondOffset,
                    rightTop.y + normals[1].y * diamondOffset
                ),
                diamondRadius,
                diamondPaint
            )

            // Lower half of side rails
            val leftBottom = interpolate(offsetCorners[0], offsetCorners[3], (8 - i) / 8.0f)
            val rightBottom = interpolate(offsetCorners[1], offsetCorners[2], (8 - i) / 8.0f)
            drawDiamond(
                canvas,
                PointF(
                    leftBottom.x + normals[3].x * diamondOffset,
                    leftBottom.y + normals[3].y * diamondOffset
                ),
                diamondRadius,
                diamondPaint
            )
            drawDiamond(
                canvas,
                PointF(
                    rightBottom.x + normals[1].x * diamondOffset,
                    rightBottom.y + normals[1].y * diamondOffset
                ),
                diamondRadius,
                diamondPaint
            )
        }
    }

    private fun drawDiamond(canvas: Canvas, center: PointF, radius: Float, paint: Paint) {
        val path = Path()
        path.moveTo(center.x, center.y - radius) // Top
        path.lineTo(center.x + radius, center.y) // Right
        path.lineTo(center.x, center.y + radius) // Bottom
        path.lineTo(center.x - radius, center.y) // Left
        path.close()
        canvas.drawPath(path, paint)
    }

    fun drawRailLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val matrix = state.railPitchMatrix ?: return
        val referenceRadius = DrawingUtils.getPerspectiveRadiusAndLift(
            state.protractorUnit.center, state.protractorUnit.radius, state, matrix
        ).radius

        val textPaint = paints.textPaint.apply {
            this.typeface = typeface
            this.textSize = referenceRadius * 4f
            this.color = MonteCarlo.toArgb()
        }
        val textPadding = referenceRadius * 5f

        val pathToLabel = if (state.isBankingMode) state.bankShotPath else state.aimingLineBankPath
        pathToLabel?.let { path ->
            if (path.size > 1) {
                val bankPoint = path[1]
                getRailForPoint(bankPoint, state)?.let { railType ->
                    textRenderer.drawDiamondLabel(
                        canvas,
                        bankPoint,
                        railType,
                        state,
                        textPaint,
                        textPadding
                    )
                }
            }
        }

        if (!state.isBankingMode) {
            state.tangentLineBankPath?.let { path ->
                if (path.size > 1) {
                    val tangentBankPoint = path[1]
                    getRailForPoint(tangentBankPoint, state)?.let { railType ->
                        textRenderer.drawDiamondLabel(
                            canvas,
                            tangentBankPoint,
                            railType,
                            state,
                            textPaint,
                            textPadding
                        )
                    }
                }
            }
        }


        if (!state.isBankingMode) {
            state.shotGuideImpactPoint?.let { impactPoint ->
                getRailForPoint(impactPoint, state)?.let { railType ->
                    textRenderer.drawDiamondLabel(canvas, impactPoint, railType, state, textPaint, textPadding)
                }
            }
        }
    }

    private fun getRailForPoint(point: PointF, state: OverlayState): LineTextRenderer.RailType? {
        val table = state.table
        if (!table.isVisible) return null

        val corners = table.corners
        val tolerance = 5f

        if (pointToSegmentDistance(point, corners[0], corners[1]) < tolerance) return LineTextRenderer.RailType.TOP
        if (pointToSegmentDistance(point, corners[1], corners[2]) < tolerance) return LineTextRenderer.RailType.RIGHT
        if (pointToSegmentDistance(point, corners[2], corners[3]) < tolerance) return LineTextRenderer.RailType.BOTTOM
        if (pointToSegmentDistance(point, corners[3], corners[0]) < tolerance) return LineTextRenderer.RailType.LEFT

        return null
    }

    private fun pointToSegmentDistance(p: PointF, v: PointF, w: PointF): Float {
        val l2 = (v.x - w.x).pow(2) + (v.y - w.y).pow(2)
        if (l2 == 0f) return kotlin.math.hypot(p.x - v.x, p.y - v.y)
        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
        t = t.coerceIn(0f, 1f)
        val projectionX = v.x + t * (w.x - v.x)
        val projectionY = v.y + t * (w.y - v.y)
        return kotlin.math.hypot(p.x - projectionX, p.y - projectionY)
    }

    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x, p.y)
        return if (mag > 0) PointF(p.x / mag.toFloat(), p.y / mag.toFloat()) else PointF(0f, 0f)
    }

    private fun getPointAlongLine(start: PointF, end: PointF, distance: Float): PointF {
        val vector = PointF(end.x - start.x, end.y - start.y)
        val mag = kotlin.math.hypot(vector.x, vector.y)
        val unitVector = if (mag > 0) PointF(vector.x / mag, vector.y / mag) else PointF(0f, 0f)
        return PointF(start.x + unitVector.x * distance, start.y + unitVector.y * distance)
    }

    private fun interpolate(p1: PointF, p2: PointF, fraction: Float): PointF {
        return PointF(p1.x + (p2.x - p1.x) * fraction, p1.y + (p2.y - p1.y) * fraction)
    }
}
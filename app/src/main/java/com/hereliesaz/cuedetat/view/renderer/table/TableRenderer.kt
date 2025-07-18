// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.cos
import kotlin.math.sin

class TableRenderer {
    private val pocketSizeFactor = 1.8f
    private val cornerPocketAngle = 45.0
    private val sidePocketAngle = 90.0

    fun drawSurface(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.table.isVisible || state.table.corners.size < 4) return

        val tablePath = Path()
        val corners = state.table.corners
        tablePath.moveTo(corners[0].x, corners[0].y)
        for (i in 1 until corners.size) {
            tablePath.lineTo(corners[i].x, corners[i].y)
        }
        tablePath.close()

        canvas.drawPath(tablePath, paints.tableFillPaint)
        canvas.drawPath(tablePath, paints.tableOutlinePaint)

        // Draw grid lines if helpers are visible
        if (state.areHelpersVisible) {
            val gridPaint = Paint(paints.tableOutlinePaint).apply {
                alpha = 50
                strokeWidth = 1f
            }
            val width = state.table.logicalWidth
            val height = state.table.logicalHeight
            val halfW = width / 2f
            val halfH = height / 2f

            // Center lines
            canvas.drawLine(0f, -halfH, 0f, halfH, gridPaint)
            canvas.drawLine(-halfW, 0f, halfW, 0f, gridPaint)

            // Quarter lines
            canvas.drawLine(-halfW / 2f, -halfH, -halfW / 2f, halfH, gridPaint)
            canvas.drawLine(halfW / 2f, -halfH, halfW / 2f, halfH, gridPaint)
            canvas.drawLine(-halfW, -halfH / 2f, halfW, -halfH / 2f, gridPaint)
            canvas.drawLine(-halfW, halfH / 2f, halfW, halfH / 2f, gridPaint)
        }
    }

    fun drawPockets(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.table.isVisible) return

        val pocketRadius = LOGICAL_BALL_RADIUS * pocketSizeFactor
        val pocketGlowPaint = Paint(paints.ballGlowPaint)

        state.table.pockets.forEachIndexed { index, pocket ->
            val angle = when (index) {
                0 -> -cornerPocketAngle + 180
                1 -> cornerPocketAngle + 180
                2 -> cornerPocketAngle
                3 -> -cornerPocketAngle
                4 -> sidePocketAngle
                5 -> -sidePocketAngle
                else -> 0.0
            }
            drawPocket(canvas, pocket.x, pocket.y, pocketRadius, angle, paints.pocketPaint, pocketGlowPaint, paints)
        }

        // Draw highlights on aimed pockets
        state.aimedPocketIndex?.let {
            val pocket = state.table.pockets[it]
            val aimedPaint = Paint(paints.pocketPaint).apply { color = AcidPatina.toArgb() }
            val aimedGlow = Paint(paints.ballGlowPaint).apply { color = AcidPatina.toArgb() }
            drawPocket(canvas, pocket.x, pocket.y, pocketRadius, 0.0, aimedPaint, aimedGlow, paints, isFullCircle = true)
        }
        state.tangentAimedPocketIndex?.let {
            val pocket = state.table.pockets[it]
            val aimedPaint = Paint(paints.pocketPaint).apply { color = AcidPatina.copy(alpha = 0.5f).toArgb() }
            val aimedGlow = Paint(paints.ballGlowPaint).apply { color = AcidPatina.copy(alpha = 0.5f).toArgb() }
            drawPocket(canvas, pocket.x, pocket.y, pocketRadius, 0.0, aimedPaint, aimedGlow, paints, isFullCircle = true)
        }
    }

    private fun drawPocket(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        angleDegrees: Double,
        fillPaint: Paint,
        glowPaint: Paint,
        paints: PaintCache,
        isFullCircle: Boolean = false
    ) {
        canvas.drawCircle(cx, cy, radius, glowPaint)
        canvas.drawCircle(cx, cy, radius, fillPaint)
        if (!isFullCircle) {
            val angleRad = Math.toRadians(angleDegrees)
            val coverX = cx + (radius * 2 * cos(angleRad)).toFloat()
            val coverY = cy + (radius * 2 * sin(angleRad)).toFloat()
            canvas.drawCircle(coverX, coverY, radius, paints.tableFillPaint)
        }
    }
}
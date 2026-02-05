// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Holes

/**
 * Responsible for rendering the virtual pool table surface, its boundaries, grid lines, and pockets.
 *
 * This renderer operates purely in the 2D "Logical Plane". The `canvas` passed to it typically
 * already has the `state.logicalPlaneMatrix` applied (via `TableRenderer.drawSurface` call site)
 * OR is drawn manually using logical coordinates.
 *
 * NOTE: The "Surface" here implies the felt. The "Rails" are rendered separately by [RailRenderer]
 * because rails have height (3D lift), whereas the surface is flat (Z=0).
 */
class TableRenderer {

    companion object {
        /**
         * Returns the logical coordinates of the 6 pockets.
         * Used by hit-testing logic in [UpdateStateUseCase] as well as rendering.
         */
        fun getLogicalPockets(state: CueDetatState): List<PointF> {
            return state.table.pockets
        }
    }

    /**
     * Draws the table outline and the "Diamond System" grid.
     *
     * @param canvas The canvas to draw on.
     * @param state The current state (containing table dimensions).
     * @param paints The shared paint cache.
     */
    fun drawSurface(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        // If the table isn't detected or enabled, don't draw the ghostly grid.
        if (!state.table.isVisible) return

        // The corners are defined in Logical Inches relative to the table center (0,0).
        val corners = state.table.corners
        if (corners.size < 4) return // Safety check.

        val tableOutlinePaint = paints.tableOutlinePaint
        val diamondGridPaint = paints.gridLinePaint

        // 1. Draw the Table Outline (The Bed).
        // This is a simple rectangle connecting the 4 corners.
        val path = Path()
        path.moveTo(corners[0].x, corners[0].y)
        path.lineTo(corners[1].x, corners[1].y)
        path.lineTo(corners[2].x, corners[2].y)
        path.lineTo(corners[3].x, corners[3].y)
        path.close()
        canvas.drawPath(path, tableOutlinePaint)

        // 2. Draw the Diamond System Grid.
        // The "Diamond System" is a standard pool calculation method using diamonds on the rails.
        // We draw faint lines connecting these diamonds to help the user visualize banking angles.
        // A standard table is 2:1 aspect ratio.
        // Length = 8 segments (7 lines). Width = 4 segments (3 lines).

        // Vertical lines (dividing the width into 4 quarters).
        for (i in 1..3) {
            val fraction = i / 4.0f
            // Linear interpolation between Top-Left and Top-Right
            val top = interpolate(corners[0], corners[1], fraction)
            // Linear interpolation between Bottom-Left and Bottom-Right
            val bottom = interpolate(corners[3], corners[2], fraction)
            canvas.drawLine(top.x, top.y, bottom.x, bottom.y, diamondGridPaint)
        }

        // Horizontal lines (dividing the length into 8 eighths).
        for (i in 1..7) {
            val fraction = i / 8.0f
            // Linear interpolation between Top-Left and Bottom-Left
            val left = interpolate(corners[0], corners[3], fraction)
            // Linear interpolation between Top-Right and Bottom-Right
            val right = interpolate(corners[1], corners[2], fraction)
            canvas.drawLine(left.x, left.y, right.x, right.y, diamondGridPaint)
        }
    }

    /**
     * Linear interpolation helper.
     * Returns a point [fraction] of the way from [p1] to [p2].
     */
    private fun interpolate(p1: PointF, p2: PointF, fraction: Float): PointF {
        return PointF(p1.x + (p2.x - p1.x) * fraction, p1.y + (p2.y - p1.y) * fraction)
    }

    /**
     * Draws the 6 pockets.
     * Pockets react to aiming: they light up if a calculated shot path ends in them.
     */
    fun drawPockets(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        if (!state.table.isVisible) return

        // Determine pocket size based on ball size.
        // Standard pocket is ~1.8x - 2.0x ball diameter (but here we use radius ratio).
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val holesConfig = Holes() // Visual config (colors).
        val pockets = getLogicalPockets(state)
        val pocketRadius = referenceRadius * 1.8f

        // Pre-allocate paints for different states (Normal, Aimed, Banked).
        val pocketedPaintWhite =
            Paint(paints.pocketFillPaint).apply { color = android.graphics.Color.WHITE }
        val pocketedPaintRed = Paint(paints.pocketFillPaint).apply { color = WarningRed.toArgb() }
        val pocketOutlinePaint = Paint(paints.tableOutlinePaint).apply {
            color = holesConfig.strokeColor.toArgb()
            strokeWidth = holesConfig.strokeWidth
        }
        val pocketFillPaint = Paint(paints.pocketFillPaint).apply {
            color = holesConfig.fillColor.toArgb()
        }

        // Draw each pocket.
        pockets.forEachIndexed { index, pos ->
            // Check if this pocket is the destination of a calculated shot.
            val isAimedAtByAimingLine = state.aimedPocketIndex == index && !state.isBankingMode
            val isAimedAtByTangentLine =
                state.tangentAimedPocketIndex == index && !state.isBankingMode
            val isPocketedInBank = state.isBankingMode && index == state.pocketedBankShotPocketIndex

            // Select color based on state.
            val fillPaint = when {
                isAimedAtByAimingLine || isPocketedInBank -> pocketedPaintWhite // Success!
                isAimedAtByTangentLine -> pocketedPaintRed // Scratch / Tangent Hazard?
                else -> pocketFillPaint // Idle
            }

            // Draw.
            canvas.drawCircle(pos.x, pos.y, pocketRadius, fillPaint)
            canvas.drawCircle(pos.x, pos.y, pocketRadius, pocketOutlinePaint)
        }
    }
}

// app/src/main/java/com/hereliesaz/cuedetat/domain/EdgeGeometryFitter.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.*

/**
 * Calculates AR pose refinements based on detected felt edges (quadrilateral corners).
 * This allows the table to "snap" into alignment with the physical felt boundaries
 * without waiting for 6 independent stable pocket clusters.
 */
object EdgeGeometryFitter {

    data class Refinement(
        val translationDelta: PointF,
        val rotationDelta: Float,
        val scaleFactor: Float
    )

    /**
     * Estimates the correction needed to align the virtual table with detected edges.
     * @param detectedLogicalPoints 4 corners of the felt quadrilateral in logical space.
     * @param table The current logical table model.
     * @return A Refinement delta to be applied to the current pose.
     */
    fun fitEdges(
        detectedLogicalPoints: List<PointF>,
        tableLogicalWidth: Float,
        tableLogicalHeight: Float
    ): Refinement? {
        if (detectedLogicalPoints.size < 4) return null

        // 1. Calculate centroid of detected points to find translation error
        val centroidX = detectedLogicalPoints.sumOf { it.x.toDouble() }.toFloat() / detectedLogicalPoints.size
        val centroidY = detectedLogicalPoints.sumOf { it.y.toDouble() }.toFloat() / detectedLogicalPoints.size
        
        // 2. Sort detected points to match logical [TL, TR, BR, BL]
        val sorted = orderPoints(detectedLogicalPoints, centroidX, centroidY) ?: return null
        
        // 3. Estimate Rotation
        // Compute average angle of the 'long' edges (rails) relative to horizontal
        val topEdgeAngle = atan2(sorted[1].y - sorted[0].y, sorted[1].x - sorted[0].x)
        val bottomEdgeAngle = atan2(sorted[2].y - sorted[3].y, sorted[2].x - sorted[3].x)
        val avgAngle = (topEdgeAngle + bottomEdgeAngle) / 2f
        val rotationDeg = Math.toDegrees(avgAngle.toDouble()).toFloat()

        // 4. Estimate Scale
        val detectedWidth = (dist(sorted[0], sorted[1]) + dist(sorted[3], sorted[2])) / 2f
        val detectedHeight = (dist(sorted[0], sorted[3]) + dist(sorted[1], sorted[2])) / 2f
        
        val scaleW = detectedWidth / tableLogicalWidth
        val scaleH = detectedHeight / tableLogicalHeight
        val scale = (scaleW + scaleH) / 2f

        // We only return deltas. The caller decides whether to 'snap' or 'sink' (smoothly interpolate).
        return Refinement(
            translationDelta = PointF(centroidX, centroidY),
            rotationDelta = rotationDeg,
            scaleFactor = scale
        )
    }

    private fun orderPoints(pts: List<PointF>, cx: Float, cy: Float): List<PointF>? {
        // Simple 4-way sort for rectangle corners
        val tl = pts.minByOrNull { (it.x - cx) + (it.y - cy) } ?: return null
        val br = pts.maxByOrNull { (it.x - cx) + (it.y - cy) } ?: return null
        val tr = pts.minByOrNull { -(it.x - cx) + (it.y - cy) } ?: return null
        val bl = pts.maxByOrNull { -(it.x - cx) + (it.y - cy) } ?: return null
        if (setOf(tl, tr, br, bl).size != 4) return null
        return listOf(tl, tr, br, bl)
    }

    private fun dist(a: PointF, b: PointF) =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
}

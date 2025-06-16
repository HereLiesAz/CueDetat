// app/src/main/java/com/hereliesaz/cuedetat/view/model/LogicalTable.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import android.graphics.RectF

/**
 * Defines the standard sizes for a pool table. The dimensions are in inches.
 * The playing surface is what matters for physics calculations.
 */
enum class TableSize(
    val playingSurfaceWidth: Float,
    val playingSurfaceHeight: Float,
    val displayName: String
) {
    SIX_FOOT(37f, 74f, "6'"),
    SEVEN_FOOT(44f, 88f, "7'"), // Default
    EIGHT_FOOT(50f, 100f, "8'"),
    NINE_FOOT(56f, 112f, "9'");

    fun getNext(): TableSize {
        return entries[(this.ordinal + 1) % entries.size]
    }
}

/**
 * Represents the geometric definition of the pool table on the Logical Plane.
 * All coordinates are relative to the table's center, which is considered (0,0).
 */
data class PoolTable(
    val size: TableSize = TableSize.SEVEN_FOOT,
    val center: PointF = PointF(0f, 0f),
    val rotationDegrees: Float = 0f
) {
    val playingSurface: RectF
        get() = RectF(
            -size.playingSurfaceWidth / 2f,
            -size.playingSurfaceHeight / 2f,
            size.playingSurfaceWidth / 2f,
            size.playingSurfaceHeight / 2f
        )

    // Pockets are defined relative to the playing surface.
    // A standard pocket is ~4.5 inches in diameter. We'll use a radius of 2.25f.
    val pocketRadius: Float = 2.25f

    val pockets: List<PointF>
        get() {
            val halfWidth = playingSurface.width() / 2f
            val halfHeight = playingSurface.height() / 2f
            return listOf(
                // Corners
                PointF(-halfWidth, -halfHeight),
                PointF(halfWidth, -halfHeight),
                PointF(-halfWidth, halfHeight),
                PointF(halfWidth, halfHeight),
                // Sides
                PointF(0f, -halfHeight),
                PointF(0f, halfHeight)
            )
        }
}

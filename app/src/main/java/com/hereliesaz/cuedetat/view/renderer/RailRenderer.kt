package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {

    companion object {
        // SVG path data containing ONLY the outer rail shape, with the inner line removed.
        private const val SVG_RAIL_PATH_DATA =
            "M2186 2727 c-10 -7 -23 -38 -29 -70 l-12 -58 -767 1 -768 0 -67 55 c-78 63 -113 70 -139 30 -21 -33 -11 -58 52 -129 l46 -51 -1 -685 c-1 -377 -4 -720 -7 -762 -7 -77 -7 -78 -67 -148 -57 -67 -59 -71 -47 -100 7 -16 20 -32 30 -35 21 -7 70 4 70 15 0 4 27 26 59 49 l59 41 771 0 771 0 0 -47 c0 -54 5 -65 37 -82 57 -29 97 0 107 79 l7 53 282 -7 c155 -3 506 -6 779 -6 l497 0 68 -60 c76 -66 110 -74 143 -32 26 32 12 66 -55 132 l-55 54 0 766 0 766 46 44 c25 25 54 59 65 77 19 30 19 34 4 63 -12 24 -22 30 -49 30 -26 0 -46 -12 -93 -55 l-61 -55 -778 0 -779 -1 -8 48 c-13 77 -64 114 -111 80z"
    }

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f
    private var cachedRailPath: Path? = null

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (cachedRailPath == null) {
            cachedRailPath = PathParser.createPathFromPathData(SVG_RAIL_PATH_DATA)
        }
        val railPath = Path(cachedRailPath) // Work with a copy

        val ballRadius = state.protractorUnit.radius
        if (ballRadius <= 0) return

        // Define the target size and position for the rails on the logical canvas
        val tableHeight = tableToBallRatioShort * ballRadius
        val tableWidth = tableToBallRatioLong * ballRadius
        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f

        val targetBounds = RectF(
            tableCenterX - tableWidth / 2,
            tableCenterY - tableHeight / 2,
            tableCenterX + tableWidth / 2,
            tableCenterY + tableHeight / 2
        )

        // Get the original bounds from the parsed SVG path
        val originalBounds = RectF()
        railPath.computeBounds(originalBounds, true)

        // Create a matrix to scale and translate the SVG path to fit the target bounds
        val matrix = Matrix()
        matrix.setRectToRect(originalBounds, targetBounds, Matrix.ScaleToFit.FILL)
        railPath.transform(matrix)


        // --- Draw Rails and Diamonds ---
        canvas.drawPath(railPath, paints.tableOutlinePaint)

        // --- Draw Diamonds relative to the table bounds ---
        val diamondRadius = ballRadius / 4f
        val diamondPaint = Paint(paints.tableOutlinePaint).apply {
            style = Paint.Style.FILL_AND_STROKE
        }
        val railOffset = ballRadius * 3.0f

        // Diamonds on the long rails (top and bottom)
        for (i in 1..3) {
            val xPos = targetBounds.left + (tableWidth * (i / 4f))
            canvas.drawCircle(xPos, targetBounds.top - railOffset, diamondRadius, diamondPaint)
            canvas.drawCircle(xPos, targetBounds.bottom + railOffset, diamondRadius, diamondPaint)
        }
        // Diamonds on the short rails (left and right)
        canvas.drawCircle(targetBounds.left - railOffset, tableCenterY, diamondRadius, diamondPaint)
        canvas.drawCircle(
            targetBounds.right + railOffset,
            tableCenterY,
            diamondRadius,
            diamondPaint
        )
    }
}
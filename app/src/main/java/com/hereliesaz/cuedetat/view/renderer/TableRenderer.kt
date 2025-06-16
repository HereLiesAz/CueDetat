package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    // Standard 2:1 ratio for pool tables.
    // Size is based on ball radius. A standard 9ft table (100"x50") and a 2.25" ball
    // gives a play surface width of ~44 ball diameters. We'll use radius, so ~88.
    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val ballRadius = state.protractorUnit.radius
        if (ballRadius <= 0) return

        // Calculate table dimensions based on the current ball radius to maintain proportion
        val tableHeight = tableToBallRatioShort * ballRadius
        val tableWidth = tableToBallRatioLong * ballRadius
        // Position the table in the center of the view for stability
        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f

        val left = tableCenterX - tableWidth / 2
        val top = tableCenterY - tableHeight / 2
        val right = tableCenterX + tableWidth / 2
        val bottom = tableCenterY + tableHeight / 2

        // Draw the main table outline
        canvas.drawRect(left, top, right, bottom, paints.tableOutlinePaint)

        // --- Draw Holes ---
        // Pockets are roughly twice the diameter of a ball
        val pocketRadius = ballRadius * 2f
        val holePaint = paints.tableOutlinePaint
        // Corner Pockets
        canvas.drawCircle(left, top, pocketRadius, holePaint)
        canvas.drawCircle(right, top, pocketRadius, holePaint)
        canvas.drawCircle(left, bottom, pocketRadius, holePaint)
        canvas.drawCircle(right, bottom, pocketRadius, holePaint)
        // Side Pockets
        canvas.drawCircle(tableCenterX, top, pocketRadius, holePaint)
        canvas.drawCircle(tableCenterX, bottom, pocketRadius, holePaint)
    }
}
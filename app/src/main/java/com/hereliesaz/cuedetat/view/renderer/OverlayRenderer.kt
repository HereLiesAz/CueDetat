// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()
    private val cvDebugRenderer = CvDebugRenderer()

    // Cache for the static table elements
    private var tableBitmap: Bitmap? = null
    private var lastTableStateHash: Int? = null

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // If the mask is to be shown (either in test or calibration), draw only it and nothing else.
        if (state.showCvMask) {
            cvDebugRenderer.draw(canvas, state)
            return
        }

        // If we are calibrating but not yet showing the mask, draw nothing but the camera feed.
        if (state.isCalibratingColor) {
            return
        }

        // --- Performance Optimization: Caching LOGICAL table geometry ---
        val currentTableStateHash = calculateTableStateHash(state)
        val pitchMatrix = state.pitchMatrix ?: return // The primary matrix is required.

        if (tableBitmap == null || lastTableStateHash != currentTableStateHash || tableBitmap?.width != canvas.width || tableBitmap?.height != canvas.height) {
            lastTableStateHash = currentTableStateHash
            // Invalidate and recreate the bitmap for the LOGICAL, UN-TRANSFORMED table.
            tableBitmap?.recycle()
            tableBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            val tableCanvas = Canvas(tableBitmap!!)
            tableCanvas.drawColor(0, PorterDuff.Mode.CLEAR)

            // Center the canvas on the logical origin (0,0) to draw the static table.
            tableCanvas.save()
            tableCanvas.translate(tableCanvas.width / 2f, tableCanvas.height / 2f)

            // Pass 1: Draw Table Surface onto the bitmap
            if (state.table.isVisible) {
                tableRenderer.drawSurface(tableCanvas, state, paints)
            }
            // Pass 2: Draw Rails and their labels onto the bitmap
            if (state.table.isVisible) {
                railRenderer.draw(tableCanvas, state, paints, typeface)
                railRenderer.drawRailLabels(tableCanvas, state, paints, typeface)
            }
            tableCanvas.restore()
        }

        // Draw the cached, un-transformed bitmap, applying the dynamic pitch matrix.
        // This is a single, hardware-accelerated operation.
        tableBitmap?.let {
            canvas.drawBitmap(it, pitchMatrix, paints.bitmapPaint)
        }
        // --- End of Caching Logic ---


        // Pass 3: Draw dynamic elements that live on the table plane (pockets, lines)
        canvas.save()
        canvas.concat(pitchMatrix)
        if (state.table.isVisible) {
            tableRenderer.drawPockets(canvas, state, paints)
        }
        lineRenderer.drawLogicalLines(canvas, state, paints, typeface)
        canvas.restore()

        // Pass 4: Draw all balls, which handle their own "lift" effect.
        ballRenderer.draw(canvas, state, paints, typeface)
    }


    /**
     * Generates a hash code based on the state properties that affect the
     * static table's appearance. This hash must NOT include dynamic properties
     * like the pitch matrix or aiming state.
     */
    private fun calculateTableStateHash(state: OverlayState): Int {
        var result = state.viewWidth
        result = 31 * result + state.viewHeight
        // HASH LOGICAL AND APPEARANCE PROPERTIES ONLY
        result = 31 * result + state.table.hashCode()
        result = 31 * result + state.areHelpersVisible.hashCode()
        result = 31 * result + state.luminanceAdjustment.hashCode()
        result = 31 * result + state.glowStickValue.hashCode()
        result = 31 * result + (state.appControlColorScheme?.hashCode() ?: 0)
        return result
    }
}
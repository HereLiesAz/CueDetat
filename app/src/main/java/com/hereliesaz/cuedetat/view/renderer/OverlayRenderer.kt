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

        // --- Performance Optimization: Bitmap Caching for Static Table ---
        val currentTableStateHash = calculateTableStateHash(state)
        if (tableBitmap == null || lastTableStateHash != currentTableStateHash || tableBitmap?.width != canvas.width || tableBitmap?.height != canvas.height) {
            lastTableStateHash = currentTableStateHash
            // Invalidate and recreate the bitmap
            tableBitmap?.recycle()
            tableBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            val tableCanvas = Canvas(tableBitmap!!)

            // Clear the bitmap canvas
            tableCanvas.drawColor(0, PorterDuff.Mode.CLEAR)

            // Pass 1: Draw Table Surface onto the bitmap
            if (state.table.isVisible) {
                tableCanvas.save()
                tableCanvas.concat(state.pitchMatrix)
                tableRenderer.drawSurface(tableCanvas, state, paints)
                tableCanvas.restore()
            }
            // Pass 2: Draw Lifted Rails and their labels onto the bitmap
            if (state.table.isVisible) {
                tableCanvas.save()
                tableCanvas.concat(state.railPitchMatrix)
                railRenderer.draw(tableCanvas, state, paints, typeface)
                railRenderer.drawRailLabels(tableCanvas, state, paints, typeface)
                tableCanvas.restore()
            }
            // Pass 3: Draw Pockets (on top of lines) onto the bitmap
            if (state.table.isVisible) {
                tableCanvas.save()
                tableCanvas.concat(state.pitchMatrix)
                tableRenderer.drawPockets(tableCanvas, state, paints)
                tableCanvas.restore()
            }
        }

        // Draw the cached table bitmap onto the main canvas
        tableBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        // --- End of Caching Logic ---


        // Pass 4: Draw all dynamic logical lines on the main canvas
        canvas.save()
        canvas.concat(state.pitchMatrix)
        lineRenderer.drawLogicalLines(canvas, state, paints, typeface)
        canvas.restore()

        // Pass 5: Draw all dynamic balls on the main canvas
        ballRenderer.draw(canvas, state, paints, typeface)
    }

    /**
     * Generates a hash code based on the state properties that affect the
     * static table's appearance. If this hash changes, the table bitmap needs
     * to be redrawn.
     */
    private fun calculateTableStateHash(state: OverlayState): Int {
        var result = state.viewWidth
        result = 31 * result + state.viewHeight
        result = 31 * result + (state.pitchMatrix?.hashCode() ?: 0)
        result = 31 * result + (state.railPitchMatrix?.hashCode() ?: 0)
        result = 31 * result + state.table.hashCode()
        result = 31 * result + state.areHelpersVisible.hashCode()
        result = 31 * result + state.luminanceAdjustment.hashCode()
        result = 31 * result + state.glowStickValue.hashCode()
        result = 31 * result + (state.aimedPocketIndex ?: -1)
        result = 31 * result + (state.tangentAimedPocketIndex ?: -1)
        result = 31 * result + (state.pocketedBankShotPocketIndex ?: -1)
        return result
    }
}
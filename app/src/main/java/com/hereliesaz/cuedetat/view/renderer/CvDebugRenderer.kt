// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/CvDebugRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.domain.CueDetatState
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class CvDebugRenderer {

    private val maskPaint = Paint().apply {
        alpha = 100 // Set transparency for the overlay
    }
    private var bmp: Bitmap? = null

    fun draw(canvas: Canvas, state: CueDetatState) {
        if (!state.showCvMask) return
        val maskMat = state.visionData?.cvMask ?: return

        // Prevent crash if the Mat is empty for a frame
        if (maskMat.empty()) return

        try {
            // Ensure bitmap is the correct size, or create it
            if (bmp == null || bmp?.width != maskMat.cols() || bmp?.height != maskMat.rows()) {
                bmp?.recycle() // Recycle old bitmap
                bmp = Bitmap.createBitmap(maskMat.cols(), maskMat.rows(), Bitmap.Config.ARGB_8888)
            }

            // Convert the Mat to a colored Mat to draw on canvas
            val coloredMask = Mat()
            Imgproc.cvtColor(maskMat, coloredMask, Imgproc.COLOR_GRAY2BGRA)

            // Convert the colored Mat to a Bitmap
            Utils.matToBitmap(coloredMask, bmp)

            // Draw the bitmap scaled to the full canvas size
            bmp?.let {
                val destWidth = canvas.width.toFloat()
                val destHeight = canvas.height.toFloat()
                val srcWidth = it.width.toFloat()
                val srcHeight = it.height.toFloat()

                val matrix = android.graphics.Matrix()
                matrix.setScale(destWidth / srcWidth, destHeight / srcHeight)
                canvas.drawBitmap(it, matrix, maskPaint)
            }

            // Release the Mats to prevent memory leaks
            coloredMask.release()
            // The original maskMat should not be released here, as it's part of the state
            // and might be used elsewhere. Let the state management handle its lifecycle.
        } catch (e: Exception) {
            // Handle potential exceptions from bitmap creation or mat conversion
        }
    }
}
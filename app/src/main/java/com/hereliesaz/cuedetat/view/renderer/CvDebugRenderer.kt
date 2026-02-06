// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/CvDebugRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.domain.CueDetatState
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Renderer responsible for drawing debug visualizations from the Computer Vision system.
 *
 * Specifically, it draws the binary mask (the black and white image showing what the CV algorithm "sees")
 * as an overlay on the screen to help the user tune thresholds.
 */
class CvDebugRenderer {

    /** Paint used to draw the bitmap. Sets transparency to allow seeing the camera feed underneath. */
    private val maskPaint = Paint().apply {
        alpha = 100 // Set alpha to ~40% (100/255) for the overlay.
    }

    /** Reusable Bitmap to avoid frequent allocations during the render loop. */
    private var bmp: Bitmap? = null

    /**
     * Draws the CV debug mask onto the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param state The current state containing the latest vision data.
     */
    fun draw(canvas: Canvas, state: CueDetatState) {
        // Exit early if the mask overlay is not enabled in settings.
        if (!state.showCvMask) return

        // Get the latest mask Mat (matrix) from the vision data.
        val maskMat = state.visionData?.cvMask ?: return

        // Prevent crash if the Mat is empty or invalid for a frame.
        if (maskMat.empty()) return

        try {
            // Ensure the reusable bitmap exists and matches the dimensions of the incoming Mat.
            if (bmp == null || bmp?.width != maskMat.cols() || bmp?.height != maskMat.rows()) {
                bmp?.recycle() // Explicitly recycle the old bitmap to free native memory.
                // Create a new bitmap with ARGB_8888 config (standard for Android drawing).
                bmp = Bitmap.createBitmap(maskMat.cols(), maskMat.rows(), Bitmap.Config.ARGB_8888)
            }

            // The mask is likely a single-channel grayscale Mat (CV_8UC1).
            // We need to convert it to a 4-channel BGRA Mat (CV_8UC4) to map it to a Bitmap.
            val coloredMask = Mat()
            Imgproc.cvtColor(maskMat, coloredMask, Imgproc.COLOR_GRAY2BGRA)

            // Convert the OpenCV Mat to the Android Bitmap.
            Utils.matToBitmap(coloredMask, bmp)

            // Draw the bitmap onto the canvas.
            bmp?.let {
                // Calculate scaling factors to stretch the bitmap to fill the view.
                // The CV frame (e.g., 640x480) might differ from the Screen size (e.g., 1080x1920).
                val destWidth = canvas.width.toFloat()
                val destHeight = canvas.height.toFloat()
                val srcWidth = it.width.toFloat()
                val srcHeight = it.height.toFloat()

                // Create a matrix for the scale transformation.
                val matrix = android.graphics.Matrix()
                matrix.setScale(destWidth / srcWidth, destHeight / srcHeight)

                // Draw the scaled bitmap with the semi-transparent paint.
                canvas.drawBitmap(it, matrix, maskPaint)
            }

            // Release the temporary colored Mat immediately to prevent native memory leaks.
            coloredMask.release()
            // Note: We do NOT release 'maskMat' here, as it belongs to the 'visionData' object
            // held in the state. The repository managing that data is responsible for its lifecycle.
        } catch (e: Exception) {
            // Catch generic exceptions to prevent the app from crashing during rendering glitches.
            // In production, this should probably log the error.
        }
    }
}

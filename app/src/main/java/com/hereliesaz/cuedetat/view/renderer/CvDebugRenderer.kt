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

    /** Reusable Mat for the BGRA-converted mask, to avoid a fresh allocation every frame. */
    private val coloredMask = Mat()

    /** Reused mask→view transform. */
    private val maskMatrix = android.graphics.Matrix()

    /** Frees the native Mat and Bitmap. The renderer must not be drawn with afterwards. */
    fun release() {
        coloredMask.release()
        bmp?.recycle()
        bmp = null
    }

    companion object {
        private const val TAG = "CvDebugRenderer"
    }

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
            // 'coloredMask' is a reused class-level field; cvtColor overwrites its contents
            // in place, so no fresh Mat is allocated per frame.
            Imgproc.cvtColor(maskMat, coloredMask, Imgproc.COLOR_GRAY2BGRA)

            // Convert the OpenCV Mat to the Android Bitmap.
            Utils.matToBitmap(coloredMask, bmp)

            // Draw the bitmap onto the canvas.
            bmp?.let {
                // Same transform BallRenderer.drawBoundingBoxes uses for the same frames, so the
                // mask lines up with the preview and the boxes: CameraBackground's FILL_CENTER is
                // centre-crop with the sensor rotation (uniform scale, no stretching).
                val destWidth = canvas.width.toFloat()
                val destHeight = canvas.height.toFloat()
                val srcWidth = it.width.toFloat()
                val srcHeight = it.height.toFloat()
                val rotation = (state.visionData?.sourceImageRotation ?: 0).toFloat()
                val rotatedW = if (rotation % 180f == 0f) srcWidth else srcHeight
                val rotatedH = if (rotation % 180f == 0f) srcHeight else srcWidth
                val scale = maxOf(destWidth / rotatedW, destHeight / rotatedH)

                maskMatrix.reset()
                maskMatrix.postTranslate(-srcWidth / 2f, -srcHeight / 2f)
                maskMatrix.postRotate(rotation)
                maskMatrix.postScale(scale, scale)
                maskMatrix.postTranslate(destWidth / 2f, destHeight / 2f)

                canvas.drawBitmap(it, maskMatrix, maskPaint)
            }

            // Note: We do NOT release 'coloredMask' here, since it is a reused class-level
            // field (see above), not a per-call allocation. We also do NOT release 'maskMat'
            // here, as it belongs to the 'visionData' object held in the state. The repository
            // managing that data is responsible for its lifecycle.
        } catch (e: Exception) {
            // Catch generic exceptions to prevent the app from crashing during rendering glitches.
            android.util.Log.e(TAG, "CV debug mask draw failed", e)
        }
    }
}

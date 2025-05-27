// app/src/main/java/com/hereliesaz/cuedetat/tracking/ball_detector/BallDetector.kt
package com.hereliesaz.cuedetat.tracking.ball_detector

import android.content.Context
import android.graphics.Bitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class BallDetector(private val context: Context,
                   private val hsvRanges: List<HSVRange>? = null, // Optional, if specific colors needed
                   private val minBallRadius: Int = 20, // Min pixel radius for a detected ball
                   private val maxBallRadius: Int = 100) { // Max pixel radius for a detected ball

    fun detectBalls(bitmap: Bitmap): List<Ball> {
        val balls = mutableListOf<Ball>()
        if (bitmap.isRecycled) return balls // Defensive check

        val rgba = Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, rgba) // Convert Bitmap to OpenCV Mat (RGBA)
        val hsv = Mat()
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGBA2HSV) // Convert RGBA to HSV color space

        // Default HSV ranges for detection (can be expanded for more ball colors)
        // This targets white (cue ball) and yellow (e.g., 1-ball, 9-ball)
        val defaultHsvRanges = hsvRanges ?: listOf(
            // White/Light colors (e.g., cue ball) - broad range for varied lighting
            HSVRange(0, 0, 180, 180, 25, 255),
            // Yellow colors (e.g., 1-ball, 9-ball)
            HSVRange(20, 100, 100, 30, 255, 255)
        )

        val circles = Mat() // Mat to store detected circles from Hough Transform
        try {
            for (range in defaultHsvRanges) {
                val lower = Scalar(range.hmin.toDouble(), range.smin.toDouble(), range.vmin.toDouble())
                val upper = Scalar(range.hmax.toDouble(), range.smax.toDouble(), range.vmax.toDouble())
                val mask = Mat()
                Core.inRange(hsv, lower, upper, mask) // Create a binary mask for the current HSV range

                Imgproc.GaussianBlur(mask, mask, Size(9.0, 9.0), 2.0, 2.0) // Apply Gaussian blur to reduce noise

                // Apply Hough Circle Transform
                // param1: Higher threshold for Canny edge detector (used internally by HoughCircles).
                // param2: Accumulator threshold for the circle centers. Smaller values means more circles.
                Imgproc.HoughCircles(mask, circles, Imgproc.HOUGH_GRADIENT, 1.0, // dp (inverse ratio of resolution)
                    mask.rows() / 8.0, // minDist (min distance between centers)
                    100.0, 30.0, // param1, param2
                    minBallRadius, maxBallRadius) // minRadius, maxRadius

                // Iterate through detected circles and add them to the list
                if (circles.cols() > 0) {
                    for (i in 0 until circles.cols()) {
                        val circle = circles.get(0, i) // circle[0] = x, circle[1] = y, circle[2] = radius
                        if (circle != null && circle.size >= 3) {
                            val x = circle[0].toInt()
                            val y = circle[1].toInt()
                            val r = circle[2].toInt()
                            // Assign a unique ID using a combination of timestamp and a simple index
                            balls.add(Ball("${range.hmin}_${balls.size}_${System.currentTimeMillis()}", x, y, r))
                        }
                    }
                }
                mask.release() // Release the mask Mat after use to prevent memory leaks
            }
        } finally {
            // Ensure all Mat objects are released in a finally block
            rgba.release()
            hsv.release()
            circles.release()
        }
        return balls
    }
}
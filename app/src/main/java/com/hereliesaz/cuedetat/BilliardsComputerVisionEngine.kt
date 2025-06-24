package com.hereliesaz.cuedetat

import android.graphics.Bitmap
import android.graphics.Point
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Detects pool balls and table contours from camera image input.
 */
class BilliardsComputerVisionEngine {

    fun detectBallsAndTable(bitmap: Bitmap): Pair<List<Point>, Mat?> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian blur
        Imgproc.GaussianBlur(gray, gray, Size(9.0, 9.0), 2.0)

        // Use HoughCircles to detect circular blobs (balls)
        val circles = Mat()
        Imgproc.HoughCircles(
            gray, circles, Imgproc.HOUGH_GRADIENT,
            1.0, gray.rows() / 8.0, 100.0, 20.0, 5, 30
        )

        val ballCenters = mutableListOf<Point>()
        for (i in 0 until circles.cols()) {
            val data = circles.get(0, i)
            val x = data[0].toInt()
            val y = data[1].toInt()
            ballCenters.add(Point(x, y))
        }

        return Pair(ballCenters, mat)
    }
}

package com.hereliesaz.cuedetat

import android.graphics.Bitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class TableEdgeDetector {

    fun detectTableEdges(bitmap: Bitmap): List<Point>? {
        val mat = Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(blurred, edges, 50.0, 150.0)

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var maxArea = 0.0
        var tableContour: MatOfPoint2f? = null

        for (contour in contours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

            if (approx.total() == 4L && Imgproc.isContourConvex(MatOfPoint(*approx.toArray()))) {
                val area = Imgproc.contourArea(approx)
                if (area > maxArea) {
                    maxArea = area
                    tableContour = approx
                }
            }
        }

        tableContour?.let {
            val points = it.toArray().toList()
            return orderPointsClockwise(points)
        }

        return null
    }

    private fun orderPointsClockwise(points: List<Point>): List<Point> {
        val sorted = points.sortedWith(compareBy({ it.y }, { it.x }))
        val topPoints = sorted.take(2).sortedBy { it.x }
        val bottomPoints = sorted.takeLast(2).sortedBy { it.x }

        return listOf(
            topPoints[0],
            topPoints[1],
            bottomPoints[1],
            bottomPoints[0]
        )
    }
}

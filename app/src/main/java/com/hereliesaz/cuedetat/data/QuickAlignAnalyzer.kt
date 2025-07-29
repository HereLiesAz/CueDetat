package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import org.opencv.calib3d.Calib3d
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.sqrt

@Singleton
class QuickAlignAnalyzer @Inject constructor() {

    /**
     * Calculates the homography matrix from the tapped screen points and
     * logical corner pockets, then decomposes it to extract translation,
     * rotation, and scale.
     *
     * @param tappedPoints List of 4 Offset objects representing the tapped
     *    screen coordinates.
     * @param logicalCornerPockets List of 4 Offset objects representing the
     *    corresponding logical coordinates of the corner pockets (e.g., 0-1
     *    range).
     * @param imageBitmap The current camera frame bitmap.
     * @return A Triple containing (translation: Offset, rotation: Float,
     *    scale: Float).
     */
    fun calculateAlignment(
        tappedPoints: List<Offset>,
        logicalCornerPockets: List<Offset>,
        imageBitmap: Bitmap
    ): Triple<Offset, Float, Float> {

        if (tappedPoints.size != 4 || logicalCornerPockets.size != 4) {
            throw IllegalArgumentException("Both tappedPoints and logicalCornerPockets must contain exactly 4 points.")
        }

        // Convert Compose Offset to OpenCV Point
        val srcPoints = MatOfPoint2f(
            Point(tappedPoints[0].x.toDouble(), tappedPoints[0].y.toDouble()),
            Point(tappedPoints[1].x.toDouble(), tappedPoints[1].y.toDouble()),
            Point(tappedPoints[2].x.toDouble(), tappedPoints[2].y.toDouble()),
            Point(tappedPoints[3].x.toDouble(), tappedPoints[3].y.toDouble())
        )

        val dstPoints = MatOfPoint2f(
            Point(logicalCornerPockets[0].x.toDouble(), logicalCornerPockets[0].y.toDouble()),
            Point(logicalCornerPockets[1].x.toDouble(), logicalCornerPockets[1].y.toDouble()),
            Point(logicalCornerPockets[2].x.toDouble(), logicalCornerPockets[2].y.toDouble()),
            Point(logicalCornerPockets[3].x.toDouble(), logicalCornerPockets[3].y.toDouble())
        )

        // Calculate Homography
        val homography = Calib3d.findHomography(srcPoints, dstPoints)

        // Decompose Homography Matrix (simplified for 2D transform)
        // H = [ s*cos(theta) -s*sin(theta) tx ]
        //     [ s*sin(theta)  s*cos(theta) ty ]
        //     [ 0             0            1  ]

        val scaleX = sqrt(
            homography.get(0, 0)[0] * homography.get(0, 0)[0] + homography.get(
                1,
                0
            )[0] * homography.get(1, 0)[0]
        ).toFloat()
        val scaleY = sqrt(
            homography.get(0, 1)[0] * homography.get(0, 1)[0] + homography.get(
                1,
                1
            )[0] * homography.get(1, 1)[0]
        ).toFloat()
        val scale = (scaleX + scaleY) / 2 // Average scale

        val rotationRad = atan2(homography.get(1, 0)[0], homography.get(0, 0)[0]).toFloat()
        val rotationDegrees = Math.toDegrees(rotationRad.toDouble()).toFloat()

        val translateX = homography.get(0, 2)[0].toFloat()
        val translateY = homography.get(1, 2)[0].toFloat()
        val translation = Offset(translateX, translateY)

        srcPoints.release()
        dstPoints.release()
        homography.release()

        return Triple(translation, rotationDegrees, scale)
    }
}
// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/CalibrationRepository.kt

package com.hereliesaz.cuedetat.data

import org.opencv.calib3d.Calib3d
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point3
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import com.google.gson.Gson
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepository @Inject constructor(
    private val gson: Gson
) {
    private val patternSize = Size(4.0, 11.0) // 4x11 grid of circles

    fun findPattern(frame: Mat): MatOfPoint2f? {
        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)

        val corners = MatOfPoint2f()
        val found = Calib3d.findCirclesGrid(
            gray,
            patternSize,
            corners,
            Calib3d.CALIB_CB_ASYMMETRIC_GRID
        )

        gray.release()

        return if (found) {
            corners
        } else {
            corners.release()
            null
        }
    }

    fun calculateCalibration(imagePoints: List<MatOfPoint2f>, imageSize: Size): Pair<Mat, Mat>? {
        if (imagePoints.isEmpty()) return null

        val objectPoints = mutableListOf<Mat>()
        val singleObjectPoints = MatOfPoint3f()
        val points = mutableListOf<Point3>()
        for (i in 0 until patternSize.height.toInt()) {
            for (j in 0 until patternSize.width.toInt()) {
                points.add(Point3((2 * j + i % 2).toDouble(), i.toDouble(), 0.0))
            }
        }
        singleObjectPoints.fromList(points)

        for (i in imagePoints.indices) {
            objectPoints.add(singleObjectPoints)
        }

        val cameraMatrix = Mat()
        val distCoeffs = Mat()
        val rvecs = mutableListOf<Mat>()
        val tvecs = mutableListOf<Mat>()

        try {
            Calib3d.calibrateCamera(
                objectPoints,
                imagePoints,
                imageSize,
                cameraMatrix,
                distCoeffs,
                rvecs,
                tvecs
            )
            singleObjectPoints.release()
            return Pair(cameraMatrix, distCoeffs)
        } catch (e: Exception) {
            // Calibration can fail if the data is bad
            singleObjectPoints.release()
            cameraMatrix.release()
            distCoeffs.release()
            return null
        }
    }

    suspend fun submitCalibrationData(cameraMatrix: Mat, distCoeffs: Mat) {
        val cameraMatrixData = DoubleArray(9)
        cameraMatrix.get(0, 0, cameraMatrixData)

        val distCoeffsData = DoubleArray(5)
        distCoeffs.get(0, 0, distCoeffsData)

        val payload = mapOf(
            "cameraMatrix" to cameraMatrixData.toList(),
            "distCoeffs" to distCoeffsData.toList()
        )

        val json = gson.toJson(payload)
        android.util.Log.d("CalibrationRepository", "Submitting calibration data: $json")
        delay(2000)
    }
}
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

/**
 * Repository responsible for performing camera calibration calculations.
 *
 * It uses OpenCV functions to detect calibration patterns (Asymmetric Circles Grid)
 * and calculate the camera's intrinsic parameters (Camera Matrix) and distortion coefficients.
 */
@Singleton
class CalibrationRepository @Inject constructor(
    private val gson: Gson
) {
    // The target calibration pattern is a 4x11 Asymmetric Circles Grid.
    // This pattern is chosen for its robustness in detection.
    private val patternSize = Size(4.0, 11.0)

    /**
     * Attempts to find the calibration pattern in a given video frame.
     *
     * @param frame The current camera frame (BGR format).
     * @return A [MatOfPoint2f] containing the coordinates of the detected pattern corners, or null if not found.
     */
    fun findPattern(frame: Mat): MatOfPoint2f? {
        val gray = Mat()
        // Convert to grayscale as corner detection works on intensity channels.
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)

        val corners = MatOfPoint2f()
        // Use OpenCV's findCirclesGrid to locate the pattern centers.
        val found = Calib3d.findCirclesGrid(
            gray,
            patternSize,
            corners,
            Calib3d.CALIB_CB_ASYMMETRIC_GRID
        )

        // Release the grayscale image memory.
        gray.release()

        return if (found) {
            corners // Return detected points.
        } else {
            corners.release() // Clean up if failed.
            null
        }
    }

    /**
     * Calculates the camera calibration parameters based on a set of captured images.
     *
     * @param imagePoints A list of detected 2D points from multiple frames.
     * @param imageSize The resolution of the images used.
     * @return A Pair containing the Camera Matrix and Distortion Coefficients (Mat objects), or null if failed.
     */
    fun calculateCalibration(imagePoints: List<MatOfPoint2f>, imageSize: Size): Pair<Mat, Mat>? {
        if (imagePoints.isEmpty()) return null

        // Prepare object points (the 3D coordinates of the pattern in real world units).
        // Since the pattern is planar (z=0), we define the points on a grid.
        val objectPoints = mutableListOf<Mat>()
        val singleObjectPoints = MatOfPoint3f()
        val points = mutableListOf<Point3>()

        // Generate the standard asymmetric grid coordinates.
        for (i in 0 until patternSize.height.toInt()) {
            for (j in 0 until patternSize.width.toInt()) {
                points.add(Point3((2 * j + i % 2).toDouble(), i.toDouble(), 0.0))
            }
        }
        singleObjectPoints.fromList(points)

        // Repeat the object points for every captured image (as the real-world pattern is constant).
        for (i in imagePoints.indices) {
            objectPoints.add(singleObjectPoints)
        }

        // Output matrices.
        val cameraMatrix = Mat()
        val distCoeffs = Mat()
        val rvecs = mutableListOf<Mat>()
        val tvecs = mutableListOf<Mat>()

        try {
            // Perform the calibration calculation.
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
            // Calibration can fail if the data is degenerate or insufficient.
            singleObjectPoints.release()
            cameraMatrix.release()
            distCoeffs.release()
            return null
        }
    }

    /**
     * Simulates submitting the calibration data to a remote server.
     *
     * In a real app, this would perform a network POST request.
     *
     * @param cameraMatrix The calculated camera matrix.
     * @param distCoeffs The calculated distortion coefficients.
     */
    suspend fun submitCalibrationData(cameraMatrix: Mat, distCoeffs: Mat) {
        // Extract data to arrays for serialization.
        val cameraMatrixData = DoubleArray(9)
        cameraMatrix.get(0, 0, cameraMatrixData)

        val distCoeffsData = DoubleArray(5)
        distCoeffs.get(0, 0, distCoeffsData)

        // Create a JSON payload.
        val payload = mapOf(
            "cameraMatrix" to cameraMatrixData.toList(),
            "distCoeffs" to distCoeffsData.toList()
        )

        val json = gson.toJson(payload)
        // Log the payload instead of sending it.
        android.util.Log.d("CalibrationRepository", "Submitting calibration data: $json")
        // Simulate network latency.
        delay(2000)
    }
}

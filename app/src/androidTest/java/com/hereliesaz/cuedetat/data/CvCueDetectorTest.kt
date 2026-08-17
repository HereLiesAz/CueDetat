package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import org.opencv.core.Rect as CvRect

/**
 * Instrumented (not plain JVM) because CvCueDetector calls into OpenCV's
 * native libs, same constraint as MergedTFLiteDetectorTest — OpenCV is
 * initialised natively by MyApplication when the instrumented test process
 * starts, so no explicit OpenCVLoader call is needed here.
 */
@RunWith(AndroidJUnit4::class)
class CvCueDetectorTest {

    private fun bgrAndHsvFrom(bitmap: Bitmap): Pair<Mat, Mat> {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val bgr = Mat()
        Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR)
        rgba.release()
        val hsv = Mat()
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV)
        return Pair(bgr, hsv)
    }

    /** Samples a corner patch known to be pure felt to derive the felt HSV prior. */
    private fun feltStats(hsv: Mat): Pair<FloatArray, FloatArray> {
        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        val patch = hsv.submat(CvRect(0, 0, 40, 40))
        Core.meanStdDev(patch, mean, stddev)
        patch.release()
        val meanArr = floatArrayOf(
            mean.get(0, 0)[0].toFloat(),
            mean.get(1, 0)[0].toFloat(),
            mean.get(2, 0)[0].toFloat(),
        )
        // A perfectly flat synthetic swatch has ~0 real stddev; floor it so the
        // inRange tolerance in FeltMask isn't degenerate.
        val sdArr = floatArrayOf(
            stddev.get(0, 0)[0].toFloat().coerceAtLeast(2f),
            stddev.get(1, 0)[0].toFloat().coerceAtLeast(2f),
            stddev.get(2, 0)[0].toFloat().coerceAtLeast(2f),
        )
        return Pair(meanArr, sdArr)
    }

    @Test
    fun detectsLongStraightCueOnFeltBackground() {
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(20, 110, 40)) // felt green

        val paint = Paint().apply {
            color = Color.rgb(235, 225, 200) // pale cue-stick color
            strokeWidth = 6f
            isAntiAlias = true
        }
        // A long diagonal line spanning most of the frame — should read as a cue.
        canvas.drawLine(40f, 40f, 600f, 440f, paint)

        val (bgr, hsv) = bgrAndHsvFrom(bitmap)
        try {
            val (feltHsv, feltStdDev) = feltStats(hsv)
            val detector = CvCueDetector()
            val detections = detector.detect(bgr, hsv, feltHsv, feltStdDev)
            detector.release()

            assertTrue(
                "expected at least one cue candidate on a long straight line, got none",
                detections.isNotEmpty()
            )

            val best = detections.maxByOrNull { it.confidence }!!
            val drawnLen = Math.hypot((600 - 40).toDouble(), (440 - 40).toDouble())
            val rectDiag = Math.hypot(best.rect.width().toDouble(), best.rect.height().toDouble())
            assertTrue(
                "expected the detected bounding box (diag=$rectDiag) to cover a good " +
                    "fraction of the drawn line (diag=$drawnLen)",
                rectDiag > drawnLen * 0.5
            )
        } finally {
            bgr.release()
            hsv.release()
        }
    }

    @Test
    fun feltOnlyImageYieldsNoCues() {
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(20, 110, 40))

        val (bgr, hsv) = bgrAndHsvFrom(bitmap)
        try {
            val (feltHsv, feltStdDev) = feltStats(hsv)
            val detector = CvCueDetector()
            val detections = detector.detect(bgr, hsv, feltHsv, feltStdDev)
            detector.release()

            assertEquals(
                "a uniform felt-only frame should produce zero cue candidates",
                0,
                detections.size
            )
        } finally {
            bgr.release()
            hsv.release()
        }
    }
}

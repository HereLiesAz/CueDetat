// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt

package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.utils.toMat
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.CvRefinementMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import org.opencv.core.Rect as OCVRect

/**
 * The central repository for all Computer Vision (CV) operations.
 *
 * This class acts as the bridge between the CameraX image stream and the application's state.
 * It is responsible for:
 * 1.  Processing raw camera frames (YUV/NV21) into OpenCV Matrices.
 * 2.  Executing ML Kit object detection to find generic "round objects".
 * 3.  Refining those detections using OpenCV algorithms (Hough Circles or Contours) to precise ball locations.
 * 4.  Calculating color statistics (HSV) to distinguish the cue ball from object balls.
 * 5.  Transforming 2D image coordinates into 3D-aware logical table coordinates.
 *
 * PERFORMANCE CRITICALITY:
 * This class runs on a background thread for every single camera frame (aiming for 30 FPS).
 * Therefore, object allocation is strictly forbidden inside the main loop.
 * All OpenCV [Mat] objects are allocated once as class members ("reusable...") and reused.
 */
@Singleton
class VisionRepository @Inject constructor(
    // We inject a generic ML Kit detector configured for "Prominent Objects".
    // This gives us rough bounding boxes (ROIs) which we then refine with OpenCV.
    @GenericDetector private val genericObjectDetector: ObjectDetector,
) {

    // The single source of truth for vision data.
    // The UI collects from this flow to render debug overlays or update the game state.
    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    // FPS throttling: We track the last processing time to ensure we don't clog the pipeline
    // if processing takes longer than 33ms.
    private var lastFrameTime = 0L

    // A cached mask Mat for debugging visualization.
    // It is nullable because we only allocate it if the user enables "Show CV Mask".
    private var reusableMask: Mat? = null

    // ---------------------------------------------------------------------------------------------
    // REUSABLE MATRICES (MEMORY OPTIMIZATION)
    // ---------------------------------------------------------------------------------------------
    // Allocating Mat objects in a loop causes massive GC thrashing. We reuse these instances.

    // Holds the converted BGR image from the camera.
    private val reusableFrameMat = Mat()
    // Holds the rotated version of the frame if the device orientation requires it.
    private val reusableRotatedMat = Mat()
    // Holds the HSV (Hue-Saturation-Value) version of the frame for color segmentation.
    private val reusableHsvMat = Mat()

    // Reusable temporary Mats for the refineBallCenter pipeline.
    private val reusableGray = Mat()      // Grayscale version of ROI
    private val reusableEdges = Mat()     // Canny edge detection output
    private val reusableHierarchy = Mat() // Contour hierarchy (unused but required by findContours)
    private val reusableCircles = Mat()   // Output of HoughCircles

    // Reusable primitive arrays to avoid 'new float[]' calls.
    private val reusableMatrixValues = FloatArray(9) // For dumping Matrix values
    private val reusablePointArray = FloatArray(2)   // For mapping points

    // Reusable wrappers for mean/stdDev calculations.
    private val reusableMean = MatOfDouble()
    private val reusableStdDev = MatOfDouble()

    // A standard morphological kernel (5x5 Ellipse).
    // Used for "opening" and "closing" operations to clean up noise in binary masks.
    private val reusableMorphKernel by lazy {
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    }

    /**
     * The main entry point for frame processing.
     * Called by CameraX's ImageAnalysis.Analyzer.
     *
     * @param imageProxy The wrapper around the raw hardware buffer. MUST be closed.
     * @param state The current application state (needed for zoom/pitch context).
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, state: CueDetatState) {
        // Step 1: Throttling.
        // Check if enough time has passed since the last frame to maintain ~30 FPS.
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < 33) {
            imageProxy.close() // DROP FRAME.
            return
        }
        lastFrameTime = currentTime

        // Extract the underlying android.media.Image.
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            // Wrap it for ML Kit.
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            // Calculate the scaling matrix between Camera Image coordinates and Screen View coordinates.
            // Camera buffers are often high-res (e.g., 1920x1080) while the View might be smaller.
            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            try {
                // Step 2: ML Kit Detection (Coarse Pass).
                // We use ML Kit to find "objects" first. This is faster than running
                // Hough Circles on the entire 1080p frame.
                // Tasks.await is safe here because we are already on a background thread.
                val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))

                // Step 3: OpenCV Conversion.
                // Convert the YUV image buffer to a BGR Mat.
                // We use our extension method 'toMat' which efficiently copies the buffer.
                // Result is stored in 'reusableFrameMat'.
                val originalMat = imageProxy.toMat(reusableFrameMat)

                // Step 4: Rotation Handling.
                // OpenCV Mats don't respect the metadata rotation tag. We must manually rotate the pixels
                // if the device is in portrait or reverse-landscape.
                var matToUse: Mat
                when (rotationDegrees) {
                    90 -> {
                        Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE)
                        matToUse = reusableRotatedMat
                    }
                    180 -> {
                        Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_180)
                        matToUse = reusableRotatedMat
                    }
                    270 -> {
                        Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                        matToUse = reusableRotatedMat
                    }
                    else -> {
                        matToUse = originalMat
                    }
                }
                // NOTE: 'matToUse' now points to the correct, upright BGR image.

                // Step 5: Color Space Conversion (BGR -> HSV).
                // We need HSV for robust color sampling.
                Imgproc.cvtColor(matToUse, reusableHsvMat, Imgproc.COLOR_BGR2HSV)
                val hsvMat = reusableHsvMat

                // Step 6: Color Sampling (User Interaction).
                // If the user is dragging the color picker (state.colorSamplePoint is not null),
                // we calculate the mean color in a 5x5 patch under their finger.
                val hsvTuple = state.colorSamplePoint?.let {
                    // Map screen coordinates (0..1) to image coordinates (pixels).
                    val imageX = (it.x * (hsvMat.cols() / state.viewWidth.toFloat())).toInt()
                    val imageY = (it.y * (hsvMat.rows() / state.viewHeight.toFloat())).toInt()

                    // Define the Region of Interest (ROI) - a small 5x5 square.
                    val patchSize = 5
                    val roiX = (imageX - patchSize / 2).coerceIn(0, hsvMat.cols() - patchSize)
                    val roiY = (imageY - patchSize / 2).coerceIn(0, hsvMat.rows() - patchSize)
                    val roi = OCVRect(roiX, roiY, patchSize, patchSize)

                    // Extract the submatrix.
                    val sampleRegion = hsvMat.submat(roi)

                    // Compute Mean and Standard Deviation.
                    Core.meanStdDev(sampleRegion, reusableMean, reusableStdDev)

                    // Extract values to float arrays.
                    val meanArray = floatArrayOf(
                        reusableMean.get(0, 0)[0].toFloat(), // H
                        reusableMean.get(1, 0)[0].toFloat(), // S
                        reusableMean.get(2, 0)[0].toFloat()  // V
                    )
                    val stddevArray = floatArrayOf(
                        reusableStdDev.get(0, 0)[0].toFloat(),
                        reusableStdDev.get(1, 0)[0].toFloat(),
                        reusableStdDev.get(2, 0)[0].toFloat()
                    )

                    sampleRegion.release() // Always release submatrices to avoid memory leaks.
                    Pair(meanArray, stddevArray)
                }

                // Determine the "Reference Color" for detection.
                // Priority: Locked Color > Sampled Color > Center of Screen Default.
                val hsv = state.lockedHsvColor ?: hsvTuple?.first ?: run {
                    // Default: Sample center 50x50 pixels.
                    val roiWidth = 50
                    val roiHeight = 50
                    val roiX = (hsvMat.cols() - roiWidth) / 2
                    val roiY = (hsvMat.rows() - roiHeight) / 2
                    val centerRoi = hsvMat.submat(OCVRect(roiX, roiY, roiWidth, roiHeight))
                    val meanColor = Core.mean(centerRoi)
                    centerRoi.release()
                    floatArrayOf(
                        meanColor.`val`[0].toFloat(),
                        meanColor.`val`[1].toFloat(),
                        meanColor.`val`[2].toFloat()
                    )
                }

                // Step 7: Debug Mask Generation (Optional).
                // If "Show CV Mask" is enabled, we perform a full-frame color threshold
                // so the user can see what the computer sees.
                val cvMask = if (state.showCvMask) {
                    if (reusableMask == null) {
                        reusableMask = Mat()
                    }
                    val mask = reusableMask!!

                    // Calculate dynamic thresholds based on standard deviation if available.
                    if (state.lockedHsvColor != null && state.lockedHsvStdDev != null) {
                        val mean = state.lockedHsvColor
                        val stdDev = state.lockedHsvStdDev
                        val stdDevMultiplier = 2.0 // Strictness factor

                        val lowerBound = Scalar(
                            (mean[0] - stdDevMultiplier * stdDev[0]).coerceAtLeast(0.0),
                            (mean[1] - stdDevMultiplier * stdDev[1]).coerceAtLeast(40.0), // Min Saturation
                            (mean[2] - stdDevMultiplier * stdDev[2]).coerceAtLeast(40.0)  // Min Value
                        )
                        val upperBound = Scalar(
                            (mean[0] + stdDevMultiplier * stdDev[0]).coerceAtMost(180.0),
                            (mean[1] + stdDevMultiplier * stdDev[1]).coerceAtMost(255.0),
                            (mean[2] + stdDevMultiplier * stdDev[2]).coerceAtMost(255.0)
                        )
                        Core.inRange(hsvMat, lowerBound, upperBound, mask)
                    } else {
                        // Fallback: Fixed Hue range.
                        val hueRange = 10.0
                        val lowerBound = Scalar((hsv[0] - hueRange).coerceAtLeast(0.0), 100.0, 100.0)
                        val upperBound = Scalar((hsv[0] + hueRange).coerceAtMost(180.0), 255.0, 255.0)
                        Core.inRange(hsvMat, lowerBound, upperBound, mask)
                    }

                    // Clean up noise (close small holes).
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, reusableMorphKernel)

                    // CRITICAL: We MUST clone the mask here.
                    // The UI thread reads this mask to draw the bitmap. If we modify 'mask' (which is 'reusableMask')
                    // in the next frame while the UI is still drawing the previous one, we get tearing or crashes.
                    mask.clone()
                } else {
                    reusableMask?.release()
                    reusableMask = null
                    null
                }

                // Step 8: Filter ML Kit Results.
                // ML Kit often detects non-ball objects. We filter them by calculating the
                // expected size of a ball at that Y-coordinate (perspective-aware).
                val filteredDetectedObjects = detectedObjects.filter {
                    val box = it.boundingBox
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY().toFloat(),
                        state,
                        imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    // If the box is massively larger than a ball, ignore it.
                    (box.width() * box.height()) <= maxAllowedArea
                }

                // Step 9: Refine Locations.
                // For each valid ML Kit box, we zoom in (Crop) and run precise OpenCV algorithms.
                val refinedScreenPoints = filteredDetectedObjects.mapNotNull { detectedObject ->
                    refineBallCenter(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    // Convert the result from Image Coords -> Screen Coords.
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }

                // Step 10: Map to Logical Space.
                // Convert Screen Pixels -> Logical Inches using the inverse perspective matrix.
                val detectedLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    refinedScreenPoints.map { screenPoint ->
                        Perspective.screenToLogical(screenPoint, inverseMatrix)
                    }
                } else {
                    emptyList()
                }

                // Step 11: Table Filtering.
                // Ignore balls detected outside the table boundaries.
                val filteredBalls = if (state.table.isVisible) {
                    detectedLogicalPoints.filter { state.table.isPointInside(it) }
                } else {
                    detectedLogicalPoints
                }

                // Step 12: Emit Results.
                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                    cvMask = cvMask,
                    sourceImageWidth = inputImage.width,
                    sourceImageHeight = inputImage.height,
                    sourceImageRotation = rotationDegrees
                )

                if (hsvTuple != null) {
                    finalVisionData = finalVisionData.copy(
                        detectedHsvColor = hsvTuple.first,
                    )
                }

                _visionDataFlow.value = finalVisionData

                // NOTE: We intentionally do NOT release reusable mats here. They are cleared/overwritten next frame.

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // ALWAYS close the image proxy to free the hardware buffer.
                // Failure to do this hangs the camera.
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Refines the center of a detected object using precise computer vision.
     *
     * ML Kit gives us a box. We assume the ball is inside. We crop that box and run
     * either Contour detection or Hough Circle Transform on that small patch.
     */
    private fun refineBallCenter(
        detectedObject: DetectedObject,
        frame: Mat,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): PointF? {
        val box = detectedObject.boundingBox
        val roi = OCVRect(box.left, box.top, box.width(), box.height())

        // Safety check: Ensure ROI is inside the image.
        if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > frame.cols() || roi.y + roi.height > frame.rows()) {
            return null
        }

        // Calculate expected radius again for the local search.
        val expectedRadiusInImageCoords =
            getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
        val tolerance = 0.5f // +/- 50% size tolerance
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        // Create a submatrix (view) into the frame. No data copy.
        val roiMat = frame.submat(roi)

        // Clean up the ROI image.
        Imgproc.morphologyEx(roiMat, roiMat, Imgproc.MORPH_OPEN, reusableMorphKernel)

        // Run the selected algorithm.
        val refinedCenterInRoi = when (state.cvRefinementMethod) {
            CvRefinementMethod.CONTOUR -> findBallByContour(
                roiMat,
                minRadius,
                maxRadius,
                state.cannyThreshold1.toDouble(),
                state.cannyThreshold2.toDouble()
            )
            CvRefinementMethod.HOUGH -> findBallByHough(
                roiMat,
                minRadius,
                maxRadius,
                state.houghP1.toDouble(),
                state.houghP2.toDouble()
            )
        }

        roiMat.release() // Release the submat wrapper.

        // Convert the local ROI coordinate back to global image coordinate.
        return refinedCenterInRoi?.let {
            PointF(it.x + roi.x, it.y + roi.y)
        }
    }

    /**
     * Calculates how large a ball *should* be in the image at a given vertical position.
     *
     * Due to perspective, balls at the bottom of the screen (closer) appear larger than
     * balls at the top (further away). We use the projection matrix to determine this scale factor.
     */
    private fun getExpectedRadiusAtImageY(
        imageY: Float,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): Float {
        val pitchMatrix = state.pitchMatrix
        if (!state.hasInverseMatrix || pitchMatrix == null) return LOGICAL_BALL_RADIUS

        // Map Image Y -> Screen Y.
        reusablePointArray[0] = 0f
        reusablePointArray[1] = imageY
        imageToScreenMatrix.mapPoints(reusablePointArray)
        val screenY = reusablePointArray[1]

        // Determine logical top/bottom of the table/world.
        val logicalTopY = if (state.table.isVisible) -state.table.logicalHeight / 2f else -200f
        val logicalBottomY = if (state.table.isVisible) state.table.logicalHeight / 2f else 200f
        val logicalTop = PointF(0f, logicalTopY)
        val logicalBottom = PointF(0f, logicalBottomY)

        // Project logical points to Screen Space to get their radii.
        val screenTopInfo = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalTop, LOGICAL_BALL_RADIUS, state, pitchMatrix
        )
        val screenBottomInfo = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalBottom, LOGICAL_BALL_RADIUS, state, pitchMatrix
        )
        val screenTopY = DrawingUtils.mapPoint(logicalTop, pitchMatrix).y
        val screenBottomY = DrawingUtils.mapPoint(logicalBottom, pitchMatrix).y

        val rangeY = screenBottomY - screenTopY
        if (abs(rangeY) < 1f) return screenBottomInfo.radius

        // Linear interpolation based on screen Y position.
        val fraction = ((screenY - screenTopY) / rangeY).coerceIn(0f, 1f)
        val interpolatedRadius =
            screenTopInfo.radius + fraction * (screenBottomInfo.radius - screenTopInfo.radius)

        // Convert back from Screen Radius -> Image Radius using scale factor.
        imageToScreenMatrix.getValues(reusableMatrixValues)
        val scaleY = reusableMatrixValues[Matrix.MSCALE_Y]

        return if (scaleY > 0) interpolatedRadius / scaleY else LOGICAL_BALL_RADIUS
    }

    /**
     * Algorithm 1: Canny Edge Detection + Contours.
     * Finds balls by looking for circular edges.
     */
    private fun findBallByContour(
        roiMat: Mat,
        minRadius: Float,
        maxRadius: Float,
        cannyT1: Double,
        cannyT2: Double
    ): PointF? {
        // Convert to Grayscale.
        Imgproc.cvtColor(roiMat, reusableGray, Imgproc.COLOR_BGR2GRAY)
        // Blur to reduce noise (vital for Canny).
        Imgproc.GaussianBlur(reusableGray, reusableGray, Size(5.0, 5.0), 2.0, 2.0)

        // Edge Detection.
        Imgproc.Canny(reusableGray, reusableEdges, cannyT1, cannyT2)

        val contours = ArrayList<MatOfPoint>()
        // Find Contours in the edge map.
        Imgproc.findContours(
            reusableEdges,
            contours,
            reusableHierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var bestCenter: PointF? = null

        if (contours.isNotEmpty()) {
            // Concatenate all contours to find the Minimum Enclosing Circle of *all* edges in the box.
            // This assumes the box primarily contains one object (the ball).
            val allPoints = MatOfPoint()
            Core.vconcat(contours as List<Mat>, allPoints)
            val allPoints2f = MatOfPoint2f(*allPoints.toArray())

            val centerArray = org.opencv.core.Point()
            val radiusArray = FloatArray(1)
            Imgproc.minEnclosingCircle(allPoints2f, centerArray, radiusArray)
            val radius = radiusArray[0]

            // Validate size.
            if (radius > minRadius && radius < maxRadius) {
                bestCenter = PointF(centerArray.x.toFloat(), centerArray.y.toFloat())
            }

            allPoints.release()
            allPoints2f.release()
        }

        // Release contours manually.
        contours.forEach { it.release() }

        return bestCenter
    }

    /**
     * Algorithm 2: Hough Circle Transform.
     * Finds circles by voting in parameter space. Slower but more robust to partial occlusion.
     */
    private fun findBallByHough(
        roiMat: Mat,
        minRadius: Float,
        maxRadius: Float,
        houghP1: Double,
        houghP2: Double
    ): PointF? {
        Imgproc.cvtColor(roiMat, reusableGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(reusableGray, reusableGray, 5)

        Imgproc.HoughCircles(
            reusableGray,
            reusableCircles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            roiMat.rows().toDouble() / 8, // Min dist between circles
            houghP1,
            houghP2,
            minRadius.toInt(),
            maxRadius.toInt()
        )

        var center: PointF? = null
        if (reusableCircles.cols() > 0) {
            // Get the first detected circle.
            val circleData = reusableCircles[0, 0]
            if (circleData != null && circleData.isNotEmpty()) {
                center = PointF(circleData[0].toFloat(), circleData[1].toFloat())
            }
        }
        return center
    }

    private fun getTransformationMatrix(
        sourceWidth: Int, sourceHeight: Int,
        destWidth: Int, destHeight: Int
    ): Matrix {
        val matrix = Matrix()
        val sx = destWidth.toFloat() / sourceWidth.toFloat()
        val sy = destHeight.toFloat() / sourceHeight.toFloat()
        matrix.postScale(sx, sy)
        return matrix
    }
}

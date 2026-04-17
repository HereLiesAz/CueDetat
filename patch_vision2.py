import re

with open("app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt", "r") as f:
    text = f.read()

# Restore imports
pkg = "package com.hereliesaz.cuedetat.data\n"
new_imports = """
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
"""
if "import com.google.mlkit.vision.common.InputImage" not in text:
    text = text.replace(pkg, pkg + new_imports)

# Restore constructor
repo_old = """@Singleton
class VisionRepository @Inject constructor(
    private val pocketDetector: PocketDetector,
    private val poolDetector: TFLitePoolDetector,
    private val dataStoreManager: DataStoreManager,
"""
repo_new = """@Singleton
class VisionRepository @Inject constructor(
    private val pocketDetector: PocketDetector,
    private val poolDetector: TFLitePoolDetector,
    @GenericDetector private val genericObjectDetector: ObjectDetector,
    private val dataStoreManager: DataStoreManager,
"""
text = text.replace(repo_old, repo_new)


# Fix processImage
pi_block_old = """            val imageToScreenMatrix = getTransformationMatrix(
                imageProxy.width, imageProxy.height,
                state.viewWidth, state.viewHeight
            )

            try {
                val rawDetections = poolDetector.detect(bitmap)
            val detectedObjects = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""

pi_block_new = """            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            try {
                val genericObjects = Tasks.await(genericObjectDetector.process(inputImage))
                val rawDetections = poolDetector.detect(bitmap)
                val customPoolBalls = rawDetections.filter { it.classId == 1 }
                val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                    android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
                }"""
text = text.replace(pi_block_old, pi_block_new)

old_filter1 = """                val filteredDetectedObjects = detectedObjects.filter {
                    val box = it.rect
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val refinedScreenPoints = filteredDetectedObjects.mapNotNull { obj ->
                    refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }"""

new_filter1 = """                val filteredGenericObjects = genericObjects.filter {
                    val box = it.boundingBox
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY().toFloat(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val refinedGenericPoints = filteredGenericObjects.mapNotNull { obj ->
                    refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
                }.map { pt ->
                    val arr = floatArrayOf(pt.x, pt.y)
                    imageToScreenMatrix.mapPoints(arr)
                    PointF(arr[0], arr[1])
                }

                val filteredCustomBalls = customPoolBalls.filter {
                    val box = it.rect
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val refinedCustomPoints = filteredCustomBalls.mapNotNull { obj ->
                    refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
                }.map { pt ->
                    val arr = floatArrayOf(pt.x, pt.y)
                    imageToScreenMatrix.mapPoints(arr)
                    PointF(arr[0], arr[1])
                }"""
text = text.replace(old_filter1, new_filter1)


old_logical1 = """                val detectedLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    refinedScreenPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else {
                    emptyList()
                }

                val filteredBalls = if (state.table.isVisible) {
                    detectedLogicalPoints.filter { state.table.isPointInside(it) }
                } else {
                    detectedLogicalPoints
                }"""

new_logical1 = """                val genericLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    refinedGenericPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else { emptyList() }
                
                val customLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    refinedCustomPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else { emptyList() }

                val finalGenericBalls = if (state.table.isVisible) {
                    genericLogicalPoints.filter { state.table.isPointInside(it) }
                } else { genericLogicalPoints }
                
                val finalCustomBalls = if (state.table.isVisible) {
                    customLogicalPoints.filter { state.table.isPointInside(it) }
                } else { customLogicalPoints }"""
text = text.replace(old_logical1, new_logical1)


old_vd1 = """                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt()) },
                    detectedCues = detectedCues,
                    cvMask = cvMask,
                    sourceImageWidth = imageProxy.width,
                    sourceImageHeight = imageProxy.height,"""

new_vd1 = """                var finalVisionData = VisionData(
                    genericBalls = finalGenericBalls,
                    customBalls = finalCustomBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredGenericObjects.map { it.boundingBox },
                    detectedCues = detectedCues,
                    cvMask = cvMask,
                    sourceImageWidth = inputImage.width,
                    sourceImageHeight = inputImage.height,"""
text = text.replace(old_vd1, new_vd1)


# Fix processArCpuImage
pa_block_old = """            val rawDetections = poolDetector.detect(bitmap)
            val detectedObjects = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""

pa_block_new = """            val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
            val genericObjects = Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detect(bitmap)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""
text = text.replace(pa_block_old, pa_block_new)

old_filter2 = """            val filteredObjects = detectedObjects.filter {
                val box = it.rect
                val er = getExpectedRadiusAtImageY(box.centerY(), state, imageToScreenMatrix)
                (box.width() * box.height()) <= 2 * Math.PI * er * er
            }

            val refinedScreenPoints = filteredObjects.mapNotNull { obj ->
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
            }.map { pt ->
                val arr = floatArrayOf(pt.x, pt.y)
                imageToScreenMatrix.mapPoints(arr)
                android.graphics.PointF(arr[0], arr[1])
            }"""

new_filter2 = """            val filteredGenericObjects = genericObjects.filter {
                val box = it.boundingBox
                val er = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
                (box.width() * box.height()) <= 2 * Math.PI * er * er
            }
            
            val filteredCustomBalls = customPoolBalls.filter {
                val box = it.rect
                val er = getExpectedRadiusAtImageY(box.centerY(), state, imageToScreenMatrix)
                (box.width() * box.height()) <= 2 * Math.PI * er * er
            }

            val refinedGenericPoints = filteredGenericObjects.mapNotNull { obj ->
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
            }.map { pt ->
                val arr = floatArrayOf(pt.x, pt.y)
                imageToScreenMatrix.mapPoints(arr)
                android.graphics.PointF(arr[0], arr[1])
            }
            
            val refinedCustomPoints = filteredCustomBalls.mapNotNull { obj ->
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
            }.map { pt ->
                val arr = floatArrayOf(pt.x, pt.y)
                imageToScreenMatrix.mapPoints(arr)
                android.graphics.PointF(arr[0], arr[1])
            }"""
text = text.replace(old_filter2, new_filter2)

old_logical2 = """            val logicalPoints = if (state.hasInverseMatrix) {
                val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                val tps = state.lensWarpTps
                refinedScreenPoints.map { sp ->
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (tps != null) ThinPlateSpline.applyWarp(tps, lp) else lp
                }
            } else {
                emptyList()
            }

            val filteredBalls = if (state.table.isVisible) {
                logicalPoints.filter { state.table.isPointInside(it) }
            } else {
                logicalPoints
            }"""

new_logical2 = """            val genericLogicalPoints = if (state.hasInverseMatrix) {
                val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                val tps = state.lensWarpTps
                refinedGenericPoints.map { sp ->
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (tps != null) ThinPlateSpline.applyWarp(tps, lp) else lp
                }
            } else { emptyList() }
            
            val customLogicalPoints = if (state.hasInverseMatrix) {
                val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                val tps = state.lensWarpTps
                refinedCustomPoints.map { sp ->
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (tps != null) ThinPlateSpline.applyWarp(tps, lp) else lp
                }
            } else { emptyList() }

            val finalGenericBalls = if (state.table.isVisible) {
                genericLogicalPoints.filter { state.table.isPointInside(it) }
            } else { genericLogicalPoints }
            
            val finalCustomBalls = if (state.table.isVisible) {
                customLogicalPoints.filter { state.table.isPointInside(it) }
            } else { customLogicalPoints }"""
text = text.replace(old_logical2, new_logical2)

old_vd2 = """            var newVisionData = VisionData(
                genericBalls = filteredBalls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = filteredObjects.map { android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt()) },
                cvMask = null,
                sourceImageWidth = image.width,
                sourceImageHeight = image.height,
                sourceImageRotation = rotationDegrees
            )"""

new_vd2 = """            var newVisionData = VisionData(
                genericBalls = finalGenericBalls,
                customBalls = finalCustomBalls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = filteredGenericObjects.map { it.boundingBox },
                detectedCues = detectedCues,
                cvMask = null,
                sourceImageWidth = inputImage.width,
                sourceImageHeight = inputImage.height,
                sourceImageRotation = rotationDegrees
            )"""
text = text.replace(old_vd2, new_vd2)


# Add back the generic object detector overload for refineBallCenter
text += """

    private fun refineBallCenter(
        detectedObject: DetectedObject,
        frame: Mat,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): PointF? {
        val box = detectedObject.boundingBox
        val roi = OCVRect(box.left, box.top, box.width(), box.height())

        if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > frame.cols() || roi.y + roi.height > frame.rows()) {
            return null
        }

        val expectedRadiusInImageCoords = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
        val tolerance = 0.5f
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        val roiMat = frame.submat(roi)
        Imgproc.morphologyEx(roiMat, roiMat, Imgproc.MORPH_OPEN, reusableMorphKernel)

        val refinedCenterInRoi = findBallByContour(
            roiMat, minRadius, maxRadius,
            state.cannyThreshold1.toDouble(), state.cannyThreshold2.toDouble()
        )

        roiMat.release()
        return refinedCenterInRoi?.let { PointF(it.x + roi.x, it.y + roi.y) }
    }
"""

with open("app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt", "w") as f:
    f.write(text)


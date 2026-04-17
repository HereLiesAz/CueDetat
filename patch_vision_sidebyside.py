with open("app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt", "r") as f:
    text = f.read()

# 1. Add poolDetector to constructor
from_text = """class VisionRepository @Inject constructor(
    private val pocketDetector: PocketDetector,
    @GenericDetector private val genericObjectDetector: ObjectDetector,
    private val dataStoreManager: DataStoreManager,"""
to_text = """class VisionRepository @Inject constructor(
    private val pocketDetector: PocketDetector,
    @GenericDetector private val genericObjectDetector: ObjectDetector,
    private val poolDetector: com.hereliesaz.cuedetat.data.TFLitePoolDetector,
    private val dataStoreManager: DataStoreManager,"""
text = text.replace(from_text, to_text)

# 2. Add bitmap to processImage
text = text.replace(
    'fun processImage(imageProxy: ImageProxy, state: CueDetatState) {',
    'fun processImage(imageProxy: ImageProxy, bitmap: android.graphics.Bitmap, state: CueDetatState) {'
)

# 3. Add toBitmap extension at the end of the class
text = text[:text.rfind('}')] + """
    private var arOutputBitmap: android.graphics.Bitmap? = null

    private fun android.media.Image.toBitmap(): android.graphics.Bitmap? {
        if (format != android.graphics.ImageFormat.YUV_420_888) return null

        val w = width
        val h = height
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val pixels = IntArray(w * h)
        for (row in 0 until h) {
            for (col in 0 until w) {
                val y = yBuf.get(row * yRowStride + col).toInt() and 0xFF
                val uvRow = row / 2
                val uvCol = col / 2
                val uvIdx = uvRow * uvRowStride + uvCol * uvPixelStride
                val u = (uBuf.get(uvIdx).toInt() and 0xFF) - 128
                val v = (vBuf.get(uvIdx).toInt() and 0xFF) - 128

                val r = (y + 1.370705f * v).toInt().coerceIn(0, 255)
                val g = (y - 0.698001f * v - 0.337633f * u).toInt().coerceIn(0, 255)
                val b = (y + 1.732446f * u).toInt().coerceIn(0, 255)
                pixels[row * w + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val bmp = arOutputBitmap?.takeIf { it.width == w && it.height == h }
            ?: android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888).also { arOutputBitmap = it }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun refineBallCenterPoolDetection(
        detectedObject: com.hereliesaz.cuedetat.data.PoolDetection,
        frame: org.opencv.core.Mat,
        state: CueDetatState,
        imageToScreenMatrix: android.graphics.Matrix
    ): android.graphics.PointF? {
        val box = detectedObject.rect
        val roi = OCVRect(box.left.toInt(), box.top.toInt(), box.width().toInt(), box.height().toInt())

        if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > frame.cols() || roi.y + roi.height > frame.rows()) {
            return null
        }

        val expectedRadiusInImageCoords = getExpectedRadiusAtImageY(box.centerY(), state, imageToScreenMatrix)
        val tolerance = 0.5f
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        val roiMat = frame.submat(roi)
        org.opencv.imgproc.Imgproc.morphologyEx(roiMat, roiMat, org.opencv.imgproc.Imgproc.MORPH_OPEN, reusableMorphKernel)

        val refinedCenterInRoi = findBallByContour(
            roiMat, minRadius, maxRadius,
            state.cannyThreshold1.toDouble(), state.cannyThreshold2.toDouble()
        )

        roiMat.release()
        return refinedCenterInRoi?.let { android.graphics.PointF(it.x.toFloat() + roi.x, it.y.toFloat() + roi.y) }
    }
}
"""


# 4. Modify processImage logic
pi_old = """            val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))"""
pi_new = """            val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detect(bitmap)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""
text = text.replace(pi_old, pi_new, 1)

filter1_old = """                val refinedScreenPoints = filteredDetectedObjects.mapNotNull { detectedObject ->
                    refineBallCenter(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }

                val detectedLogicalPoints = if (state.hasInverseMatrix) {
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

filter1_new = """                val refinedScreenPoints = filteredDetectedObjects.mapNotNull { detectedObject ->
                    refineBallCenter(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }
                
                val filteredCustomPoolBalls = customPoolBalls.filter {
                    val box = it.rect
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val customScreenPoints = filteredCustomPoolBalls.mapNotNull { detectedObject ->
                    refineBallCenterPoolDetection(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }

                val detectedLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    refinedScreenPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else { emptyList() }
                
                val customLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    customScreenPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else { emptyList() }

                val filteredBalls = if (state.table.isVisible) {
                    detectedLogicalPoints.filter { state.table.isPointInside(it) }
                } else { detectedLogicalPoints }
                
                val finalCustomBalls = if (state.table.isVisible) {
                    customLogicalPoints.filter { state.table.isPointInside(it) }
                } else { customLogicalPoints }"""
text = text.replace(filter1_old, filter1_new)


vd1_old = """                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                    cvMask = cvMask,
                    sourceImageWidth = inputImage.width,
                    sourceImageHeight = inputImage.height,
                    sourceImageRotation = rotationDegrees
                )"""

vd1_new = """                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    customBalls = finalCustomBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                    detectedCues = detectedCues,
                    cvMask = cvMask,
                    sourceImageWidth = inputImage.width,
                    sourceImageHeight = inputImage.height,
                    sourceImageRotation = rotationDegrees
                )"""
text = text.replace(vd1_old, vd1_new)

# 5. Modify processArCpuImage logic
pacpu_old = """            val detectedObjects = com.google.android.gms.tasks.Tasks.await(genericObjectDetector.process(inputImage))"""
pacpu_new = """            val bitmap = image.toBitmap() ?: return
            val detectedObjects = com.google.android.gms.tasks.Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detect(bitmap)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""
text = text.replace(pacpu_old, pacpu_new)


filter2_old = """            val refinedScreenPoints = filteredObjects.mapNotNull { obj ->
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
            }.map { pt ->
                val arr = floatArrayOf(pt.x, pt.y)
                imageToScreenMatrix.mapPoints(arr)
                android.graphics.PointF(arr[0], arr[1])
            }

            val logicalPoints = if (state.hasInverseMatrix) {
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

filter2_new = """            val refinedScreenPoints = filteredObjects.mapNotNull { obj ->
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
            }.map { pt ->
                val arr = floatArrayOf(pt.x, pt.y)
                imageToScreenMatrix.mapPoints(arr)
                android.graphics.PointF(arr[0], arr[1])
            }
            
            val filteredCustomPoolBalls = customPoolBalls.filter {
                val box = it.rect
                val expectedRadius = getExpectedRadiusAtImageY(
                    box.centerY(), state, imageToScreenMatrix
                )
                val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                (box.width() * box.height()) <= maxAllowedArea
            }

            val customScreenPoints = filteredCustomPoolBalls.mapNotNull { detectedObject ->
                refineBallCenterPoolDetection(detectedObject, matToUse, state, imageToScreenMatrix)
            }.map { pointInImageCoords ->
                val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                imageToScreenMatrix.mapPoints(screenPointArray)
                PointF(screenPointArray[0], screenPointArray[1])
            }

            val logicalPoints = if (state.hasInverseMatrix) {
                val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                val tps = state.lensWarpTps
                refinedScreenPoints.map { sp ->
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (tps != null) ThinPlateSpline.applyWarp(tps, lp) else lp
                }
            } else { emptyList() }
            
            val customLogicalPoints = if (state.hasInverseMatrix) {
                val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                val tps = state.lensWarpTps
                customScreenPoints.map { sp ->
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (tps != null) ThinPlateSpline.applyWarp(tps, lp) else lp
                }
            } else { emptyList() }

            val filteredBalls = if (state.table.isVisible) {
                logicalPoints.filter { state.table.isPointInside(it) }
            } else { logicalPoints }
            
            val finalCustomBalls = if (state.table.isVisible) {
                customLogicalPoints.filter { state.table.isPointInside(it) }
            } else { customLogicalPoints }"""
text = text.replace(filter2_old, filter2_new)


vd2_old = """            var newVisionData = VisionData(
                genericBalls = filteredBalls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = filteredObjects.map { it.boundingBox },
                cvMask = null,
                sourceImageWidth = inputImage.width,
                sourceImageHeight = inputImage.height,
                sourceImageRotation = rotationDegrees
            )"""

vd2_new = """            var newVisionData = VisionData(
                genericBalls = filteredBalls,
                customBalls = finalCustomBalls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = filteredObjects.map { it.boundingBox },
                detectedCues = detectedCues,
                cvMask = null,
                sourceImageWidth = inputImage.width,
                sourceImageHeight = inputImage.height,
                sourceImageRotation = rotationDegrees
            )"""
text = text.replace(vd2_old, vd2_new)

with open("app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt", "w") as f:
    f.write(text)


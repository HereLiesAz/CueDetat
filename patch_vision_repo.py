import re

with open("app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt", "r") as f:
    text = f.read()

# Imports removal (MLKit, etc)
text = re.sub(r'import com\.google\.mlkit\.vision.*?\n', '', text)
text = re.sub(r'import com\.hereliesaz\.cuedetat\.di\.GenericDetector\n', '', text)
text = re.sub(r'import com\.google\.android\.gms.*?\n', '', text)

# Inject poolDetector instead of genericObjectDetector
text = re.sub(
    r'@GenericDetector private val genericObjectDetector: ObjectDetector',
    r'private val poolDetector: TFLitePoolDetector',
    text
)

# Signature of processImage
text = text.replace(
    'fun processImage(imageProxy: ImageProxy, state: CueDetatState) {',
    'fun processImage(imageProxy: ImageProxy, bitmap: android.graphics.Bitmap, state: CueDetatState) {'
)

# Block 1 Replacement
block1_old = """            val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))"""
block1_new = """            val rawDetections = poolDetector.detect(bitmap)
            val detectedObjects = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""
text = text.replace(block1_old, block1_new)

# Sub-block inside block 1
filter1_old = """                val filteredDetectedObjects = detectedObjects.filter {
                    val box = it.boundingBox
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY().toFloat(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }"""
filter1_new = """                val filteredDetectedObjects = detectedObjects.filter {
                    val box = it.rect
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }"""
text = text.replace(filter1_old, filter1_new)

# VisionData construction in block 1
vd1_old = """                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },"""
vd1_new = """                    detectedBoundingBoxes = filteredDetectedObjects.map { android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt()) },
                    detectedCues = detectedCues,"""
text = text.replace(vd1_old, vd1_new)


# Block 2 Replacement
block2_old = """            val detectedObjects = com.google.android.gms.tasks.Tasks.await(genericObjectDetector.process(inputImage))"""
block2_new = """            val rawDetections = poolDetector.detect(bitmap)
            val detectedObjects = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }"""
text = text.replace(block2_old, block2_new)

filter2_old = """            val filteredObjects = detectedObjects.filter {
                val box = it.boundingBox
                val er = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
                (box.width() * box.height()) <= 2 * Math.PI * er * er
            }"""
filter2_new = """            val filteredObjects = detectedObjects.filter {
                val box = it.rect
                val er = getExpectedRadiusAtImageY(box.centerY(), state, imageToScreenMatrix)
                (box.width() * box.height()) <= 2 * Math.PI * er * er
            }"""
text = text.replace(filter2_old, filter2_new)

vd2_old = """                detectedBoundingBoxes = filteredObjects.map { it.boundingBox },"""
vd2_new = """                detectedBoundingBoxes = filteredObjects.map { android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt()) },
                detectedCues = detectedCues,"""
text = text.replace(vd2_old, vd2_new)


# refineBallCenter signature and logic
refine_sig_old = """    private fun refineBallCenter(
        detectedObject: DetectedObject,
        frame: Mat,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): PointF? {"""
refine_sig_new = """    private fun refineBallCenter(
        detectedObject: PoolDetection,
        frame: Mat,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): PointF? {"""
text = text.replace(refine_sig_old, refine_sig_new)

refine_box_old = """        val box = detectedObject.boundingBox
        val roi = OCVRect(box.left, box.top, box.width(), box.height())"""
refine_box_new = """        val box = detectedObject.rect
        val roi = OCVRect(box.left.toInt(), box.top.toInt(), box.width().toInt(), box.height().toInt())"""
text = text.replace(refine_box_old, refine_box_new)

refine_radius_old = """        val expectedRadiusInImageCoords = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)"""
refine_radius_new = """        val expectedRadiusInImageCoords = getExpectedRadiusAtImageY(box.centerY(), state, imageToScreenMatrix)"""
text = text.replace(refine_radius_old, refine_radius_new)


with open("app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt", "w") as f:
    f.write(text)


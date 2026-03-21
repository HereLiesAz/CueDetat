// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Matrix
import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.PocketCluster
import com.hereliesaz.cuedetat.domain.PocketId
import com.hereliesaz.cuedetat.domain.TableGeometryFitter
import com.hereliesaz.cuedetat.domain.TableScanModel
import com.hereliesaz.cuedetat.domain.TpsWarpData
import com.hereliesaz.cuedetat.domain.decomposeHomography
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject

/** Min observations per cluster before geometry fitting is attempted. */
private const val MIN_OBSERVATIONS_TO_FIT = 3

/** Max distance (logical inches) to merge a new detection into an existing cluster. */
private const val CLUSTER_MERGE_DISTANCE = 3.0f

@HiltViewModel
class TableScanViewModel @Inject constructor(
    private val tableScanRepository: TableScanRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<Map<PocketId, Boolean>>(emptyMap())
    val scanProgress: StateFlow<Map<PocketId, Boolean>> = _scanProgress.asStateFlow()

    private val _selectedTableSize = MutableStateFlow<TableSize>(TableSize.EIGHT_FT)
    val selectedTableSize: StateFlow<TableSize> = _selectedTableSize.asStateFlow()

    /**
     * Emits events when scan is complete. Collected by TableScanScreen.
     * Emits LoadTableScan(model) first, then ApplyQuickAlign. Both must be dispatched
     * before the screen is dismissed — the screen must NOT close inside this collector.
     * Use scanComplete to trigger dismissal after all events have been dispatched.
     */
    private val _scanResult = MutableSharedFlow<MainScreenEvent>()
    val scanResult = _scanResult.asSharedFlow()

    /** Flips to true after completeScan emits all events. Screen dismisses on this signal. */
    private val _scanComplete = MutableStateFlow(false)
    val scanComplete: StateFlow<Boolean> = _scanComplete.asStateFlow()

    // Mutable cluster accumulator: identity → running cluster.
    private val clusters = mutableMapOf<PocketId, MutableList<PointF>>()

    // Felt colour sampled from recent frames (rolling mean HSV of centre crop).
    @Volatile private var lastFeltHsv: FloatArray = floatArrayOf(120f, 0.5f, 0.4f)

    // Last known inversePitchMatrix from the main state — set by the screen on each recompose.
    @Volatile private var inversePitchMatrix: Matrix? = null
    @Volatile private var hasInverseMatrix: Boolean = false
    @Volatile private var viewWidth: Int = 0
    @Volatile private var viewHeight: Int = 0

    // Cached calibration data
    @Volatile private var cameraMatrix: Mat? = null
    @Volatile private var distCoeffs: Mat? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.calibrationDataFlow.collect { data ->
                cameraMatrix = data.first
                distCoeffs = data.second
            }
        }
    }

    fun updateStateSnapshot(inverse: Matrix?, hasInverse: Boolean, vw: Int, vh: Int) {
        inversePitchMatrix = inverse
        hasInverseMatrix = hasInverse
        viewWidth = vw
        viewHeight = vh
    }

    /** Called by TableScanAnalyzer each frame with the mean HSV of the centre crop of the felt. */
    fun onFeltColorSampled(hsv: FloatArray) { lastFeltHsv = hsv }

    fun onTableSizeSelected(size: TableSize) {
        _selectedTableSize.value = size
    }

    /**
     * Called by TableScanAnalyzer on each frame.
     * Projects image-space blobs to logical space and merges into clusters.
     */
    fun onFrame(
        imagePoints: List<PointF>,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ) {
        if (!hasInverseMatrix) return
        val inverse = inversePitchMatrix ?: return

        viewModelScope.launch(Dispatchers.Default) {
            // Step 1: Build imageToScreenMatrix — mirrors VisionRepository.getTransformationMatrix exactly.
            val imageToScreen = Matrix().apply {
                postScale(
                    viewWidth.toFloat() / imageWidth.toFloat(),
                    viewHeight.toFloat() / imageHeight.toFloat()
                )
            }

            // Step 2: Project image → screen → logical for each detection.
            // Use your calibration matrix to undistort the raw image points first!
            val rawPointsMat = MatOfPoint2f(*imagePoints.map { Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray())
            val undistortedPointsMat = MatOfPoint2f()

            val camMat = cameraMatrix
            val distMat = distCoeffs
            if (camMat != null && distMat != null && !camMat.empty() && !distMat.empty()) {
                // We pass cameraMatrix as the 'P' (newCameraMatrix) parameter as well,
                // otherwise OpenCV returns normalized coordinates instead of pixel coordinates!
                Calib3d.undistortPoints(rawPointsMat, undistortedPointsMat, camMat, distMat, Mat(), camMat)
            } else {
                rawPointsMat.copyTo(undistortedPointsMat) // Fallback to pinhole if no calibration exists
            }

            val undistortedList = undistortedPointsMat.toList()
            val screenPts = FloatArray(undistortedList.size * 2)

            undistortedList.forEachIndexed { i, p ->
                screenPts[i * 2] = p.x.toFloat()
                screenPts[i * 2 + 1] = p.y.toFloat()
            }
            imageToScreen.mapPoints(screenPts)

            val logicalPts = undistortedList.indices.map { i ->
                val screenPt = PointF(screenPts[i * 2], screenPts[i * 2 + 1])
                screenToLogical(screenPt, inverse)
            }

            // Step 3: Merge into clusters.
            logicalPts.forEach { logicalPt -> mergeIntoCluster(logicalPt) }

            // Step 4: Update UI progress — count clusters with enough observations.
            val stableIds = clusters.entries
                .filter { it.value.size >= MIN_OBSERVATIONS_TO_FIT }
                .map { it.key }
                .toSet()
            withContext(Dispatchers.Main) {
                _scanProgress.value = stableIds.associateWith { true }
            }

            // Step 5: Attempt fit when 6 clusters are stable.
            val stableClusters = clusters.filter { it.value.size >= MIN_OBSERVATIONS_TO_FIT }
            if (stableClusters.size >= 6) {
                val centerPts = stableClusters.values.map { observations ->
                    PointF(
                        observations.sumOf { it.x.toDouble() }.toFloat() / observations.size,
                        observations.sumOf { it.y.toDouble() }.toFloat() / observations.size
                    )
                }
                val fitResult = TableGeometryFitter.fit(centerPts) ?: return@launch

                // Re-key clusters by identified PocketId.
                val identifiedCenters = fitResult.associate { (id, pt) -> id to pt }
                completeScan(identifiedCenters, imageWidth.toFloat(), imageHeight.toFloat())
            }
        }
    }

    private fun mergeIntoCluster(pt: PointF) {
        for ((_, observations) in clusters) {
            val mean = PointF(
                observations.sumOf { it.x.toDouble() }.toFloat() / observations.size,
                observations.sumOf { it.y.toDouble() }.toFloat() / observations.size
            )
            val dist = kotlin.math.hypot((pt.x - mean.x).toDouble(), (pt.y - mean.y).toDouble()).toFloat()
            if (dist <= CLUSTER_MERGE_DISTANCE) {
                observations.add(pt)
                return
            }
        }
        // New cluster — only create up to 6 (one per PocketId).
        // Discard detections beyond 6 to prevent false positives from overwriting real clusters.
        if (clusters.size < PocketId.values().size) {
            clusters[PocketId.values()[clusters.size]] = mutableListOf(pt)
        }
    }

    private suspend fun completeScan(
        identifiedLogical: Map<PocketId, PointF>,
        imgWidth: Float,
        imgHeight: Float
    ) {
        val tableSize = _selectedTableSize.value
        val logicalTable = Table(tableSize, true)

        // Table.pockets ordering (verified from Table.kt):
        //   pockets[0] = TL (-halfW, -halfH)
        //   pockets[1] = TR ( halfW, -halfH)
        //   pockets[2] = BL (-halfW,  halfH)
        //   pockets[3] = BR ( halfW,  halfH)
        //   pockets[4] = SL (side left)
        //   pockets[5] = SR (side right)
        val pocketOrder = listOf(
            PocketId.TL to logicalTable.pockets[0],
            PocketId.TR to logicalTable.pockets[1],
            PocketId.BL to logicalTable.pockets[2],
            PocketId.BR to logicalTable.pockets[3],
            PocketId.SL to logicalTable.pockets[4],
            PocketId.SR to logicalTable.pockets[5]
        )

        // Build homography: detected logical → true logical.
        val srcMat = MatOfPoint2f()
        val dstMat = MatOfPoint2f()
        val srcList = pocketOrder.mapNotNull { (id, _) ->
            identifiedLogical[id]?.let { Point(it.x.toDouble(), it.y.toDouble()) }
        }
        val dstList = pocketOrder.map { (_, pt) -> Point(pt.x.toDouble(), pt.y.toDouble()) }
        if (srcList.size != 6) return
        srcMat.fromList(srcList)
        dstMat.fromList(dstList)

        val homography = Calib3d.findHomography(srcMat, dstMat, Calib3d.RANSAC, 3.0)
        if (homography.empty()) return

        // Calculate physical dimensions in logical space (inches)
        val logicalWidth = logicalTable.pockets[1].x - logicalTable.pockets[0].x // TR.x - TL.x
        val logicalHeight = logicalTable.pockets[2].y - logicalTable.pockets[0].y // BL.y - TL.y

        // Pass the logical dimensions so the translation vector scales correctly
        val (translation, rotation, scale) = decomposeHomography(homography, logicalWidth, logicalHeight)

        // Residual TPS: estimated logical → true logical.
        val estimatedDst = MatOfPoint2f()
        Core.perspectiveTransform(srcMat, estimatedDst, homography)
        val estimatedLogical = estimatedDst.toList().map { PointF(it.x.toFloat(), it.y.toFloat()) }
        val trueLogical = dstList.map { PointF(it.x.toFloat(), it.y.toFloat()) }
        val tpsWarpData = TpsWarpData(srcPoints = estimatedLogical, dstPoints = trueLogical)

        // Build PocketClusters with initial observation data.
        val pocketClusters = pocketOrder.map { (id, _) ->
            PocketCluster(
                identity = id,
                logicalPosition = identifiedLogical[id]!!,
                observationCount = clusters[id]?.size ?: 1,
                variance = 1.0f
            )
        }

        val feltColorHsv = lastFeltHsv.toList()
        val location = tableScanRepository.getCurrentLocation()
        val model = TableScanModel(
            pockets = pocketClusters,
            lensWarpTps = tpsWarpData,
            tableSize = tableSize,
            feltColorHsv = feltColorHsv,
            scanLatitude = location?.first,
            scanLongitude = location?.second
        )
        tableScanRepository.save(model)

        // Invariant: emit LoadTableScan FIRST so tableScanModel is set in state before pose is applied.
        _scanResult.emit(MainScreenEvent.LoadTableScan(model))
        _scanResult.emit(MainScreenEvent.ApplyQuickAlign(translation, rotation, scale, tpsWarpData))
        // Signal screen dismissal AFTER both events are enqueued.
        _scanComplete.value = true
    }

    fun resetScan() {
        clusters.clear()
        _scanProgress.value = emptyMap()
        _scanComplete.value = false
    }

    // ------ Coordinate helpers ------

    private fun screenToLogical(screen: PointF, inverse: Matrix): PointF {
        val arr = floatArrayOf(screen.x, screen.y)
        inverse.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
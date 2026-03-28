// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Matrix
import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.PocketCluster
import com.hereliesaz.cuedetat.domain.PocketId
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
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
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
    val pocketDetector: PocketDetector
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

    private val _scanComplete = MutableStateFlow(false)
    val scanComplete: StateFlow<Boolean> = _scanComplete.asStateFlow()

    private val _capturedFeltHsv = MutableStateFlow<FloatArray?>(null)
    val capturedFeltHsv: StateFlow<FloatArray?> = _capturedFeltHsv.asStateFlow()

    private val _selectedSampleIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSampleIds: StateFlow<Set<String>> = _selectedSampleIds.asStateFlow()

    fun toggleSampleSelection(id: String) {
        val current = _selectedSampleIds.value
        _selectedSampleIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectSamples(ids: Set<String>) {
        _selectedSampleIds.value = ids
    }

    fun clearSelection() {
        _selectedSampleIds.value = emptySet()
    }

    fun deleteSelectedSamples() {
        val ids = _selectedSampleIds.value
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                _scanResult.emit(MainScreenEvent.DeleteFeltSamples(ids))
                clearSelection()
            }
        }
    }

    fun moveSample(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            _scanResult.emit(MainScreenEvent.MoveFeltSample(fromIndex, toIndex))
        }
    }

    // Mutable cluster accumulator: identity → running cluster.
    private val clusters = mutableMapOf<PocketId, MutableList<PointF>>()

    // Felt colour sampled from recent frames (rolling mean HSV of centre crop).
    @Volatile private var lastFeltHsv: FloatArray = floatArrayOf(120f, 0.5f, 0.4f)

    // Last known inversePitchMatrix from the main state — set by the screen on each recompose.
    @Volatile private var inversePitchMatrix: Matrix? = null
    @Volatile private var hasInverseMatrix: Boolean = false
    @Volatile private var viewWidth: Int = 0
    @Volatile private var viewHeight: Int = 0

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
            val screenPts = FloatArray(imagePoints.size * 2)
            imagePoints.forEachIndexed { i, p -> screenPts[i * 2] = p.x; screenPts[i * 2 + 1] = p.y }
            imageToScreen.mapPoints(screenPts)

            val logicalPts = imagePoints.indices.map { i ->
                val screenPt = PointF(screenPts[i * 2], screenPts[i * 2 + 1])
                screenToLogical(screenPt, inverse)
            }

            // Step 3: Merge into clusters (accumulated for manual geometry fitting if needed).
            logicalPts.forEach { logicalPt -> mergeIntoCluster(logicalPt) }
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

        // Decompose using logical dimensions where center is (0,0), canceling out the canvasCenter offset logic intended for pixel coordinates
        val (translation, rotation, scale) = decomposeHomography(homography, 0f, 0f)

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
        _capturedFeltHsv.value = null
    }

    // ------ Coordinate helpers ------

    /**
     * One-click felt capture. 
     * Immediately completes the scan using the captured felt color and a default table pose.
     */
    fun captureFeltAndComplete() {
        viewModelScope.launch {
            val hsv = lastFeltHsv.toList()
            _scanResult.emit(MainScreenEvent.AddFeltSample(hsv))

            val tableSize = _selectedTableSize.value
            
            // Default model: Identity warp, 6 placeholder logical pockets.
            val tpsWarpData = TpsWarpData(
                srcPoints = listOf(PointF(0f, 0f)),
                dstPoints = listOf(PointF(0f, 0f))
            )
            
            val logicalTable = Table(tableSize, true)
            val defaultClusters = logicalTable.pockets.mapIndexed { i, pt ->
                PocketCluster(
                    identity = PocketId.values()[i],
                    logicalPosition = PointF(pt.x, pt.y),
                    observationCount = 1,
                    variance = 0f
                )
            }
            
            val location = tableScanRepository.getCurrentLocation()
            val model = TableScanModel(
                pockets = defaultClusters,
                lensWarpTps = tpsWarpData,
                tableSize = tableSize,
                feltColorHsv = hsv,
                scanLatitude = location?.first,
                scanLongitude = location?.second
            )
            
            tableScanRepository.save(model)
            
            // Lock the color and load the model.
            _scanResult.emit(MainScreenEvent.LockColor(lastFeltHsv, floatArrayOf(0.01f, 0.01f, 0.01f)))
            _scanResult.emit(MainScreenEvent.LoadTableScan(model))
            _scanResult.emit(MainScreenEvent.StartArTracking)

            // Signal the UI to show the captured color, then close after the animation plays.
            _capturedFeltHsv.value = lastFeltHsv
            _scanComplete.value = true
        }
    }

    private fun screenToLogical(screen: PointF, inverse: Matrix): PointF {
        val arr = floatArrayOf(screen.x, screen.y)
        inverse.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
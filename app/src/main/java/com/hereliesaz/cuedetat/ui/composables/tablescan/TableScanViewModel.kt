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
import com.hereliesaz.cuedetat.domain.TableGeometryFitter
import com.hereliesaz.cuedetat.domain.EdgeGeometryFitter
import com.hereliesaz.cuedetat.domain.decomposeHomography
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject
import kotlin.math.sqrt

/** Min observations per cluster before geometry fitting is attempted. */
private const val MIN_OBSERVATIONS_TO_FIT = 3

/** Max distance (logical inches) to merge a new detection into an existing cluster. */
private const val CLUSTER_MERGE_DISTANCE = 3.0f

enum class ScanStep {
    FELT_CAPTURE,
    POCKET_GUIDE,
    AUTO_READY
}

@HiltViewModel
class TableScanViewModel @Inject constructor(
    private val tableScanRepository: TableScanRepository,
    val pocketDetector: PocketDetector
) : ViewModel() {

    private val _scanStep = MutableStateFlow(ScanStep.FELT_CAPTURE)
    val scanStep: StateFlow<ScanStep> = _scanStep.asStateFlow()

    private val _currentPocketTarget = MutableStateFlow<PocketId?>(null)
    val currentPocketTarget: StateFlow<PocketId?> = _currentPocketTarget.asStateFlow()

    private val _mlConfidence = MutableStateFlow(0f)
    val mlConfidence: StateFlow<Float> = _mlConfidence.asStateFlow()

    private val _mlTableBoundary = MutableStateFlow<android.graphics.RectF?>(null)

    private val _darknessConfidence = MutableStateFlow(0f)
    val darknessConfidence: StateFlow<Float> = _darknessConfidence.asStateFlow()

    @Volatile private var latestCenterHistogram: List<Float> = emptyList()
    private val capturedHistograms = mutableMapOf<PocketId, List<Float>>()

    private val _scanProgress = MutableStateFlow<Map<PocketId, Boolean>>(emptyMap())

    private val _selectedTableSize = MutableStateFlow(TableSize.EIGHT_FT)

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

    init {
        // Resume partial scan if it exists
        viewModelScope.launch {
            val partial = tableScanRepository.loadPartialScan()
            if (partial != null) {
                val (step, savedPoints) = partial
                _scanStep.value = step
                savedPoints.forEach { (id, pt) ->
                    clusters[id] = mutableListOf(pt)
                    _scanProgress.value += (id to true)
                }
                
                // If it was POCKET_GUIDE, set the target to the first missing pocket
                if (step == ScanStep.POCKET_GUIDE) {
                    val nextIdx = PocketId.entries.toTypedArray().indexOfFirst { !savedPoints.containsKey(it) }
                    if (nextIdx != -1) {
                        _currentPocketTarget.value = PocketId.entries.toTypedArray()[nextIdx]
                    }
                }
            }
        }
    }

    private val _capturedFeltHsv = MutableStateFlow<FloatArray?>(null)
    val capturedFeltHsv: StateFlow<FloatArray?> = _capturedFeltHsv.asStateFlow()

    private val _selectedSampleIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSampleIds: StateFlow<Set<String>> = _selectedSampleIds.asStateFlow()

    fun toggleSampleSelection(id: String) {
        val current = _selectedSampleIds.value
        _selectedSampleIds.value = if (current.contains(id)) current - id else current + id
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

    // Mutable cluster accumulator: identity → running cluster.
    private val clusters = mutableMapOf<PocketId, MutableList<PointF>>()

    // Felt colour sampled from recent frames (rolling mean HSV of centre crop).
    @Volatile private var lastFeltHsv: FloatArray = floatArrayOf(120f, 0.5f, 0.4f)

    @Volatile private var currentViewOffset = PointF(0f, 0f)
    @Volatile private var currentRotation = 0f
    @Volatile private var currentZoomSlider = 0f

    private var inversePitchMatrix: Matrix? = null
    private var hasInverseMatrix: Boolean = false
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    fun updateStateSnapshot(
        inverse: Matrix?, 
        hasInverse: Boolean, 
        vw: Int, 
        vh: Int,
        viewOffset: PointF,
        rotation: Float,
        zoomSlider: Float
    ) {
        inversePitchMatrix = inverse
        hasInverseMatrix = hasInverse
        viewWidth = vw
        viewHeight = vh
        currentViewOffset = viewOffset
        currentRotation = rotation
        currentZoomSlider = zoomSlider
    }

    /** Called by TableScanAnalyzer each frame with the mean HSV of the centre crop of the felt. */
    fun onFeltColorSampled(hsv: FloatArray) { lastFeltHsv = hsv }

    fun onCenterVSampled(normalizedV: Float, histogram: List<Float>) {
        latestCenterHistogram = histogram
        val feltV = _capturedFeltHsv.value?.get(2) ?: lastFeltHsv[2]
        if (feltV < 0.05f) return
        _darknessConfidence.value = (1f - (normalizedV / feltV)).coerceIn(0f, 1f)
    }

    /**
     * Called by TableScanAnalyzer on each frame.
     * Projects image-space blobs to logical space and merges into clusters.
     */
    fun onFrame(
        imagePoints: List<PointF>,
        edges: List<Pair<PointF, PointF>>?,
        tableBoundary: android.graphics.RectF?,
        confidence: Float,
        imageWidth: Int,
        imageHeight: Int
    ) {
        if (!hasInverseMatrix) return
        val inverse = inversePitchMatrix ?: return

        _mlConfidence.value = confidence
        _mlTableBoundary.value = tableBoundary

        viewModelScope.launch(Dispatchers.Default) {
            var stableCount = clusters.count { it.value.size >= MIN_OBSERVATIONS_TO_FIT }

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
                Perspective.screenToLogical(screenPt, inverse)
            }

            // Step 2.5: Edge-based Alignment Refinement (Fallback Step)
            if (!edges.isNullOrEmpty() && stableCount < 6) {
                val edgeLogicalPts = edges.flatMap { listOf(it.first, it.second) }.map { imgPt ->
                    val sPts = floatArrayOf(imgPt.x, imgPt.y)
                    imageToScreen.mapPoints(sPts)
                    Perspective.screenToLogical(PointF(sPts[0], sPts[1]), inverse)
                }

                val logicalTable = Table(_selectedTableSize.value, true)
                val refinement = EdgeGeometryFitter.fitEdges(
                    edgeLogicalPts, 
                    logicalTable.logicalWidth, 
                    logicalTable.logicalHeight
                )

                if (refinement != null) {
                    // Pull the table toward the edges with a low-pass filter to prevent jitter.
                    // We only apply this if we are not yet 'locked' by stable pockets.
                    
                    val nextTranslateX = currentViewOffset.x + refinement.translationDelta.x * 0.1f
                    val nextTranslateY = currentViewOffset.y + refinement.translationDelta.y * 0.1f
                    val nextRotation = currentRotation + refinement.rotationDelta * 0.1f
                    
                    _scanResult.emit(MainScreenEvent.UpdateArPose(
                        translation = androidx.compose.ui.geometry.Offset(nextTranslateX, nextTranslateY),
                        rotation = nextRotation,
                        scale = 1.0f // We keep scale at 1.0 for now as it's primarily handled by ZoomSlider
                    ))
                }
            }

            // Step 3: Merge into clusters.
            logicalPts.forEach { logicalPt -> mergeIntoCluster(logicalPt) }

            // Step 4: Update UI progress (# of stable clusters / 6).
            stableCount = clusters.count { it.value.size >= MIN_OBSERVATIONS_TO_FIT }
            _scanProgress.value = PocketId.entries.toTypedArray()
                .take(stableCount)
                .associateWith { true }

            // Step 5: Auto-fit and complete when 6 clusters are stable.
            if (stableCount >= 6 && !_scanComplete.value) {
                val stableClusters = clusters.filter { it.value.size >= MIN_OBSERVATIONS_TO_FIT }
                val centerPts = stableClusters.values.map { observations ->
                    PointF(
                        observations.sumOf { it.x.toDouble() }.toFloat() / observations.size,
                        observations.sumOf { it.y.toDouble() }.toFloat() / observations.size
                    )
                }
                val fitResult = TableGeometryFitter.fit(centerPts) ?: return@launch
                val identifiedCenters = fitResult.associate { (id, pt) -> id to pt }
                completeScan(identifiedCenters)
            }
        }
    }

    private fun mergeIntoCluster(pt: PointF) {
        // Find existing cluster within distance
        val nearest = clusters.minByOrNull { (_, observations) ->
            val meanX = observations.map { it.x }.average().toFloat()
            val meanY = observations.map { it.y }.average().toFloat()
            val dx = pt.x - meanX
            val dy = pt.y - meanY
            dx * dx + dy * dy
        }

        if (nearest != null) {
            val observations = nearest.value
            val meanX = observations.map { it.x }.average().toFloat()
            val meanY = observations.map { it.y }.average().toFloat()
            val dist = sqrt(((pt.x - meanX) * (pt.x - meanX) + (pt.y - meanY) * (pt.y - meanY)).toDouble()).toFloat()
            
            if (dist < CLUSTER_MERGE_DISTANCE) {
                observations.add(pt)
                return
            }
        }
        // New cluster — only create up to 6 (one per PocketId).
        // Discard detections beyond 6 to prevent false positives from overwriting real clusters.
        if (clusters.size < PocketId.entries.size) {
            clusters[PocketId.entries[clusters.size]] = mutableListOf(pt)
        }
    }

    private suspend fun completeScan(
        identifiedLogical: Map<PocketId, PointF>
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
            scanLongitude = location?.second,
            pocketSurroundHistograms = capturedHistograms.toMap(),
            calibrationTimestamp = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            tableScanRepository.save(model)
            tableScanRepository.clearPartialScan()
        }

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
        _scanStep.value = ScanStep.FELT_CAPTURE
        _currentPocketTarget.value = null
        _mlConfidence.value = 0f
        _mlTableBoundary.value = null
        tableScanRepository.clearPartialScan()
    }

    fun startManualHoleCapture() {
        clusters.clear()
        _scanProgress.value = emptyMap()
        _scanComplete.value = false
        _scanStep.value = ScanStep.POCKET_GUIDE
        _currentPocketTarget.value = PocketId.entries.toTypedArray()[0]
        _mlConfidence.value = 0f
        _mlTableBoundary.value = null
        tableScanRepository.clearPartialScan()
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
                    identity = PocketId.entries[i],
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
            
            withContext(Dispatchers.IO) {
                tableScanRepository.save(model)
                tableScanRepository.clearPartialScan()
            }
            
            // Convert Android Compose HSV (0-360, 0-1, 0-1) to OpenCV HSV (0-180, 0-255, 0-255)
            val opencvHsv = floatArrayOf(
                lastFeltHsv[0] / 2f,
                lastFeltHsv[1] * 255f,
                lastFeltHsv[2] * 255f
            )

            // Lock the color and place the table immediately with the default pose.
            _scanResult.emit(MainScreenEvent.LockColor(opencvHsv, floatArrayOf(5.0f, 30.0f, 30.0f)))
            _scanResult.emit(MainScreenEvent.LoadTableScan(model))
            
            // Explicitly place the table center at the current view center (0,0 translation)
            _scanResult.emit(MainScreenEvent.UpdateArPose(
                translation = androidx.compose.ui.geometry.Offset(0f, 0f), 
                rotation = 0f, 
                scale = 1.0f
            ))
            
            _capturedFeltHsv.value = lastFeltHsv

            // Instead of finishing, transition to Guided Pocket mode
            _scanStep.value = ScanStep.POCKET_GUIDE
            _currentPocketTarget.value = PocketId.entries.toTypedArray()[0]
        }
    }

    /**
     * User clicks to capture the pocket currently under the reticle.
     */
    fun captureCurrentPocket() {
        val inverse = inversePitchMatrix ?: return
        val step = _scanStep.value
        val currentId = _currentPocketTarget.value ?: return

        if (step != ScanStep.POCKET_GUIDE) return

        viewModelScope.launch {
            // Screen center is (viewWidth/2, viewHeight/2)
            val screenCenter = PointF(viewWidth / 2f, viewHeight / 2f)
            val logicalPt = Perspective.screenToLogical(screenCenter, inverse)

            // Add to clusters (manually)
            clusters.getOrPut(currentId) { mutableListOf() }.add(logicalPt)
            capturedHistograms[currentId] = latestCenterHistogram.toList()

            _scanProgress.value += (currentId to true)

            // Persist progress immediately
            val pointsToSave = clusters.mapValues { it.value.first() }
            withContext(Dispatchers.IO) {
                tableScanRepository.savePartialScan(ScanStep.POCKET_GUIDE, pointsToSave)
            }

            // Advance Wizard
            val nextIdx = PocketId.entries.indexOf(currentId) + 1
            if (nextIdx < PocketId.entries.size) {
                _currentPocketTarget.value = PocketId.entries.toTypedArray()[nextIdx]
            } else {
                // Done capturing all 6!
                _currentPocketTarget.value = null
                
                val centerPts = clusters.mapValues { (_, obs) ->
                    PointF(obs.map { it.x }.average().toFloat(), obs.map { it.y }.average().toFloat())
                }
                
                // We don't use TableGeometryFitter here because the user MANUALLY told us which pocket is which.
                // We trust the identities.
                completeScan(centerPts)
            }
        }
    }
}

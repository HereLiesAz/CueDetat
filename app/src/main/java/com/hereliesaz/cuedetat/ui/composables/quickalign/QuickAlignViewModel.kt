// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.TpsWarpData
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.calib3d.Calib3d
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/** Enum defining the stages of the Quick Align wizard. */
enum class QuickAlignStep {
    SELECT_SIZE,
    CAPTURE_PHOTO,
    ALIGN_TABLE,
    FINISHED
}

/** Enum defining the interactive points on the alignment overlay. */
enum class DraggablePocket {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, SIDE_LEFT, SIDE_RIGHT
}

/**
 * ViewModel for the Quick Align feature.
 *
 * Handles the logic for:
 * - State transitions between wizard steps.
 * - Storing and managing the captured table image.
 * - Calculating the Homography matrix (perspective transform) based on user-defined points.
 * - Emitting the final alignment result to the app.
 */
@HiltViewModel
class QuickAlignViewModel @Inject constructor() : ViewModel() {

    // Current wizard step.
    private val _currentStep = MutableStateFlow(QuickAlignStep.SELECT_SIZE)
    val currentStep = _currentStep.asStateFlow()

    // Selected physical table size (e.g., 8ft).
    private val _selectedTableSize = MutableStateFlow<TableSize?>(null)
    val selectedTableSize = _selectedTableSize.asStateFlow()

    // Captured photo of the table.
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    // Positions of the 6 pocket control points in image coordinates.
    private val _pocketPositions = MutableStateFlow<Map<DraggablePocket, Offset>>(emptyMap())
    val pocketPositions = _pocketPositions.asStateFlow()

    // One-shot event to send the result back to the main screen.
    private val _alignResult = MutableSharedFlow<MainScreenEvent.ApplyQuickAlign>()
    val alignResult = _alignResult.asSharedFlow()

    // Ideal (undistorted) pocket positions — set once when photo is captured.
    private var _idealPositions: Map<DraggablePocket, Offset> = emptyMap()

    // Which pockets the user has explicitly placed (released a drag on).
    private val _pinnedPockets = MutableStateFlow<Set<DraggablePocket>>(emptySet())
    val pinnedPockets = _pinnedPockets.asStateFlow()

    // Cancellable job for background TPS prediction during drag.
    private var dragJob: Job? = null

    /**
     * User selects table size. Proceed to capture.
     */
    fun onTableSizeSelected(size: TableSize) {
        _selectedTableSize.value = size
        _currentStep.value = QuickAlignStep.CAPTURE_PHOTO
    }

    /**
     * Photo captured from analyzer. Convert to Bitmap and initialize control points.
     */
    fun onPhotoCaptured(mat: Mat) {
        viewModelScope.launch {
            // Convert Mat to Bitmap for display.
            val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bmp)
            mat.release()
            _capturedBitmap.value = bmp

            // Set initial pocket positions to defaults (inset rectangle).
            initializePocketPositions(IntSize(bmp.width, bmp.height))
            _currentStep.value = QuickAlignStep.ALIGN_TABLE
        }
    }

    private fun initializePocketPositions(size: IntSize) {
        val padding = 0.2f
        val positions = mapOf(
            DraggablePocket.TOP_LEFT     to Offset(size.width * padding,       size.height * padding),
            DraggablePocket.TOP_RIGHT    to Offset(size.width * (1 - padding), size.height * padding),
            DraggablePocket.BOTTOM_RIGHT to Offset(size.width * (1 - padding), size.height * (1 - padding)),
            DraggablePocket.BOTTOM_LEFT  to Offset(size.width * padding,       size.height * (1 - padding)),
            DraggablePocket.SIDE_LEFT    to Offset(size.width * padding,       size.height * 0.5f),
            DraggablePocket.SIDE_RIGHT   to Offset(size.width * (1 - padding), size.height * 0.5f)
        )
        _pocketPositions.value = positions
        _idealPositions = positions
    }

    /**
     * Updates the dragged pocket immediately for responsiveness, then predicts
     * all unpinned pockets on a background thread using TPS. Cancels any
     * in-progress prediction (replace-on-new strategy).
     */
    fun onPocketDrag(pocket: DraggablePocket, newPosition: Offset) {
        // Update dragged pocket immediately.
        val current = _pocketPositions.value.toMutableMap()
        current[pocket] = newPosition
        _pocketPositions.value = current

        dragJob?.cancel()

        val idealPositions = _idealPositions
        val allPockets = DraggablePocket.values().toList()

        // Build constraint set: all pinned pockets + currently dragged pocket.
        val constrained = buildMap<DraggablePocket, Offset> {
            _pinnedPockets.value.forEach { p -> current[p]?.let { put(p, it) } }
            put(pocket, newPosition)  // Dragged overrides pinned if same pocket.
        }

        dragJob = viewModelScope.launch {
            val unpinned = allPockets - constrained.keys
            if (unpinned.isEmpty()) return@launch

            val predicted = withContext(Dispatchers.Default) {
                val srcPts = constrained.entries.map { (p, _) ->
                    android.graphics.PointF(idealPositions[p]!!.x, idealPositions[p]!!.y)
                }
                val dstPts = constrained.entries.map { (_, pos) ->
                    android.graphics.PointF(pos.x, pos.y)
                }
                val imageTps = TpsWarpData(srcPoints = srcPts, dstPoints = dstPts)
                unpinned.associateWith { p ->
                    val ideal = idealPositions[p]!!
                    val result = ThinPlateSpline.applyWarp(imageTps, android.graphics.PointF(ideal.x, ideal.y))
                    Offset(result.x, result.y)
                }
            }

            val updated = _pocketPositions.value.toMutableMap()
            predicted.forEach { (p, pos) -> updated[p] = pos }
            _pocketPositions.value = updated
        }
    }

    /**
     * Pins the pocket at its current position. Called on drag end from the screen.
     */
    fun onPocketReleased(pocket: DraggablePocket) {
        _pinnedPockets.value = _pinnedPockets.value + pocket
    }

    /**
     * Computes homography from all 6 pocket positions, decomposes it to
     * translation/rotation/scale, then computes the residual TPS from the
     * homography-estimated logical positions to the true logical positions.
     *
     * Note on Table.pockets index order: [0]=TL, [1]=TR, [2]=BL, [3]=BR, [4]=SL, [5]=SR
     * The BL/BR indices are swapped from intuitive order — [2] is BL, [3] is BR.
     */
    fun onFinishAlign() {
        val imagePoints = _pocketPositions.value
        val tableSize = _selectedTableSize.value
        val image = _capturedBitmap.value
        if (imagePoints.size != 6 || tableSize == null || image == null) return

        viewModelScope.launch {
            val logicalTable = Table(tableSize, true)

            // Map DraggablePocket → Table.pockets index (note BL=2, BR=3 swap)
            val pocketOrder = listOf(
                DraggablePocket.TOP_LEFT,
                DraggablePocket.TOP_RIGHT,
                DraggablePocket.BOTTOM_RIGHT,
                DraggablePocket.BOTTOM_LEFT,
                DraggablePocket.SIDE_LEFT,
                DraggablePocket.SIDE_RIGHT
            )
            val logicalOrder = listOf(
                logicalTable.pockets[0], // TL
                logicalTable.pockets[1], // TR
                logicalTable.pockets[3], // BR (index 3, not 2!)
                logicalTable.pockets[2], // BL (index 2, not 3!)
                logicalTable.pockets[4], // SL
                logicalTable.pockets[5]  // SR
            )

            val imagePts = pocketOrder.map { imagePoints[it]!! }

            val srcMat = MatOfPoint2f()
            srcMat.fromList(imagePts.map { Point(it.x.toDouble(), it.y.toDouble()) })

            val dstMat = MatOfPoint2f()
            dstMat.fromList(logicalOrder.map { Point(it.x.toDouble(), it.y.toDouble()) })

            val homography = Calib3d.findHomography(srcMat, dstMat, Calib3d.RANSAC, 5.0)

            if (!homography.empty()) {
                val (translation, rotation, scale) = decomposeHomography(
                    homography, image.width.toFloat(), image.height.toFloat()
                )

                // Homography-estimated logical positions: H * image_pts
                val estimatedDst = MatOfPoint2f()
                Core.perspectiveTransform(srcMat, estimatedDst, homography)
                val estimatedLogical = estimatedDst.toList()
                    .map { android.graphics.PointF(it.x.toFloat(), it.y.toFloat()) }

                // True logical positions
                val trueLogical = logicalOrder.map { android.graphics.PointF(it.x, it.y) }

                // Residual TPS: estimated logical → true logical.
                val tpsWarpData = TpsWarpData(srcPoints = estimatedLogical, dstPoints = trueLogical)

                _alignResult.emit(MainScreenEvent.ApplyQuickAlign(translation, rotation, scale, tpsWarpData))
            }
            onResetPoints()
        }
    }

    /**
     * Extracts approximate T, R, S from the homography matrix.
     * Note: This is a simplification; full pose estimation requires PnP, but for 2D alignment
     * this approximation suffices for initial setup.
     */
    private fun decomposeHomography(
        h: Mat,
        imgWidth: Float,
        imgHeight: Float
    ): Triple<Offset, Float, Float> {
        val h0 = h[0, 0][0].toFloat()
        val h1 = h[0, 1][0].toFloat()
        val h2 = h[0, 2][0].toFloat()
        val h3 = h[1, 0][0].toFloat()
        val h4 = h[1, 1][0].toFloat()
        val h5 = h[1, 2][0].toFloat()

        // Estimate scale from basis vectors.
        val scaleX = sqrt(h0 * h0 + h3 * h3)
        val scaleY = sqrt(h1 * h1 + h4 * h4)
        val scale = (scaleX + scaleY) / 2.0f

        // Estimate rotation from the first column vector.
        val rotation = -atan2(h3, h0) * (180f / PI.toFloat())

        // Calculate translation relative to center.
        val canvasCenter = Offset(imgWidth / 2f, imgHeight / 2f)
        val translation = Offset(h2, h5) - canvasCenter

        return Triple(translation, rotation, 1 / scale)
    }

    /**
     * Resets the control points to defaults.
     */
    fun onResetPoints() {
        _pinnedPockets.value = emptySet()
        _selectedTableSize.value?.let {
            _capturedBitmap.value?.let { bmp ->
                initializePocketPositions(IntSize(bmp.width, bmp.height))
            }
        }
    }

    /**
     * Cancels the wizard and clears state.
     */
    fun onCancel() {
        _capturedBitmap.value?.recycle()
        _capturedBitmap.value = null
        _pocketPositions.value = emptyMap()
        _currentStep.value = QuickAlignStep.SELECT_SIZE
    }
}

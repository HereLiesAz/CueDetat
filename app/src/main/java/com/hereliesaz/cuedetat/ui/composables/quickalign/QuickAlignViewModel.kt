// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
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
        val padding = 0.2f // 20% margin from edges.
        _pocketPositions.value = mapOf(
            DraggablePocket.TOP_LEFT to Offset(size.width * padding, size.height * padding),
            DraggablePocket.TOP_RIGHT to Offset(size.width * (1 - padding), size.height * padding),
            DraggablePocket.BOTTOM_RIGHT to Offset(
                size.width * (1 - padding),
                size.height * (1 - padding)
            ),
            DraggablePocket.BOTTOM_LEFT to Offset(
                size.width * padding,
                size.height * (1 - padding)
            ),
            // Side pockets halfway down.
            DraggablePocket.SIDE_LEFT to Offset(size.width * padding, size.height * 0.5f),
            DraggablePocket.SIDE_RIGHT to Offset(size.width * (1 - padding), size.height * 0.5f)
        )
    }

    /**
     * Updates the position of a pocket marker during a drag gesture.
     * Enforces constraints (e.g., side pockets stay on the line between corners).
     */
    fun onPocketDrag(pocket: DraggablePocket, newPosition: Offset) {
        val currentPositions = _pocketPositions.value.toMutableMap()
        when (pocket) {
            DraggablePocket.SIDE_LEFT -> {
                // Constrain left side pocket to the line segment between TL and BL.
                val tl = currentPositions[DraggablePocket.TOP_LEFT]!!
                val bl = currentPositions[DraggablePocket.BOTTOM_LEFT]!!
                currentPositions[pocket] = getClosestPointOnSegment(newPosition, tl, bl)
            }

            DraggablePocket.SIDE_RIGHT -> {
                // Constrain right side pocket to the line segment between TR and BR.
                val tr = currentPositions[DraggablePocket.TOP_RIGHT]!!
                val br = currentPositions[DraggablePocket.BOTTOM_RIGHT]!!
                currentPositions[pocket] = getClosestPointOnSegment(newPosition, tr, br)
            }

            else -> { // Corner pockets
                currentPositions[pocket] = newPosition
                // If a corner moves, re-snap the adjacent side pocket to the new line.
                val tl = currentPositions[DraggablePocket.TOP_LEFT]!!
                val tr = currentPositions[DraggablePocket.TOP_RIGHT]!!
                val bl = currentPositions[DraggablePocket.BOTTOM_LEFT]!!
                val br = currentPositions[DraggablePocket.BOTTOM_RIGHT]!!

                currentPositions[DraggablePocket.SIDE_LEFT] =
                    getClosestPointOnSegment(currentPositions[DraggablePocket.SIDE_LEFT]!!, tl, bl)
                currentPositions[DraggablePocket.SIDE_RIGHT] =
                    getClosestPointOnSegment(currentPositions[DraggablePocket.SIDE_RIGHT]!!, tr, br)
            }
        }
        _pocketPositions.value = currentPositions
    }

    /**
     * Geometrically projects a point onto a line segment.
     */
    private fun getClosestPointOnSegment(point: Offset, start: Offset, end: Offset): Offset {
        val segmentVec = end - start
        val pointVec = point - start
        val segmentLengthSq = segmentVec.getDistanceSquared()
        if (segmentLengthSq == 0f) return start

        // Project point onto line, clamp t to [0, 1].
        val t = (pointVec.x * segmentVec.x + pointVec.y * segmentVec.y) / segmentLengthSq
        val clampedT = t.coerceIn(0f, 1f)
        return start + segmentVec * clampedT
    }


    /**
     * Computes the homography and emits the result.
     */
    fun onFinishAlign() {
        val imagePoints = _pocketPositions.value
        val tableSize = _selectedTableSize.value
        val image = _capturedBitmap.value
        if (imagePoints.size != 6 || tableSize == null || image == null) return

        viewModelScope.launch {
            // Source points: The user-defined corners in the image.
            val srcPointsList = listOf(
                imagePoints[DraggablePocket.TOP_LEFT]!!,
                imagePoints[DraggablePocket.TOP_RIGHT]!!,
                imagePoints[DraggablePocket.BOTTOM_RIGHT]!!,
                imagePoints[DraggablePocket.BOTTOM_LEFT]!!
            )

            val srcPoints = MatOfPoint2f()
            srcPoints.fromList(srcPointsList.map { Point(it.x.toDouble(), it.y.toDouble()) })

            // Destination points: The known logical corners of the selected table size.
            val logicalTable = com.hereliesaz.cuedetat.view.model.Table(tableSize, true)
            val logicalCorners = logicalTable.pockets.slice(0..3)

            val dstPoints = MatOfPoint2f()
            dstPoints.fromList(logicalCorners.map { Point(it.x.toDouble(), it.y.toDouble()) })

            // Calculate Homography: src -> dst.
            val homography = Calib3d.findHomography(srcPoints, dstPoints, Calib3d.RANSAC, 5.0)

            if (!homography.empty()) {
                // Decompose the homography to get simple translation/rotation/scale for the AR engine.
                val (translation, rotation, scale) = decomposeHomography(
                    homography,
                    image.width.toFloat(),
                    image.height.toFloat()
                )
                _alignResult.emit(MainScreenEvent.ApplyQuickAlign(translation, rotation, scale))
            }
            // Reset for next use.
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

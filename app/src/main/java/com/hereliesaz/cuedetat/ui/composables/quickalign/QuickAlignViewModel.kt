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

enum class QuickAlignStep {
    SELECT_SIZE,
    CAPTURE_PHOTO,
    ALIGN_TABLE,
    FINISHED
}

enum class DraggablePocket {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, SIDE_LEFT, SIDE_RIGHT
}

@HiltViewModel
class QuickAlignViewModel @Inject constructor() : ViewModel() {

    private val _currentStep = MutableStateFlow(QuickAlignStep.SELECT_SIZE)
    val currentStep = _currentStep.asStateFlow()

    private val _selectedTableSize = MutableStateFlow<TableSize?>(null)
    val selectedTableSize = _selectedTableSize.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    private val _pocketPositions = MutableStateFlow<Map<DraggablePocket, Offset>>(emptyMap())
    val pocketPositions = _pocketPositions.asStateFlow()

    private val _alignResult = MutableSharedFlow<MainScreenEvent.ApplyQuickAlign>()
    val alignResult = _alignResult.asSharedFlow()

    fun onTableSizeSelected(size: TableSize) {
        _selectedTableSize.value = size
        _currentStep.value = QuickAlignStep.CAPTURE_PHOTO
    }

    fun onPhotoCaptured(mat: Mat) {
        viewModelScope.launch {
            val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bmp)
            mat.release()
            _capturedBitmap.value = bmp
            initializePocketPositions(IntSize(bmp.width, bmp.height))
            _currentStep.value = QuickAlignStep.ALIGN_TABLE
        }
    }

    private fun initializePocketPositions(size: IntSize) {
        val padding = 0.2f
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
            DraggablePocket.SIDE_LEFT to Offset(size.width * padding, size.height * 0.5f),
            DraggablePocket.SIDE_RIGHT to Offset(size.width * (1 - padding), size.height * 0.5f)
        )
    }

    fun onPocketDrag(pocket: DraggablePocket, newPosition: Offset) {
        val currentPositions = _pocketPositions.value.toMutableMap()
        when (pocket) {
            DraggablePocket.SIDE_LEFT -> {
                val tl = currentPositions[DraggablePocket.TOP_LEFT]!!
                val bl = currentPositions[DraggablePocket.BOTTOM_LEFT]!!
                currentPositions[pocket] = getClosestPointOnSegment(newPosition, tl, bl)
            }

            DraggablePocket.SIDE_RIGHT -> {
                val tr = currentPositions[DraggablePocket.TOP_RIGHT]!!
                val br = currentPositions[DraggablePocket.BOTTOM_RIGHT]!!
                currentPositions[pocket] = getClosestPointOnSegment(newPosition, tr, br)
            }

            else -> { // Corner pockets
                currentPositions[pocket] = newPosition
                // Update side pockets to stay on the new rail
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

    private fun getClosestPointOnSegment(point: Offset, start: Offset, end: Offset): Offset {
        val segmentVec = end - start
        val pointVec = point - start
        val segmentLengthSq = segmentVec.getDistanceSquared()
        if (segmentLengthSq == 0f) return start

        val t = (pointVec.x * segmentVec.x + pointVec.y * segmentVec.y) / segmentLengthSq
        val clampedT = t.coerceIn(0f, 1f)
        return start + segmentVec * clampedT
    }


    fun onFinishAlign() {
        val imagePoints = _pocketPositions.value
        val tableSize = _selectedTableSize.value
        val image = _capturedBitmap.value
        if (imagePoints.size != 6 || tableSize == null || image == null) return

        viewModelScope.launch {
            val srcPointsList = listOf(
                imagePoints[DraggablePocket.TOP_LEFT]!!,
                imagePoints[DraggablePocket.TOP_RIGHT]!!,
                imagePoints[DraggablePocket.BOTTOM_RIGHT]!!,
                imagePoints[DraggablePocket.BOTTOM_LEFT]!!
            )

            val srcPoints = MatOfPoint2f()
            srcPoints.fromList(srcPointsList.map { Point(it.x.toDouble(), it.y.toDouble()) })

            val logicalTable = com.hereliesaz.cuedetat.view.model.Table(tableSize, true)
            val logicalCorners = logicalTable.pockets.slice(0..3)

            val dstPoints = MatOfPoint2f()
            dstPoints.fromList(logicalCorners.map { Point(it.x.toDouble(), it.y.toDouble()) })

            val homography = Calib3d.findHomography(srcPoints, dstPoints, Calib3d.RANSAC, 5.0)

            if (!homography.empty()) {
                val (translation, rotation, scale) = decomposeHomography(
                    homography,
                    image.width.toFloat(),
                    image.height.toFloat()
                )
                _alignResult.emit(MainScreenEvent.ApplyQuickAlign(translation, rotation, scale))
            }
            onResetPoints()
        }
    }

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

        val scaleX = sqrt(h0 * h0 + h3 * h3)
        val scaleY = sqrt(h1 * h1 + h4 * h4)
        val scale = (scaleX + scaleY) / 2.0f

        val rotation = -atan2(h3, h0) * (180f / PI.toFloat())

        val canvasCenter = Offset(imgWidth / 2f, imgHeight / 2f)
        val translation = Offset(h2, h5) - canvasCenter

        return Triple(translation, rotation, 1 / scale)
    }

    fun onResetPoints() {
        _selectedTableSize.value?.let {
            _capturedBitmap.value?.let { bmp ->
                initializePocketPositions(IntSize(bmp.width, bmp.height))
            }
        }
    }

    fun onCancel() {
        _capturedBitmap.value?.recycle()
        _capturedBitmap.value = null
        _pocketPositions.value = emptyMap()
        _currentStep.value = QuickAlignStep.SELECT_SIZE
    }
}
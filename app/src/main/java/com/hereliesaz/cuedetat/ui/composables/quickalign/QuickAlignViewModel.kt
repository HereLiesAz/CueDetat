// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickAlignResult(val translation: Offset, val rotation: Float, val scale: Float)

enum class QuickAlignStep {
    SELECT_SIZE,
    ALIGN_TABLE
}

data class AlignState(
    val pan: Offset = Offset.Zero,
    val zoom: Float = 1.0f,
    val rotation: Float = 0f
)

@HiltViewModel
class QuickAlignViewModel @Inject constructor() : ViewModel() {

    private val _currentStep = MutableStateFlow(QuickAlignStep.SELECT_SIZE)
    val currentStep = _currentStep.asStateFlow()

    private val _selectedTableSize = MutableStateFlow<TableSize?>(null)
    val selectedTableSize = _selectedTableSize.asStateFlow()

    private val _alignState = MutableStateFlow(AlignState())
    val alignState = _alignState.asStateFlow()

    private val _alignResult = MutableSharedFlow<QuickAlignResult>()
    val alignResult = _alignResult.asSharedFlow()

    fun onTableSizeSelected(size: TableSize) {
        _selectedTableSize.value = size
        _currentStep.value = QuickAlignStep.ALIGN_TABLE
    }

    fun onPan(delta: Offset) {
        _alignState.value = _alignState.value.copy(pan = _alignState.value.pan + delta)
    }

    fun onZoom(zoomFactor: Float) {
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            ExperienceMode.EXPERT,
            false
        ) // Quick Align uses expert mode zoom
        val newZoom = (_alignState.value.zoom * zoomFactor).coerceIn(
            minZoom / 2f,
            maxZoom * 2f
        )
        _alignState.value = _alignState.value.copy(zoom = newZoom)
    }

    fun onRotate(rotationDelta: Float) {
        _alignState.value =
            _alignState.value.copy(rotation = _alignState.value.rotation + rotationDelta)
    }

    fun onResetView() {
        _alignState.value = AlignState()
    }

    fun onFinishAlign(screenWidth: Int, screenHeight: Int) {
        val finalAlignState = _alignState.value
        val finalZoom = ZoomMapping.DEFAULT_ZOOM / finalAlignState.zoom

        val finalTranslation = finalAlignState.pan

        viewModelScope.launch {
            _alignResult.emit(
                QuickAlignResult(
                    translation = finalTranslation,
                    rotation = finalAlignState.rotation,
                    scale = finalZoom
                )
            )
        }
        onResetPoints()
    }

    fun onResetPoints() {
        _selectedTableSize.value = null
        _currentStep.value = QuickAlignStep.SELECT_SIZE
        _alignState.value = AlignState()
    }

    fun onCancel() {
        onResetPoints()
    }
}
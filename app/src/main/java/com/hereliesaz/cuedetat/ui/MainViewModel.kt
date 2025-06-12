package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.*

private object ZoomMapping {
    const val MIN_ZOOM = 0.2f
    const val MAX_ZOOM = 0.4f
    const val DEFAULT_ZOOM = 0.4f
    private const val B = 1.0069555f
    fun sliderToZoom(sliderValue: Float): Float = MIN_ZOOM * B.pow(sliderValue)
    fun zoomToSlider(zoomFactor: Float): Float =
        if (zoomFactor <= MIN_ZOOM) 0f else (ln(zoomFactor / MIN_ZOOM) / ln(B))
}

sealed class ToastMessage {
    data class StringResource(val id: Int, val formatArgs: List<Any> = emptyList()) : ToastMessage()
    data class PlainText(val text: String) : ToastMessage()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    application: Application
) : ViewModel() {

    private val graphicsCamera = Camera()
    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)

    init {
        sensorRepository.pitchAngleFlow.onEach(::onPitchAngleChanged).launchIn(viewModelScope)
    }

    private fun createInitialState(
        viewWidth: Int = 0,
        viewHeight: Int = 0,
        scheme: ColorScheme? = null
    ): OverlayState {
        val radius = (min(viewWidth, viewHeight) * 0.30f / 2f) * ZoomMapping.DEFAULT_ZOOM
        val state = OverlayState(
            protractorUnit = ProtractorUnit(
                center = PointF(viewWidth / 2f, viewHeight / 2f),
                radius = radius,
                rotationDegrees = 0f
            ),
            actualCueBall = null,
            dynamicColorScheme = scheme ?: _uiState.value.dynamicColorScheme,
            zoomSliderPosition = 100f
        )
        return state.recalculateDerivedState(graphicsCamera, 0f, viewWidth, viewHeight)
    }

    private fun updateState(updateLogic: (currentState: OverlayState) -> OverlayState) {
        val oldState = _uiState.value
        _uiState.update(updateLogic)

        val currentState = _uiState.value
        _uiState.update {
            it.recalculateDerivedState(
                graphicsCamera,
                currentState.protractorUnit.rotationDegrees,
                currentState.protractorUnit.center.x.toInt() * 2,
                currentState.protractorUnit.center.y.toInt() * 2
            )
        }

        val newState = _uiState.value
        if (newState.isImpossibleShot && !oldState.isImpossibleShot) {
            _toastMessage.value = ToastMessage.PlainText(insultingWarnings.random())
        }
    }

    fun onSizeChanged(width: Int, height: Int) {
        if (_uiState.value.protractorUnit.center.x == 0f) {
            updateState { createInitialState(width, height) }
        }
    }

    fun onZoomSliderChange(sliderPosition: Float) {
        val newZoom = ZoomMapping.sliderToZoom(sliderPosition)
        updateState { currentState ->
            val viewWidth = currentState.protractorUnit.center.x.toInt() * 2
            val viewHeight = currentState.protractorUnit.center.y.toInt() * 2
            val newRadius = (min(viewWidth, viewHeight) * 0.30f / 2f) * newZoom
            currentState.copy(
                protractorUnit = currentState.protractorUnit.copy(radius = newRadius),
                zoomSliderPosition = sliderPosition,
                valuesChangedSinceReset = true
            )
        }
    }

    fun onRotationChange(newRotation: Float) {
        var normAng = newRotation % 360f
        if (normAng < 0) normAng += 360f
        updateState {
            it.copy(
                protractorUnit = it.protractorUnit.copy(rotationDegrees = normAng),
                valuesChangedSinceReset = true
            )
        }
    }

    private fun onPitchAngleChanged(pitch: Float) {
        updateState {
            it.recalculateDerivedState(
                graphicsCamera,
                pitch,
                it.protractorUnit.center.x.toInt() * 2,
                it.protractorUnit.center.y.toInt() * 2
            )
        }
    }

    fun onUnitMoved(newPosition: PointF) {
        updateState {
            it.copy(
                protractorUnit = it.protractorUnit.copy(center = newPosition),
                valuesChangedSinceReset = true
            )
        }
    }

    fun onActualCueBallMoved(screenPosition: PointF) {
        val logicalPos =
            Perspective.screenToLogical(screenPosition, _uiState.value.inversePitchMatrix)
        updateState {
            it.copy(
                actualCueBall = it.actualCueBall?.copy(center = logicalPos)
                    ?: ActualCueBall(center = logicalPos, radius = it.protractorUnit.radius),
                valuesChangedSinceReset = true
            )
        }
    }

    fun onToggleActualCueBall() {
        updateState {
            if (it.actualCueBall == null) {
                val defaultLogicalPos =
                    PointF(it.protractorUnit.center.x, it.protractorUnit.center.y + 200f)
                it.copy(
                    actualCueBall = ActualCueBall(
                        center = defaultLogicalPos,
                        radius = it.protractorUnit.radius
                    )
                )
            } else {
                it.copy(actualCueBall = null)
            }
        }
    }

    fun onReset() {
        updateState {
            createInitialState(
                it.protractorUnit.center.x.toInt() * 2,
                it.protractorUnit.center.y.toInt() * 2,
                it.dynamicColorScheme
            )
        }
    }

    fun onThemeChanged(scheme: ColorScheme) {
        _uiState.update { it.copy(dynamicColorScheme = scheme) }
    }

    fun onToggleHelp() {
        _uiState.update { it.copy(areHelpersVisible = !it.areHelpersVisible) }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }
}

private fun OverlayState.recalculateDerivedState(
    camera: Camera,
    pitch: Float,
    viewWidth: Int,
    viewHeight: Int
): OverlayState {
    if (viewWidth == 0 || viewHeight == 0) return this

    val pitchMatrix = Perspective.createPitchMatrix(pitch, viewWidth, viewHeight, camera)
    val inverseMatrix = Matrix()
    val hasInverse = pitchMatrix.invert(inverseMatrix)

    val logicalDistance = distance(protractorUnit.protractorCueBallCenter, protractorUnit.center)
    val isPhysicalOverlap = logicalDistance < (protractorUnit.radius * 2) - 0.1f
    val isDeflectionDominantAngle =
        (protractorUnit.rotationDegrees > 90.5f && protractorUnit.rotationDegrees < 269.5f)
    var isTargetObstructing = false
    this.actualCueBall?.let {
        val distActualToGhost = distance(it.center, protractorUnit.protractorCueBallCenter)
        val distActualToTarget = distance(it.center, protractorUnit.center)
        isTargetObstructing = distActualToGhost > distActualToTarget
    }
    val isImpossible = isPhysicalOverlap || isDeflectionDominantAngle || isTargetObstructing

    return this.copy(
        pitchMatrix = pitchMatrix,
        inversePitchMatrix = inverseMatrix,
        hasInverseMatrix = hasInverse,
        isImpossibleShot = isImpossible
    )
}

private fun distance(p1: PointF, p2: PointF): Float =
    sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

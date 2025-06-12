package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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
    val toastMessage = _toastMessage.asStateFlow()


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
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = PointF(viewWidth / 2f, viewHeight / 2f),
                radius = radius,
                rotationDegrees = 0f
            ),
            actualCueBall = null,
            dynamicColorScheme = scheme ?: darkColorScheme(),
            zoomSliderPosition = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
        )
        return state.recalculateDerivedState(graphicsCamera)
    }

    private fun updateState(updateLogic: (currentState: OverlayState) -> OverlayState) {
        val oldState = _uiState.value
        val updatedState = updateLogic(oldState)
        val finalState = updatedState.recalculateDerivedState(graphicsCamera)

        _uiState.value = finalState

        if (finalState.isImpossibleShot && !oldState.isImpossibleShot) {
            _toastMessage.value = ToastMessage.PlainText(insultingWarnings.random())
        }
    }

    fun onSizeChanged(width: Int, height: Int) {
        if (_uiState.value.viewWidth == 0) {
            updateState { createInitialState(width, height, it.dynamicColorScheme) }
        }
    }

    fun onZoomSliderChange(sliderPosition: Float) {
        val newZoom = ZoomMapping.sliderToZoom(sliderPosition)
        updateState { currentState ->
            val viewWidth = currentState.viewWidth
            val viewHeight = currentState.viewHeight
            val newRadius = (min(viewWidth, viewHeight) * 0.30f / 2f) * newZoom
            currentState.copy(
                protractorUnit = currentState.protractorUnit.copy(radius = newRadius),
                actualCueBall = currentState.actualCueBall?.copy(radius = newRadius),
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
            it.copy(pitchAngle = pitch)
        }
    }

    fun onUnitMoved(screenPosition: PointF) {
        updateState {
            if (it.hasInverseMatrix) {
                val logicalPos = Perspective.screenToLogical(screenPosition, it.inversePitchMatrix)
                it.copy(
                    protractorUnit = it.protractorUnit.copy(center = logicalPos),
                    valuesChangedSinceReset = true
                )
            } else {
                it
            }
        }
    }

    fun onActualCueBallMoved(logicalPosition: PointF) {
        updateState {
            it.copy(
                actualCueBall = it.actualCueBall?.copy(center = logicalPosition)
                    ?: ActualCueBall(center = logicalPosition, radius = it.protractorUnit.radius),
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
                it.viewWidth,
                it.viewHeight,
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
    camera: Camera
): OverlayState {
    if (viewWidth == 0 || viewHeight == 0) return this

    val pitchMatrix =
        Perspective.createPitchMatrix(this.pitchAngle, this.viewWidth, this.viewHeight, camera)
    val inverseMatrix = Matrix()
    val hasInverse = pitchMatrix.invert(inverseMatrix)

    val logicalDistance =
        distance(this.protractorUnit.protractorCueBallCenter, this.protractorUnit.center)
    val isPhysicalOverlap = logicalDistance < (this.protractorUnit.radius * 2) - 0.1f
    val isDeflectionDominantAngle =
        (this.protractorUnit.rotationDegrees > 90.5f && this.protractorUnit.rotationDegrees < 269.5f)

    // BUGFIX: This was the source of the incorrect warning. The old logic was removed, but a new,
    // more correct version is needed. This only triggers if the Actual Cue Ball is active.
    var isShotThroughTarget = false
    if (this.actualCueBall != null) {
        val actual = this.actualCueBall.center
        val protractorCue = this.protractorUnit.protractorCueBallCenter
        val target = this.protractorUnit.center

        // Check if the target is roughly between the actual ball and the ghost cue ball.
        val dotProduct =
            (target.x - actual.x) * (protractorCue.x - actual.x) + (target.y - actual.y) * (protractorCue.y - actual.y)
        val squaredLength =
            (protractorCue.x - actual.x).pow(2) + (protractorCue.y - actual.y).pow(2)

        // If the dot product is between 0 and the squared length, the projection of the target lies on the segment.
        if (dotProduct > 0 && dotProduct < squaredLength) {
            // Check how far the target center is from the line segment.
            val dist =
                abs((protractorCue.x - actual.x) * (actual.y - target.y) - (actual.x - target.x) * (protractorCue.y - actual.y)) /
                        distance(actual, protractorCue)
            if (dist < this.protractorUnit.radius) {
                isShotThroughTarget = true
            }
        }
    }


    val isImpossible = isPhysicalOverlap || isDeflectionDominantAngle || isShotThroughTarget

    return this.copy(
        pitchMatrix = pitchMatrix,
        inversePitchMatrix = inverseMatrix,
        hasInverseMatrix = hasInverse,
        isImpossibleShot = isImpossible
    )
}

private fun distance(p1: PointF, p2: PointF): Float =
    sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

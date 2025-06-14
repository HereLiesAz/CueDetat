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
import com.hereliesaz.cuedetat.data.UpdateChecker
import com.hereliesaz.cuedetat.data.UpdateResult
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

sealed class ToastMessage {
    data class StringResource(val id: Int, val formatArgs: List<Any> = emptyList()) : ToastMessage()
    data class PlainText(val text: String) : ToastMessage()
}

sealed class SingleEvent {
    data class OpenUrl(val url: String) : SingleEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val updateChecker: UpdateChecker,
    private val application: Application
) : ViewModel() {

    private val graphicsCamera = Camera()
    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _singleEvent = MutableStateFlow<SingleEvent?>(null)
    val singleEvent = _singleEvent.asStateFlow()


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
            areHelpersVisible = false,
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
                if (!it.hasInverseMatrix) return@updateState it // Guard clause

                val ghostCueBallPos = it.protractorUnit.protractorCueBallCenter

                val screenBottomCenter = floatArrayOf(it.viewWidth / 2f, it.viewHeight.toFloat())
                val logicalBottomCenterArray = FloatArray(2)
                it.inversePitchMatrix.mapPoints(logicalBottomCenterArray, screenBottomCenter)
                val logicalBottomPos =
                    PointF(logicalBottomCenterArray[0], logicalBottomCenterArray[1])

                val newDefaultX = (ghostCueBallPos.x + logicalBottomPos.x) / 2f
                val newDefaultY = (ghostCueBallPos.y + logicalBottomPos.y) / 2f
                val newDefaultPos = PointF(newDefaultX, newDefaultY)

                it.copy(
                    actualCueBall = ActualCueBall(
                        center = newDefaultPos,
                        radius = it.protractorUnit.radius
                    )
                )
            } else {
                it.copy(actualCueBall = null)
            }
        }
    }

    fun onReset() {
        updateState { currentState ->
            val newRadius = (min(
                currentState.viewWidth,
                currentState.viewHeight
            ) * 0.30f / 2f) * ZoomMapping.DEFAULT_ZOOM

            val updatedActualCueBall = currentState.actualCueBall?.copy(radius = newRadius)

            currentState.copy(
                protractorUnit = ProtractorUnit(
                    center = PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f),
                    radius = newRadius,
                    rotationDegrees = 0f
                ),
                actualCueBall = updatedActualCueBall,
                zoomSliderPosition = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM),
                valuesChangedSinceReset = false,
                pitchAngle = 0.0f
            )
        }
    }

    fun onThemeChanged(scheme: ColorScheme) {
        _uiState.update { it.copy(dynamicColorScheme = scheme) }
    }

    fun onToggleHelp() {
        _uiState.update { it.copy(areHelpersVisible = !it.areHelpersVisible) }
    }

    fun onCheckForUpdate() {
        viewModelScope.launch {
            val result = updateChecker.checkForUpdate()
            val message: ToastMessage = when (result) {
                is UpdateResult.UpdateAvailable -> ToastMessage.StringResource(
                    R.string.update_available,
                    listOf(result.latestVersion)
                )

                is UpdateResult.UpToDate -> ToastMessage.StringResource(R.string.update_no_new_release)
                is UpdateResult.CheckFailed -> ToastMessage.PlainText(result.reason)
            }
            _toastMessage.value = message
        }
    }

    fun onViewArt() {
        _singleEvent.value = SingleEvent.OpenUrl("https://instagram.com/hereliesaz")
    }

    fun onSingleEventConsumed() {
        _singleEvent.value = null
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

    val anchorPointA: PointF? = if (this.actualCueBall != null) {
        this.actualCueBall.center
    } else {
        if (hasInverse) {
            val screenAnchor = floatArrayOf(viewWidth / 2f, viewHeight.toFloat())
            val logicalAnchorArray = FloatArray(2)
            inverseMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        } else {
            null
        }
    }

    val isImpossible = anchorPointA?.let { anchor ->
        val distAtoG = distance(anchor, this.protractorUnit.protractorCueBallCenter)
        val distAtoT = distance(anchor, this.protractorUnit.center)
        distAtoG > distAtoT
    } ?: false

    return this.copy(
        pitchMatrix = pitchMatrix,
        inversePitchMatrix = inverseMatrix,
        hasInverseMatrix = hasInverse,
        isImpossibleShot = isImpossible
    )
}

private fun distance(p1: PointF, p2: PointF): Float =
    sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
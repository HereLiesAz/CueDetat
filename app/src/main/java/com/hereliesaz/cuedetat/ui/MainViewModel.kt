package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.view.state.OverlayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private object ZoomMapping {
    const val MIN_ZOOM = 0.2f
    const val MAX_ZOOM = 4.0f
    const val DEFAULT_ZOOM = 0.4f
    private const val B = 1.0304433f
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
    private val githubRepository: GithubRepository,
    application: Application
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(OverlayState(zoomSliderPosition = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)))
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)
    private val graphicsCamera = Camera()

    init {
        sensorRepository.pitchAngleFlow.onEach(::onPitchAngleChanged).launchIn(viewModelScope)
    }

    private fun updateState(updateAndRecalculate: (OverlayState) -> OverlayState) {
        val oldState = _uiState.value
        _uiState.update {
            updateAndRecalculate(it).recalculateDerivedState(graphicsCamera)
        }
        val newState = _uiState.value
        if (newState.isImpossibleShot && !oldState.isImpossibleShot) {
            _toastMessage.value = ToastMessage.PlainText(insultingWarnings.random())
        }
    }

    fun onSizeChanged(width: Int, height: Int) {
        if (_uiState.value.unitCenterPosition.x == 0f) {
            updateState {
                it.copy(
                    viewWidth = width,
                    viewHeight = height,
                    unitCenterPosition = PointF(width / 2f, height / 2f),
                    logicalActualCueBallPosition = PointF(width / 2f, height * 1.2f)
                )
            }
        } else {
            updateState { it.copy(viewWidth = width, viewHeight = height) }
        }
    }

    fun onZoomSliderChange(sliderPosition: Float) {
        val newZoom = ZoomMapping.sliderToZoom(sliderPosition)
        updateState {
            it.copy(
                zoomFactor = newZoom.coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM),
                zoomSliderPosition = sliderPosition,
                valuesChangedSinceReset = true
            )
        }
    }

    fun onRotationChange(newRotation: Float) {
        var normAng = newRotation % 360f
        if (normAng < 0) normAng += 360f
        updateState { it.copy(rotationAngle = normAng, valuesChangedSinceReset = true) }
    }

    private fun onPitchAngleChanged(pitch: Float) {
        updateState { it.copy(pitchAngle = pitch) }
    }

    fun onThemeChanged(scheme: ColorScheme) {
        _uiState.update { it.copy(dynamicColorScheme = scheme) }
    }

    fun onUnitMoved(newPosition: PointF) {
        updateState { it.copy(unitCenterPosition = newPosition, valuesChangedSinceReset = true) }
    }

    fun onActualCueBallMoved(logicalPosition: PointF) {
        updateState {
            it.copy(
                logicalActualCueBallPosition = logicalPosition,
                valuesChangedSinceReset = true
            )
        }
    }

    fun onToggleActualCueBall() {
        _uiState.update { it.copy(isActualCueBallVisible = !it.isActualCueBallVisible) }
    }

    fun onReset() {
        updateState {
            OverlayState(
                viewWidth = it.viewWidth,
                viewHeight = it.viewHeight,
                unitCenterPosition = PointF(it.viewWidth / 2f, it.viewHeight / 2f),
                logicalActualCueBallPosition = PointF(it.viewWidth / 2f, it.viewHeight * 1.2f),
                valuesChangedSinceReset = false,
                dynamicColorScheme = it.dynamicColorScheme,
                zoomFactor = ZoomMapping.DEFAULT_ZOOM,
                zoomSliderPosition = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
            )
        }
    }

    fun onToggleHelp() {
        _uiState.update { it.copy(areHelpersVisible = !it.areHelpersVisible) }
    }

    fun onCheckForUpdate() {
        viewModelScope.launch {
            val latestVersion = githubRepository.getLatestVersion()
            val currentVersion = BuildConfig.VERSION_NAME

            val message = when {
                latestVersion == null -> ToastMessage.StringResource(R.string.update_check_failed)
                latestVersion == currentVersion -> ToastMessage.StringResource(R.string.update_no_new_release)
                else -> ToastMessage.StringResource(
                    R.string.update_available,
                    listOf(latestVersion)
                )
            }
            _toastMessage.value = message
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }
}

private fun distance(p1: PointF, p2: PointF): Float =
    sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

private fun OverlayState.recalculateDerivedState(camera: Camera): OverlayState {
    if (this.viewWidth == 0 || this.viewHeight == 0) return this

    val baseDiameter = min(viewWidth, viewHeight) * 0.30f
    val newLogicalRadius = (baseDiameter / 2f) * this.zoomFactor
    val newTargetCenter = this.unitCenterPosition
    val angleRad = Math.toRadians(this.rotationAngle.toDouble())
    val distance = 2 * newLogicalRadius
    val newCueCenter = PointF(
        newTargetCenter.x - (distance * sin(angleRad)).toFloat(),
        newTargetCenter.y + (distance * cos(angleRad)).toFloat()
    )

    val pitchMatrix = Matrix()
    camera.save()
    camera.setLocation(0f, 0f, -32f)
    camera.rotateX(this.pitchAngle)
    camera.getMatrix(pitchMatrix)
    camera.restore()

    val pivotX = this.viewWidth / 2f
    val pivotY = this.viewHeight / 2f
    pitchMatrix.preTranslate(-pivotX, -pivotY)
    pitchMatrix.postTranslate(pivotX, pivotY)

    val inverseMatrix = Matrix()
    val hasInverse = pitchMatrix.invert(inverseMatrix)

    val logicalDistance = distance(newCueCenter, newTargetCenter)
    val isPhysicalOverlap = logicalDistance < (newLogicalRadius * 2) - 0.1f
    val isDeflectionDominantAngle = (this.rotationAngle > 90.5f && this.rotationAngle < 269.5f)

    var isTargetObstructing = false
    if (this.isActualCueBallVisible) {
        val distActualToGhost = distance(this.logicalActualCueBallPosition, newCueCenter)
        val distActualToTarget = distance(this.logicalActualCueBallPosition, newTargetCenter)
        isTargetObstructing = distActualToGhost > distActualToTarget
    }

    val isImpossible = isPhysicalOverlap || isDeflectionDominantAngle || isTargetObstructing

    return this.copy(
        targetCircleCenter = newTargetCenter,
        cueCircleCenter = newCueCenter,
        logicalRadius = newLogicalRadius,
        pitchMatrix = pitchMatrix,
        inversePitchMatrix = inverseMatrix,
        hasInverseMatrix = hasInverse,
        isImpossibleShot = isImpossible
    )
}

package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
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
import kotlin.math.min
import kotlin.math.sin

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

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)
    private val graphicsCamera = Camera()

    init {
        sensorRepository.pitchAngleFlow
            .onEach(::onPitchAngleChanged)
            .launchIn(viewModelScope)
    }

    fun onSizeChanged(width: Int, height: Int) {
        val baseDiameter = min(width, height) * 0.30f
        _uiState.update {
            it.copy(viewWidth = width, viewHeight = height)
                .recalculateDerivedState(baseDiameter, graphicsCamera)
        }
    }

    fun onZoomChange(newZoom: Float) {
        _uiState.update {
            it.copy(zoomFactor = newZoom, valuesChangedSinceReset = true)
                .recalculateDerivedState(camera = graphicsCamera)
        }
    }

    fun onRotationChange(newRotation: Float) {
        var normAng = newRotation % 360f
        if (normAng < 0) normAng += 360f
        _uiState.update {
            it.copy(rotationAngle = normAng, valuesChangedSinceReset = true)
                .recalculateDerivedState(camera = graphicsCamera)
        }
    }

    private fun onPitchAngleChanged(pitch: Float) {
        _uiState.update {
            it.copy(pitchAngle = pitch).recalculateDerivedState(camera = graphicsCamera)
        }
    }

    fun onReset() {
        _uiState.update {
            OverlayState(viewWidth = it.viewWidth, viewHeight = it.viewHeight)
                .recalculateDerivedState(camera = graphicsCamera)
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

private fun OverlayState.recalculateDerivedState(
    baseDiameter: Float? = null,
    camera: Camera
): OverlayState {
    val newBaseDiameter = baseDiameter ?: (min(viewWidth, viewHeight) * 0.30f)
    val newLogicalRadius = (newBaseDiameter / 2f) * this.zoomFactor

    val angleRad = Math.toRadians(this.rotationAngle.toDouble())
    val distance = 2 * newLogicalRadius
    val newTargetCenter = PointF(viewWidth / 2f, viewHeight / 2f)
    val newCueCenter = PointF(
        newTargetCenter.x - (distance * sin(angleRad)).toFloat(),
        newTargetCenter.y + (distance * cos(angleRad)).toFloat()
    )

    val pitchMatrix = Matrix()
    camera.save()
    camera.rotateX(this.pitchAngle)
    camera.getMatrix(pitchMatrix)
    camera.restore()
    pitchMatrix.preTranslate(-newTargetCenter.x, -newTargetCenter.y)
    pitchMatrix.postTranslate(newTargetCenter.x, newTargetCenter.y)

    val inversePitchMatrix = Matrix()
    val hasInverse = pitchMatrix.invert(inversePitchMatrix)

    return this.copy(
        targetCircleCenter = newTargetCenter,
        cueCircleCenter = newCueCenter,
        logicalRadius = newLogicalRadius,
        pitchMatrix = pitchMatrix,
        inversePitchMatrix = inversePitchMatrix,
        hasInverseMatrix = hasInverse
    )
}

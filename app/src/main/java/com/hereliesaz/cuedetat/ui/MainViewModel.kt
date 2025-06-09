package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
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
import kotlin.math.sqrt

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

    private val _dynamicColorScheme = MutableStateFlow<ColorScheme?>(null)
    val dynamicColorScheme = _dynamicColorScheme.asStateFlow()

    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)
    private val graphicsCamera = Camera()

    private var previousZoom: Float? = null
    private var previousRotation: Float? = null

    init {
        sensorRepository.pitchAngleFlow
            .onEach(::onPitchAngleChanged)
            .launchIn(viewModelScope)
    }

    private fun updateState(updateAndRecalculate: (OverlayState) -> OverlayState) {
        val oldState = _uiState.value
        _uiState.update {
            updateAndRecalculate(it).recalculateDerivedState(camera = graphicsCamera)
        }
        val newState = _uiState.value

        if (newState.isImpossibleShot && !oldState.isImpossibleShot) {
            _toastMessage.value = ToastMessage.PlainText(insultingWarnings.random())
        }
    }

    fun onSizeChanged(width: Int, height: Int) {
        val baseDiameter = min(width, height) * 0.30f
        updateState {
            it.copy(
                viewWidth = width,
                viewHeight = height,
                logicalRadius = baseDiameter / 2f
            )
        }
    }

    fun onZoomChange(newZoom: Float) {
        updateState { it.copy(zoomFactor = newZoom, valuesChangedSinceReset = true) }
    }

    fun onRotationChange(newRotation: Float) {
        var normAng = newRotation % 360f
        if (normAng < 0) normAng += 360f
        updateState { it.copy(rotationAngle = normAng, valuesChangedSinceReset = true) }
    }

    private fun onPitchAngleChanged(pitch: Float) {
        updateState { it.copy(pitchAngle = pitch) }
    }

    fun onReset() {
        val currentState = _uiState.value

        if (currentState.valuesChangedSinceReset) {
            previousZoom = currentState.zoomFactor
            previousRotation = currentState.rotationAngle
            updateState {
                OverlayState(
                    viewWidth = it.viewWidth,
                    viewHeight = it.viewHeight,
                    valuesChangedSinceReset = false
                )
            }
        } else {
            if (previousZoom != null && previousRotation != null) {
                updateState {
                    it.copy(
                        zoomFactor = previousZoom!!,
                        rotationAngle = previousRotation!!,
                        valuesChangedSinceReset = true
                    )
                }
                previousZoom = null
                previousRotation = null
            }
        }
    }

    fun onToggleHelp() {
        _uiState.update { it.copy(areHelpersVisible = !it.areHelpersVisible) }
    }

    // NEW: Function to toggle the jumping ghost ball feature
    fun onToggleJumpingGhostBall() {
        updateState { it.copy(isJumpingGhostBallActive = !it.isJumpingGhostBallActive) }
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

    fun adaptThemeFromBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return

        viewModelScope.launch {
            val palette = Palette.from(bitmap).generate()
            _dynamicColorScheme.value = createSchemeFromPalette(palette)
        }
    }

    fun resetTheme() {
        _dynamicColorScheme.value = null
    }

    private fun createSchemeFromPalette(palette: Palette): ColorScheme {
        val primaryColor = Color(palette.getVibrantColor(palette.getMutedColor(0xFF00E5FF.toInt())))
        val secondaryColor =
            Color(palette.getLightVibrantColor(palette.getDominantColor(0xFF4DD0E1.toInt())))
        val isDark = ColorUtils.calculateLuminance(primaryColor.toArgb()) < 0.5

        return if (isDark) {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = getOnColorFor(primaryColor),
                primaryContainer = primaryColor.copy(alpha = 0.3f),
                secondary = secondaryColor,
                onSecondary = getOnColorFor(secondaryColor),
                tertiary = Color(palette.getDarkMutedColor(0xFFF50057.toInt())),
                background = Color(palette.getDarkMutedColor(0xFF1A1C1C.toInt())),
                surface = Color(palette.getMutedColor(0xFF1A1C1C.toInt())),
                onBackground = getOnColorFor(Color(palette.getDarkMutedColor(0xFF1A1C1C.toInt()))),
                onSurface = getOnColorFor(Color(palette.getMutedColor(0xFF1A1C1C.toInt())))
            )
        } else {
            lightColorScheme(
                primary = primaryColor,
                onPrimary = getOnColorFor(primaryColor),
                primaryContainer = primaryColor.copy(alpha = 0.3f),
                secondary = secondaryColor,
                onSecondary = getOnColorFor(secondaryColor),
                tertiary = Color(palette.getLightMutedColor(0xFFD81B60.toInt())),
                background = Color(palette.getLightMutedColor(0xFFFAFDFD.toInt())),
                surface = Color(palette.getDominantColor(0xFFFAFDFD.toInt())),
                onBackground = getOnColorFor(Color(palette.getLightMutedColor(0xFFFAFDFD.toInt()))),
                onSurface = getOnColorFor(Color(palette.getDominantColor(0xFFFAFDFD.toInt())))
            )
        }
    }

    private fun getOnColorFor(color: Color): Color {
        return if (ColorUtils.calculateLuminance(color.toArgb()) > 0.5) Color.Black else Color.White
    }
}

private fun OverlayState.recalculateDerivedState(camera: Camera): OverlayState {
    if (this.viewWidth == 0 || this.viewHeight == 0) return this

    val baseDiameter = min(viewWidth, viewHeight) * 0.30f
    val newLogicalRadius = (baseDiameter / 2f) * this.zoomFactor

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

    val logicalDistance = sqrt(
        (newCueCenter.x - newTargetCenter.x).pow(2) + (newCueCenter.y - newTargetCenter.y).pow(2)
    )
    val isPhysicalOverlap = logicalDistance < (newLogicalRadius * 2) - 0.1f

    var isCueOnFarSide = false
    if (hasInverse) {
        val screenAimPoint = floatArrayOf(viewWidth / 2f, viewHeight.toFloat())
        val logicalAimPoint = FloatArray(2)
        inversePitchMatrix.mapPoints(logicalAimPoint, screenAimPoint)
        val aimDirX = newCueCenter.x - logicalAimPoint[0]
        val aimDirY = newCueCenter.y - logicalAimPoint[1]
        val magAimDirSq = aimDirX * aimDirX + aimDirY * aimDirY
        if (magAimDirSq > 0.0001f) {
            val magAimDir = sqrt(magAimDirSq)
            val normAimDirX = aimDirX / magAimDir
            val normAimDirY = aimDirY / magAimDir
            val vecScreenToTargetX = newTargetCenter.x - logicalAimPoint[0]
            val vecScreenToTargetY = newTargetCenter.y - logicalAimPoint[1]
            val distTargetProj =
                vecScreenToTargetX * normAimDirX + vecScreenToTargetY * normAimDirY
            isCueOnFarSide = magAimDir > distTargetProj && distTargetProj > 0
        }
    }

    val isDeflectionDominantAngle = (this.rotationAngle > 90.5f && this.rotationAngle < 269.5f)
    val isImpossible = isCueOnFarSide || isPhysicalOverlap || isDeflectionDominantAngle

    return this.copy(
        targetCircleCenter = newTargetCenter,
        cueCircleCenter = newCueCenter,
        logicalRadius = newLogicalRadius,
        pitchMatrix = pitchMatrix,
        inversePitchMatrix = inversePitchMatrix,
        hasInverseMatrix = hasInverse,
        isImpossibleShot = isImpossible
    )
}
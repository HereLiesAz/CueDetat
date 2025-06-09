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

    fun adaptThemeFromBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return

        viewModelScope.launch {
            // Generate the palette asynchronously
            val palette = Palette.from(bitmap).generate()
            // Use our custom logic to create a Material 3 ColorScheme
            _dynamicColorScheme.value = createSchemeFromPalette(palette)
        }
    }

    // Resets the theme back to the default
    fun resetTheme() {
        _dynamicColorScheme.value = null
    }

    /**
     * Creates a Material 3 ColorScheme from a Palette instance.
     * It prioritizes vibrant colors and ensures readable "on" colors.
     */
    private fun createSchemeFromPalette(palette: Palette): ColorScheme {
        // Pick primary and secondary colors from the palette
        val primaryColor = Color(palette.getVibrantColor(palette.getMutedColor(0xFF00E5FF.toInt())))
        val secondaryColor =
            Color(palette.getLightVibrantColor(palette.getDominantColor(0xFF4DD0E1.toInt())))

        // Determine if the background should be light or dark
        val isDark = ColorUtils.calculateLuminance(primaryColor.toArgb()) < 0.5

        if (isDark) {
            // Generate a Dark Color Scheme
            val onPrimaryColor = getOnColorFor(primaryColor)
            val onSecondaryColor = getOnColorFor(secondaryColor)
            return darkColorScheme(
                primary = primaryColor,
                onPrimary = onPrimaryColor,
                primaryContainer = primaryColor.copy(alpha = 0.3f),
                secondary = secondaryColor,
                onSecondary = onSecondaryColor,
                // You can derive other colors or use fallbacks
                tertiary = Color(palette.getDarkMutedColor(0xFFF50057.toInt())),
                background = Color(palette.getDarkMutedColor(0xFF1A1C1C.toInt())),
                surface = Color(palette.getMutedColor(0xFF1A1C1C.toInt())),
                onBackground = getOnColorFor(Color(palette.getDarkMutedColor(0xFF1A1C1C.toInt()))),
                onSurface = getOnColorFor(Color(palette.getMutedColor(0xFF1A1C1C.toInt())))
            )
        } else {
            // Generate a Light Color Scheme
            val onPrimaryColor = getOnColorFor(primaryColor)
            val onSecondaryColor = getOnColorFor(secondaryColor)
            return lightColorScheme(
                primary = primaryColor,
                onPrimary = onPrimaryColor,
                primaryContainer = primaryColor.copy(alpha = 0.3f),
                secondary = secondaryColor,
                onSecondary = onSecondaryColor,
                tertiary = Color(palette.getLightMutedColor(0xFFD81B60.toInt())),
                background = Color(palette.getLightMutedColor(0xFFFAFDFD.toInt())),
                surface = Color(palette.getDominantColor(0xFFFAFDFD.toInt())),
                onBackground = getOnColorFor(Color(palette.getLightMutedColor(0xFFFAFDFD.toInt()))),
                onSurface = getOnColorFor(Color(palette.getDominantColor(0xFFFAFDFD.toInt())))
            )
        }
    }

    /**
     * Utility to determine if text on a color should be black or white for contrast.
     */
    private fun getOnColorFor(color: Color): Color {
        return if (ColorUtils.calculateLuminance(color.toArgb()) > 0.5) Color.Black else Color.White
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
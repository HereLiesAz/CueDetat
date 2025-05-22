package com.hereliesaz.cuedetat

import android.content.res.Resources
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View // Import View for visibility
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView // Import ImageView
import android.widget.SeekBar
import android.widget.TextView // Import TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat // Import for font loading
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hereliesaz.cuedetat.protractor.ProtractorOverlayView
import com.hereliesaz.cuedetat.ui.theme.PoolProtractorTheme
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.system.AppCameraManager
import com.hereliesaz.cuedetat.system.DevicePitchSensor
import com.hereliesaz.cuedetat.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), ProtractorOverlayView.ProtractorStateListener {

    private companion object {
        private const val TAG = "PoolProtractorApp"
        private const val USER_PREFERRED_MAX_ZOOM_FRACTION_OF_TOTAL_RANGE = 0.3f
        private const val SLIDER_PROGRESS_DEDICATED_TO_PREFERRED_RANGE = 95.0f
    }

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var protractorOverlayView: ProtractorOverlayView
    private lateinit var zoomSlider: SeekBar
    private lateinit var resetButton: FloatingActionButton
    private lateinit var zoomCycleButton: FloatingActionButton
    private lateinit var helpButton: FloatingActionButton
    private lateinit var controlsLayout: ConstraintLayout

    // Title Views
    private lateinit var appTitleTextView: TextView
    private lateinit var appTitleLogoImageView: ImageView

    private lateinit var appCameraManager: AppCameraManager
    private lateinit var devicePitchSensor: DevicePitchSensor

    private var valuesChangedSinceLastReset = false
    private var helpTextCurrentlyVisible = true // Track state for title toggling

    private enum class ZoomCycleState { MIN_ZOOM, MAX_ZOOM }
    private var nextZoomCycleState: ZoomCycleState = ZoomCycleState.MIN_ZOOM

    private val totalZoomFactorRange = ProtractorConfig.MAX_ZOOM_FACTOR - ProtractorConfig.MIN_ZOOM_FACTOR
    private val sliderEffectiveMaxZoomFactor = ProtractorConfig.MIN_ZOOM_FACTOR + (totalZoomFactorRange * USER_PREFERRED_MAX_ZOOM_FRACTION_OF_TOTAL_RANGE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        findViewById<FrameLayout>(R.id.activity_main_root)
        controlsLayout = findViewById(R.id.controls_layout)

        ViewCompat.setOnApplyWindowInsetsListener(controlsLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        cameraPreviewView = findViewById(R.id.cameraPreviewView)
        protractorOverlayView = findViewById(R.id.protractorOverlayView)
        zoomSlider = findViewById(R.id.zoomSlider)
        resetButton = findViewById(R.id.resetButton)
        zoomCycleButton = findViewById(R.id.zoomCycleButton)
        helpButton = findViewById(R.id.helpButton)

        // Initialize Title Views
        appTitleTextView = findViewById(R.id.appTitleTextView)
        appTitleLogoImageView = findViewById(R.id.appTitleLogoImageView)

        // Apply custom font to TextView title
        try {
            val archivoBlackTypeface = ResourcesCompat.getFont(this, R.font.archivo_black_regular)
            appTitleTextView.typeface = archivoBlackTypeface
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Archivo Black font not found for title, using default.", e)
        }


        protractorOverlayView.listener = this
        helpTextCurrentlyVisible = protractorOverlayView.getAreTextLabelsVisible() // Sync initial state
        updateTitleVisibility() // Set initial title/logo state

        appCameraManager = AppCameraManager(this, this, cameraPreviewView)
        devicePitchSensor = DevicePitchSensor(this) { pitchAngle ->
            protractorOverlayView.setPitchAngle(pitchAngle)
        }

        val composeViewForTheme = findViewById<ComposeView>(R.id.composeThemeView)
        composeViewForTheme.setContent {
            PoolProtractorTheme {
                val currentColorScheme = MaterialTheme.colorScheme
                SideEffect {
                    protractorOverlayView.applyMaterialYouColors(currentColorScheme)
                }
            }
        }

        appCameraManager.checkPermissionsAndSetupCamera()
        setupControls()
    }

    private fun convertSliderProgressToZoomFactor(progressInt: Int): Float {
        val progress = progressInt.toFloat()
        val targetZoom: Float

        if (progress <= SLIDER_PROGRESS_DEDICATED_TO_PREFERRED_RANGE) {
            val t = if (SLIDER_PROGRESS_DEDICATED_TO_PREFERRED_RANGE == 0f) {
                if (progress == 0f) 0f else 1f
            } else {
                progress / SLIDER_PROGRESS_DEDICATED_TO_PREFERRED_RANGE
            }
            val preferredZoomRange = sliderEffectiveMaxZoomFactor - ProtractorConfig.MIN_ZOOM_FACTOR
            targetZoom = ProtractorConfig.MIN_ZOOM_FACTOR + t * preferredZoomRange
        } else {
            targetZoom = sliderEffectiveMaxZoomFactor
        }
        return targetZoom.coerceIn(ProtractorConfig.MIN_ZOOM_FACTOR, max(sliderEffectiveMaxZoomFactor, ProtractorConfig.MIN_ZOOM_FACTOR))
    }

    private fun convertZoomFactorToSliderProgress(zoomFactorVal: Float): Int {
        val currentZoomFactor = zoomFactorVal.coerceIn(ProtractorConfig.MIN_ZOOM_FACTOR, ProtractorConfig.MAX_ZOOM_FACTOR)
        val progress: Int

        if (currentZoomFactor <= sliderEffectiveMaxZoomFactor) {
            val preferredZoomRange = sliderEffectiveMaxZoomFactor - ProtractorConfig.MIN_ZOOM_FACTOR
            val t = if (preferredZoomRange <= 0.0001f) {
                if (currentZoomFactor <= ProtractorConfig.MIN_ZOOM_FACTOR + 0.00005f) 0f else 1f
            } else {
                (currentZoomFactor - ProtractorConfig.MIN_ZOOM_FACTOR) / preferredZoomRange
            }
            progress = (t * SLIDER_PROGRESS_DEDICATED_TO_PREFERRED_RANGE).toInt()
        } else {
            progress = 100
        }
        return progress.coerceIn(0, 100)
    }


    private fun setupControls() {
        updateZoomSliderFromFactor(protractorOverlayView.getZoomFactor())

        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val zoomValue = convertSliderProgressToZoomFactor(progress)
                    protractorOverlayView.setZoomFactor(zoomValue)
                    nextZoomCycleState = ZoomCycleState.MIN_ZOOM
                    onUserInteraction()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        resetButton.setOnClickListener { handleResetAction() }
        helpButton.setOnClickListener {
            protractorOverlayView.toggleHelpersVisibility()
            helpTextCurrentlyVisible = protractorOverlayView.getAreTextLabelsVisible() // Update local state
            updateTitleVisibility() // Toggle title/logo
            onUserInteraction() // General interaction
        }
        zoomCycleButton.setOnClickListener { handleZoomCycleAction() }
    }

    private fun updateTitleVisibility() {
        if (helpTextCurrentlyVisible) {
            appTitleTextView.visibility = View.VISIBLE
            appTitleLogoImageView.visibility = View.GONE
        } else {
            appTitleTextView.visibility = View.GONE
            appTitleLogoImageView.visibility = View.VISIBLE
        }
    }


    private fun handleZoomCycleAction() {
        when (nextZoomCycleState) {
            ZoomCycleState.MIN_ZOOM -> {
                protractorOverlayView.setZoomFactor(ProtractorConfig.MIN_ZOOM_FACTOR)
                nextZoomCycleState = ZoomCycleState.MAX_ZOOM
            }
            ZoomCycleState.MAX_ZOOM -> {
                protractorOverlayView.setZoomFactor(ProtractorConfig.MAX_ZOOM_FACTOR)
                nextZoomCycleState = ZoomCycleState.MIN_ZOOM
            }
        }
        onUserInteraction()
    }


    private fun handleResetAction() {
        protractorOverlayView.resetToDefaults()
        helpTextCurrentlyVisible = protractorOverlayView.getAreTextLabelsVisible() // Reset visibility state
        updateTitleVisibility() // Update title based on reset state
        nextZoomCycleState = ZoomCycleState.MIN_ZOOM
        Toast.makeText(this, "View reset to defaults", Toast.LENGTH_SHORT).show()
        valuesChangedSinceLastReset = false
    }

    override fun onZoomChanged(newZoomFactor: Float) {
        updateZoomSliderFromFactor(newZoomFactor)
        if (abs(newZoomFactor - ProtractorConfig.MIN_ZOOM_FACTOR) < 0.01f) {
            nextZoomCycleState = ZoomCycleState.MAX_ZOOM
        } else if (abs(newZoomFactor - ProtractorConfig.MAX_ZOOM_FACTOR) < 0.01f) {
            nextZoomCycleState = ZoomCycleState.MIN_ZOOM
        }
        onUserInteraction()
    }

    override fun onRotationChanged(newRotationAngle: Float) {
        onUserInteraction()
    }

    override fun onUserInteraction() {
        valuesChangedSinceLastReset = true
    }

    private fun updateZoomSliderFromFactor(factor: Float) {
        val progress = convertZoomFactorToSliderProgress(factor)
        zoomSlider.progress = progress
    }

    override fun onResume() {
        super.onResume()
        devicePitchSensor.register()
    }

    override fun onPause() {
        super.onPause()
        devicePitchSensor.unregister()
    }
}
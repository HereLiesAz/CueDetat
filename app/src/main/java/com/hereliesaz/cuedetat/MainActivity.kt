// app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt
package com.hereliesaz.cuedetat

/**
 * **DEVELOPER NOTE (MANDATORY - READ AND ADHERE):**
 *
 * ALL DISTINCT PIECES OF LOGIC, ALGORITHMS, OR SIGNIFICANT HELPER FUNCTIONALITIES
 * MUST BE EXTRACTED INTO THEIR OWN DEDICATED FILES. THESE FILES SHOULD RESIDE
 * WITHIN APPROPRIATELY NAMED DIRECTORIES/PACKAGES THAT REFLECT THEIR PURPOSE.
 *
 * DO NOT ADD COMPLEX, REUSABLE, OR SUBSTANTIAL LOGIC BLOCKS AS PRIVATE METHODS
 * WITHIN THIS ACTIVITY OR OTHER LARGE CLASSES (e.g., MainOverlayView) IF THEY
 * CAN BE PROPERLY MODULARIZED.
 *
 * THE GOAL IS SINGLE RESPONSIBILITY AND HIGH COHESION AT THE FILE LEVEL.
 * THIS PRINCIPLE APPLIES RETROACTIVELY TO ANY EXISTING LOGIC THAT HAS NOT YET
 * BEEN MODULARIZED AND IS A STRICT REQUIREMENT FOR ALL FUTURE DEVELOPMENT.
 *
 * MAKE LOGIC:
 * 1. OBVIOUS IN ITS LOCATION.
 * 2. EASILY FINDABLE.
 * 3. MODULAR AND INDEPENDENT WHERE POSSIBLE.
 *
 * THE ZoomSliderLogic.kt FILE IS AN EXAMPLE OF THIS PRINCIPLE IN ACTION.
 * ALL SIMILARLY COMPLEX OR DISTINCT LOGIC SETS MUST FOLLOW THIS PATTERN.
 * FAILURE TO DO SO WILL RESULT IN CODE REJECTION/REWORK.
 */

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.system.CameraManager // Use new CameraManager
import com.hereliesaz.cuedetat.system.PitchSensor
import com.hereliesaz.cuedetat.ui.theme.PoolProtractorTheme
import com.hereliesaz.cuedetat.view.MainOverlayView
import com.hereliesaz.cuedetat.view.MainOverlayView.AppStateListener
import com.hereliesaz.cuedetat.state.AppState.SelectionMode // Import SelectionMode
import com.hereliesaz.cuedetat.view.utility.ZoomSliderLogic
import kotlin.math.abs

class MainActivity : AppCompatActivity(), AppStateListener {

    private companion object {
        private val TAG = AppConfig.TAG + "_MainActivity"
    }

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var mainOverlayView: MainOverlayView
    private lateinit var zoomSlider: SeekBar
    private lateinit var resetButton: FloatingActionButton
    private lateinit var zoomCycleButton: FloatingActionButton
    private lateinit var helpButton: FloatingActionButton
    private lateinit var controlsLayout: ConstraintLayout

    private lateinit var appTitleTextView: TextView
    private lateinit var appTitleLogoImageView: ImageView

    private lateinit var cameraManager: CameraManager
    private lateinit var pitchSensor: PitchSensor

    private var valuesChangedSinceLastReset = false
    private var helpTextCurrentlyVisible = true
    private var currentSelectionMode: SelectionMode = SelectionMode.SELECTING_CUE_BALL // Track mode in Activity

    private enum class ZoomCycleState { MIN_ZOOM, MAX_ZOOM }
    private var nextZoomCycleState: ZoomCycleState = ZoomCycleState.MIN_ZOOM


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window for full-screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // Keep screen on

        setContentView(R.layout.activity_main)
        controlsLayout = findViewById(R.id.controls_layout)

        // Apply system bar insets as padding to the controls layout to avoid overlap
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

        // Initialize UI components
        cameraPreviewView = findViewById(R.id.cameraPreviewView)
        mainOverlayView = findViewById(R.id.protractorOverlayView)
        zoomSlider = findViewById(R.id.zoomSlider)
        resetButton = findViewById(R.id.resetButton)
        zoomCycleButton = findViewById(R.id.zoomCycleButton)
        helpButton = findViewById(R.id.helpButton)
        appTitleTextView = findViewById(R.id.appTitleTextView)
        appTitleLogoImageView = findViewById(R.id.appTitleLogoImageView)

        // Set custom font for the app title
        try {
            val archivoBlackTypeface = ResourcesCompat.getFont(this, R.font.archivo_black_regular)
            appTitleTextView.typeface = archivoBlackTypeface
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Archivo Black font not found for title, using default.", e)
        }

        mainOverlayView.listener = this // Set MainActivity as the listener for MainOverlayView events
        helpTextCurrentlyVisible = mainOverlayView.getAreHelperTextsVisible() // Get initial visibility state
        currentSelectionMode = mainOverlayView.getSelectionMode() // Get initial selection mode
        updateUiVisibilityForSelectionMode() // Update UI based on initial mode
        updateTitleVisibility() // Update title/logo based on initial visibility

        // Initialize CameraManager and PitchSensor
        cameraManager = CameraManager(this, this, cameraPreviewView, mainOverlayView) // Pass mainOverlayView for camera frames
        pitchSensor = PitchSensor(
            this,
            AppConfig.FORWARD_TILT_AS_FLAT_OFFSET_DEGREES, // Use offset from AppConfig
            { pitchAngle ->
                mainOverlayView.setDevicePitchAngle(pitchAngle) // Update overlay with pitch angle
            }
        )

        // Initialize MainOverlayView's components here, passing CameraManager
        mainOverlayView.initializeComponents(cameraManager)


        // Setup ComposeView for Material 3 theming (colors applied to legacy View system)
        val composeViewForTheme = findViewById<ComposeView>(R.id.composeThemeView)
        composeViewForTheme.setContent {
            PoolProtractorTheme {
                val currentColorScheme = MaterialTheme.colorScheme
                SideEffect {
                    // Pass the current Material 3 color scheme to the custom view for its paints
                    mainOverlayView.applyMaterialYouColors(currentColorScheme)
                }
            }
        }

        cameraManager.checkPermissionsAndSetupCamera() // Request camera permissions and start camera
        setupControls() // Set up UI control listeners
    }

    /**
     * Configures listeners for UI controls (zoom slider, buttons).
     */
    private fun setupControls() {
        // Use CameraManager's current zoom to initialize slider
        updateZoomSliderFromFactor(mainOverlayView.getZoomFactor())

        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val minZoom = mainOverlayView.getMinCameraZoomFactor()
                    val maxZoom = mainOverlayView.getMaxCameraZoomFactor()
                    val zoomValue = ZoomSliderLogic.convertSliderProgressToZoomFactor(progress, minZoom, maxZoom)
                    mainOverlayView.setZoomFactor(zoomValue)
                    nextZoomCycleState = ZoomCycleState.MIN_ZOOM // Reset cycle state when slider is used
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                this@MainActivity.onUserInteraction() // Notify of user interaction
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        resetButton.setOnClickListener { handleResetAction() }
        helpButton.setOnClickListener {
            mainOverlayView.toggleHelperTextVisibility() // Toggle visibility via overlay view
        }
        zoomCycleButton.setOnClickListener { handleZoomCycleAction() }
    }

    /**
     * Updates the visibility of the app title text or logo based on helper text visibility.
     */
    private fun updateTitleVisibility() {
        if (helpTextCurrentlyVisible) {
            appTitleTextView.visibility = View.VISIBLE
            appTitleLogoImageView.visibility = View.GONE
        } else {
            appTitleTextView.visibility = View.GONE
            appTitleLogoImageView.visibility = View.VISIBLE
        }
    }

    /**
     * Updates the visibility of UI controls based on the current selection mode.
     * Only zoom and rotate interactions are enabled in AIMING mode.
     */
    private fun updateUiVisibilityForSelectionMode() {
        val controlsVisibleInAiming = currentSelectionMode == SelectionMode.AIMING
        zoomSlider.visibility = if (controlsVisibleInAiming) View.VISIBLE else View.INVISIBLE
        zoomCycleButton.visibility = if (controlsVisibleInAiming) View.VISIBLE else View.INVISIBLE

        // Help and Reset buttons are always visible, but their actions might be restricted by MainOverlayView
        // Their visibility logic is separate.
    }

    /**
     * Handles cycling through predefined zoom levels (min/max).
     */
    private fun handleZoomCycleAction() {
        // Only allow zoom cycling if in AIMING mode
        if (currentSelectionMode != SelectionMode.AIMING) return

        val currentZoom = mainOverlayView.getZoomFactor()
        val minZoom = mainOverlayView.getMinCameraZoomFactor()
        val maxZoom = mainOverlayView.getMaxCameraZoomFactor()

        val targetZoom = when (nextZoomCycleState) {
            ZoomCycleState.MIN_ZOOM -> {
                nextZoomCycleState = ZoomCycleState.MAX_ZOOM
                minZoom
            }
            ZoomCycleState.MAX_ZOOM -> {
                nextZoomCycleState = ZoomCycleState.MIN_ZOOM
                maxZoom
            }
        }
        mainOverlayView.setZoomFactor(targetZoom) // Set zoom via overlay view
    }

    /**
     * Resets the overlay view to its default interaction state.
     */
    private fun handleResetAction() {
        mainOverlayView.resetInteractionsToDefaults() // Reset view state
        nextZoomCycleState = ZoomCycleState.MIN_ZOOM // Reset zoom cycle state
        Toast.makeText(this, "View reset to defaults", Toast.LENGTH_SHORT).show()
    }

    // --- MainOverlayView.AppStateListener implementations ---

    override fun onZoomChanged(newZoomFactor: Float) {
        // Update the slider to reflect the actual camera zoom ratio, which might be slightly different from requested
        updateZoomSliderFromFactor(newZoomFactor)
        // Update zoom cycle state based on current zoom factor
        val minZoom = mainOverlayView.getMinCameraZoomFactor()
        val maxZoom = mainOverlayView.getMaxCameraZoomFactor()
        if (abs(newZoomFactor - minZoom) < 0.01f) {
            nextZoomCycleState = ZoomCycleState.MAX_ZOOM
        } else if (abs(newZoomFactor - maxZoom) < 0.01f) {
            nextZoomCycleState = ZoomCycleState.MIN_ZOOM
        }
        valuesChangedSinceLastReset = true // Mark that user interaction occurred
    }

    override fun onRotationChanged(newRotationAngle: Float) {
        valuesChangedSinceLastReset = true // Mark that user interaction occurred
    }

    override fun onUserInteraction() {
        valuesChangedSinceLastReset = true // Mark that user interaction occurred
        // Check if helper text visibility changed and update title
        val currentOverlayVisibility = mainOverlayView.getAreHelperTextsVisible()
        if (helpTextCurrentlyVisible != currentOverlayVisibility) {
            helpTextCurrentlyVisible = currentOverlayVisibility
            updateTitleVisibility()
        }
    }

    override fun onCueBallSelected(ballId: String?) {
        val message = if (ballId != null) "Cue ball selected: $ballId" else "Cue ball deselected."
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onTargetBallSelected(ballId: String?) {
        val message = if (ballId != null) "Target ball selected: $ballId" else "Target ball deselected."
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSelectionModeChanged(mode: SelectionMode) {
        currentSelectionMode = mode
        updateUiVisibilityForSelectionMode() // Update UI based on new mode
        val message = when (mode) {
            SelectionMode.SELECTING_CUE_BALL -> "Please tap the cue ball."
            SelectionMode.SELECTING_TARGET_BALL -> "Now tap the target ball."
            SelectionMode.AIMING -> "Aiming mode activated."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Selection Mode changed to: $mode")
    }

    /**
     * Updates the zoom slider's progress based on a given zoom factor.
     * @param factor The zoom factor to convert to slider progress.
     */
    private fun updateZoomSliderFromFactor(factor: Float) {
        val minZoom = mainOverlayView.getMinCameraZoomFactor()
        val maxZoom = mainOverlayView.getMaxCameraZoomFactor()
        val progress = ZoomSliderLogic.convertZoomFactorToSliderProgress(factor, minZoom, maxZoom)
        // Only update if different to avoid infinite loops with onProgressChanged
        if (zoomSlider.progress != progress) {
            zoomSlider.progress = progress
        }
    }

    override fun onResume() {
        super.onResume()
        pitchSensor.register() // Register pitch sensor listener
        Log.d(TAG, "MainActivity onResume, PitchSensor registered.")
    }

    override fun onPause() {
        super.onPause()
        pitchSensor.unregister() // Unregister pitch sensor listener
        Log.d(TAG, "MainActivity onPause, PitchSensor unregistered.")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown() // Ensure camera resources are released
        Log.d(TAG, "MainActivity onDestroy, CameraManager shutdown.")
    }
}
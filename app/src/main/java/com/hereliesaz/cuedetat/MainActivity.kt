// FILE: app\src\main\java\com\hereliesaz\cuedetat\MainActivity.kt
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

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
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
import com.hereliesaz.cuedetat.state.AppState.SelectionMode
import com.hereliesaz.cuedetat.system.CameraManager
import com.hereliesaz.cuedetat.system.PitchSensor
import com.hereliesaz.cuedetat.ui.theme.PoolProtractorTheme // Added import
import com.hereliesaz.cuedetat.updater.GitHubUpdater
import com.hereliesaz.cuedetat.view.MainOverlayView
import com.hereliesaz.cuedetat.view.MainOverlayView.AppStateListener
import com.hereliesaz.cuedetat.view.utility.ZoomSliderLogic
import kotlin.math.abs

class MainActivity : AppCompatActivity(), AppStateListener, GitHubUpdater.Callback {

    private companion object {
        private val TAG = AppConfig.TAG + "_MainActivity"
    }

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var mainOverlayView: MainOverlayView
    private lateinit var zoomSlider: SeekBar
    private lateinit var resetButton: FloatingActionButton
    private lateinit var zoomCycleButton: FloatingActionButton
    // Removed helpButton from declaration
    private lateinit var controlsLayout: ConstraintLayout
    private lateinit var appTitleContainer: FrameLayout // Reference to the FrameLayout wrapping title/logo

    private lateinit var appTitleTextView: TextView
    private lateinit var appTitleLogoImageView: ImageView

    private lateinit var cameraManager: CameraManager
    private lateinit var pitchSensor: PitchSensor
    private lateinit var gitHubUpdater: GitHubUpdater // GitHubUpdater instance

    private var valuesChangedSinceLastReset = false
    private var helpTextCurrentlyVisible = false // Default to false
    private var currentSelectionMode: SelectionMode = SelectionMode.SELECTING_CUE_BALL // Track mode in Activity

    private enum class ZoomCycleState { MIN_ZOOM, MAX_ZOOM }
    private var nextZoomCycleState: ZoomCycleState = ZoomCycleState.MIN_ZOOM


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started.")

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
                top = insets.top, // Fixed typo: 'inets' to 'insets'
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
        // Removed helpButton from findViewById
        appTitleContainer = findViewById(R.id.appTitleContainer) // Get reference to the FrameLayout
        appTitleTextView = findViewById(R.id.appTitleTextView)
        appTitleLogoImageView = findViewById(R.id.appTitleLogoImageView)

        Log.d(TAG, "mainOverlayView found: ${::mainOverlayView.isInitialized}")
        Log.d(TAG, "appTitleTextView visibility (XML default): ${appTitleTextView.visibility}")


        // Set custom font for the app title
        try {
            val archivoBlackTypeface = ResourcesCompat.getFont(this, R.font.archivo_black_regular)
            appTitleTextView.typeface = archivoBlackTypeface
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Archivo Black font not found for title, using default.", e)
        }

        mainOverlayView.listener = this // Set MainActivity as the listener for MainOverlayView events
        helpTextCurrentlyVisible = mainOverlayView.getAreHelperTextsVisible() // Get initial visibility state (will be false from AppState default)
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
        // This must be called after the view has been measured (in onSizeChanged)
        // For Compose theme application, ensure this call is made when view is ready.
        // It's safe to call here, but MainOverlayView itself might defer internal setup if w/h are 0.
        mainOverlayView.initializeComponents(cameraManager)


        // Initialize GitHubUpdater
        val currentAppVersionCode = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package name not found for version code lookup: ${e.message}")
            0 // Default to 0 if not found
        }
        gitHubUpdater = GitHubUpdater(
            this,
            AppConfig.GITHUB_REPO_OWNER,
            AppConfig.GITHUB_REPO_NAME,
            currentAppVersionCode,
            this // MainActivity implements GitHubUpdater.Callback
        )


        // Setup ComposeView for Material 3 theming (colors applied to legacy View system)
        val composeViewForTheme = findViewById<ComposeView>(R.id.composeThemeView)
        composeViewForTheme.setContent {
            // Fix: PoolProtractorTheme is a @Composable function, it must be called inside a @Composable scope.
            // The setContent block itself is a @Composable scope.
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
        // Removed helpButton.setOnClickListener {}
        zoomCycleButton.setOnClickListener { handleZoomCycleAction() }

        // Set up click listener for the app title/logo to show popup menu
        appTitleContainer.setOnClickListener { view ->
            showAppMenu(view)
        }
    }

    /**
     * Displays a popup menu when the app title/logo is tapped.
     */
    private fun showAppMenu(view: View) {
        // Create a ContextThemeWrapper to apply the custom PopupMenu style
        val wrapper = ContextThemeWrapper(this, R.style.AppTheme_PopupMenu)
        // Use ContextThemeWrapper for PopupMenu constructor
        val popup = PopupMenu(wrapper, view, Gravity.END) // Anchor to the right of the view
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_check_for_updates -> {
                    Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    gitHubUpdater.checkForUpdate()
                    true
                }
                R.id.action_toggle_help -> { // Handle the new help toggle menu item
                    mainOverlayView.toggleHelperTextVisibility()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    /**
     * Updates the visibility of the app title text or logo based on helper text visibility.
     */
    private fun updateTitleVisibility() {
        if (helpTextCurrentlyVisible) {
            appTitleTextView.visibility = View.VISIBLE
            appTitleLogoImageView.visibility = View.GONE
            Log.d(TAG, "Title visibility: Text Visible, Logo Gone (Help Text On)")
        } else {
            appTitleTextView.visibility = View.GONE
            appTitleLogoImageView.visibility = View.VISIBLE
            Log.d(TAG, "Title visibility: Text Gone, Logo Visible (Help Text Off)")
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
        Log.d(TAG, "UI visibility updated for mode: $currentSelectionMode. Zoom controls visible: $controlsVisibleInAiming")
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

    // --- GitHubUpdater.Callback implementations ---
    override fun onUpdateCheckComplete(latestRelease: GitHubUpdater.ReleaseInfo?, isNewer: Boolean) {
        if (latestRelease == null) {
            // Error already handled by onError from updater
            return
        }

        val dialogTitle: String
        val dialogMessage: String
        val positiveButtonText: String

        if (isNewer) {
            dialogTitle = "New Update Available!"
            dialogMessage = "Version ${latestRelease.releaseName} is available.\n\n" +
                    "Would you like to download and install it?"
            positiveButtonText = "Update"
        } else {
            dialogTitle = "You are on the Latest Version"
            dialogMessage = "Version ${latestRelease.releaseName} is the latest.\n\n" +
                    "Do you want to reinstall this version?"
            positiveButtonText = "Reinstall"
        }

        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setMessage(dialogMessage)
                .setPositiveButton(positiveButtonText) { dialog: DialogInterface, _: Int ->
                    gitHubUpdater.downloadUpdate(latestRelease.downloadUrl, latestRelease.releaseName)
                    Toast.makeText(this, "Downloading update...", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onUpdateDownloadComplete(downloadId: Long) {
        runOnUiThread {
            Toast.makeText(this, "Download complete. Installing...", Toast.LENGTH_LONG).show()
            gitHubUpdater.installUpdate(downloadId) // Pass the downloadId to installUpdate
        }
    }

    override fun onUpdateDownloadFailed(reason: String) {
        runOnUiThread {
            Toast.makeText(this, "Update download failed: $reason", Toast.LENGTH_LONG).show()
        }
    }

    fun onNoUpdateAvailable() {
        // This callback is now redundant if onUpdateCheckComplete handles `isNewer` flag.
        // If `latestRelease` is null, onError already handles it.
        // If `latestRelease` is not null and `isNewer` is false, `onUpdateCheckComplete` shows reinstall dialog.
        Log.d(TAG, "No newer update found (handled by onUpdateCheckComplete).")
    }

    override fun onError(message: String) {
        runOnUiThread {
            Toast.makeText(this, "Update error: $message", Toast.LENGTH_LONG).show()
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
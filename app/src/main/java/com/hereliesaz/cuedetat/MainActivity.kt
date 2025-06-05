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
import com.hereliesaz.cuedetat.ui.theme.PoolProtractorTheme
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
    private lateinit var controlsLayout: ConstraintLayout
    private lateinit var appTitleContainer: FrameLayout

    private lateinit var appTitleTextView: TextView
    private lateinit var appTitleLogoImageView: ImageView

    private lateinit var cameraManager: CameraManager
    private lateinit var pitchSensor: PitchSensor
    private lateinit var gitHubUpdater: GitHubUpdater

    private var valuesChangedSinceLastReset = false
    private var helpTextCurrentlyVisible = false
    private var currentSelectionMode: SelectionMode = SelectionMode.SELECTING_CUE_BALL

    private enum class ZoomCycleState { MIN_ZOOM, MAX_ZOOM }
    private var nextZoomCycleState: ZoomCycleState = ZoomCycleState.MIN_ZOOM


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started.")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
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
        mainOverlayView = findViewById(R.id.protractorOverlayView)
        zoomSlider = findViewById(R.id.zoomSlider)
        resetButton = findViewById(R.id.resetButton)
        zoomCycleButton = findViewById(R.id.zoomCycleButton)
        appTitleContainer = findViewById(R.id.appTitleContainer)
        appTitleTextView = findViewById(R.id.appTitleTextView)
        appTitleLogoImageView = findViewById(R.id.appTitleLogoImageView)

        Log.d(TAG, "mainOverlayView found: ${::mainOverlayView.isInitialized}")
        Log.d(TAG, "appTitleTextView visibility (XML default): ${appTitleTextView.visibility}")

        try {
            val archivoBlackTypeface = ResourcesCompat.getFont(this, R.font.archivo_black_regular)
            appTitleTextView.typeface = archivoBlackTypeface
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Archivo Black font not found for title, using default.", e)
        }

        mainOverlayView.listener = this
        helpTextCurrentlyVisible = mainOverlayView.getAreHelperTextsVisible()
        currentSelectionMode = mainOverlayView.getSelectionMode()
        updateUiVisibilityForSelectionMode()
        updateTitleVisibility()

        cameraManager = CameraManager(this, this, cameraPreviewView, mainOverlayView)
        pitchSensor = PitchSensor(
            this,
            AppConfig.FORWARD_TILT_AS_FLAT_OFFSET_DEGREES,
            { pitchAngle ->
                mainOverlayView.setDevicePitchAngle(pitchAngle)
            }
        )

        mainOverlayView.setCameraManager(cameraManager)
        mainOverlayView.setZoomFactor(AppConfig.DEFAULT_ZOOM_FACTOR)


        val currentAppVersionCode = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package name not found for version code lookup: ${e.message}")
            0
        }
        gitHubUpdater = GitHubUpdater(
            this,
            AppConfig.GITHUB_REPO_OWNER,
            AppConfig.GITHUB_REPO_NAME,
            currentAppVersionCode,
            this
        )

        val composeViewForTheme = findViewById<ComposeView>(R.id.composeThemeView)
        composeViewForTheme.setContent {
            PoolProtractorTheme {
                val currentColorScheme = MaterialTheme.colorScheme
                SideEffect {
                    mainOverlayView.applyMaterialYouColors(currentColorScheme)
                }
            }
        }

        cameraManager.checkPermissionsAndSetupCamera()
        setupControls()
    }

    private fun setupControls() {
        updateZoomSliderFromFactor(mainOverlayView.getZoomFactor())

        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val minZoom = mainOverlayView.getMinCameraZoomFactor()
                    val maxZoom = mainOverlayView.getMaxCameraZoomFactor()
                    val zoomValue = ZoomSliderLogic.convertSliderProgressToZoomFactor(progress, minZoom, maxZoom)
                    mainOverlayView.setZoomFactor(zoomValue)
                    nextZoomCycleState = ZoomCycleState.MIN_ZOOM
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                this@MainActivity.onUserInteraction()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        resetButton.setOnClickListener { handleResetAction() }
        zoomCycleButton.setOnClickListener { handleZoomCycleAction() }

        appTitleContainer.setOnClickListener { view ->
            showAppMenu(view)
        }
    }

    private fun showAppMenu(view: View) {
        val wrapper = ContextThemeWrapper(this, R.style.AppTheme_PopupMenu)
        val popup = PopupMenu(wrapper, view, Gravity.END)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_check_for_updates -> {
                    Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    gitHubUpdater.checkForUpdate()
                    true
                }
                R.id.action_toggle_help -> {
                    mainOverlayView.toggleHelperTextVisibility()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

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

    private fun updateUiVisibilityForSelectionMode() {
        // Zoom slider and button are now always visible
        zoomSlider.visibility = View.VISIBLE
        zoomCycleButton.visibility = View.VISIBLE
        Log.d(TAG, "UI visibility updated. Zoom controls are always visible.")
    }

    private fun handleZoomCycleAction() {
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
        mainOverlayView.setZoomFactor(targetZoom)
    }

    private fun handleResetAction() {
        mainOverlayView.resetInteractionsToDefaults()
        nextZoomCycleState = ZoomCycleState.MIN_ZOOM
        Toast.makeText(this, "View reset to defaults", Toast.LENGTH_SHORT).show()
    }

    override fun onZoomChanged(newZoomFactor: Float) {
        updateZoomSliderFromFactor(newZoomFactor)
        val minZoom = mainOverlayView.getMinCameraZoomFactor()
        val maxZoom = mainOverlayView.getMaxCameraZoomFactor()
        if (abs(newZoomFactor - minZoom) < 0.01f) {
            nextZoomCycleState = ZoomCycleState.MAX_ZOOM
        } else if (abs(newZoomFactor - maxZoom) < 0.01f) {
            nextZoomCycleState = ZoomCycleState.MIN_ZOOM
        }
        valuesChangedSinceLastReset = true
    }

    override fun onRotationChanged(newRotationAngle: Float) {
        valuesChangedSinceLastReset = true
    }

    override fun onUserInteraction() {
        valuesChangedSinceLastReset = true
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
        updateUiVisibilityForSelectionMode()
        val message = when (mode) {
            SelectionMode.SELECTING_CUE_BALL -> "Please tap the cue ball."
            SelectionMode.SELECTING_TARGET_BALL -> "Now tap the target ball."
            SelectionMode.AIMING -> "Aiming mode activated."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Selection Mode changed to: $mode")
    }

    private fun updateZoomSliderFromFactor(factor: Float) {
        val minZoom = mainOverlayView.getMinCameraZoomFactor()
        val maxZoom = mainOverlayView.getMaxCameraZoomFactor()
        val progress = ZoomSliderLogic.convertZoomFactorToSliderProgress(factor, minZoom, maxZoom)
        if (zoomSlider.progress != progress) {
            zoomSlider.progress = progress
        }
    }

    override fun onUpdateCheckComplete(latestRelease: GitHubUpdater.ReleaseInfo?, isNewer: Boolean) {
        if (latestRelease == null) {
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
            gitHubUpdater.installUpdate(downloadId)
        }
    }

    override fun onUpdateDownloadFailed(reason: String) {
        runOnUiThread {
            Toast.makeText(this, "Update download failed: $reason", Toast.LENGTH_LONG).show()
        }
    }

    fun onNoUpdateAvailable() {
        Log.d(TAG, "No newer update found (handled by onUpdateCheckComplete).")
    }

    override fun onError(message: String) {
        runOnUiThread {
            Toast.makeText(this, "Update error: $message", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        pitchSensor.register()
        Log.d(TAG, "MainActivity onResume, PitchSensor registered.")
    }

    override fun onPause() {
        super.onPause()
        pitchSensor.unregister()
        Log.d(TAG, "MainActivity onPause, PitchSensor unregistered.")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
        Log.d(TAG, "MainActivity onDestroy, CameraManager shutdown.")
    }
}
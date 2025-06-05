// app/src/main/java/com/hereliesaz/cuedetat/view/MainOverlayView.kt
package com.hereliesaz.cuedetat.view

import android.content.Context
import android.graphics.Canvas // Android Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode // Import SelectionMode
import com.hereliesaz.cuedetat.drawing.DrawingCoordinator
import com.hereliesaz.cuedetat.view.gesture.GestureHandler
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball // Import the Ball data class
import com.hereliesaz.cuedetat.system.CameraManager // Import CameraManager
import kotlin.math.hypot

class MainOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = AppConfig.TAG + "_MainOverlayView"

    /**
     * Listener interface for external components (like MainActivity) to react to state changes
     * originating from user interaction with MainOverlayView.
     */
    interface AppStateListener {
        fun onZoomChanged(newZoomFactor: Float)
        fun onRotationChanged(newRotationAngle: Float)
        fun onUserInteraction() // Generic interaction event
        fun onCueBallSelected(ballId: String?) // New: Notify when a cue ball is selected or deselected
        fun onTargetBallSelected(ballId: String?) // New: Notify when a target ball is selected or deselected
        fun onSelectionModeChanged(mode: SelectionMode) // New: Notify when selection mode changes
    }

    var listener: AppStateListener? = null

    // Initialize with default AppConfig, AppState, AppPaints.
    // These will be fully setup once view dimensions are known and M3 colors applied.
    private val config = AppConfig // Direct object reference
    private val appState = AppState(config)
    private val appPaints = AppPaints(context, config)

    private lateinit var gestureHandler: GestureHandler
    private lateinit var drawingCoordinator: DrawingCoordinator
    private lateinit var gestureDetector: GestureDetector // For handling single taps
    private lateinit var cameraManagerRef: CameraManager // Reference to CameraManager for zoom control

    // Flag to ensure components are initialized only once after size is known
    private var areComponentsInitialized = false

    /**
     * Initializes core drawing and interaction components.
     * This is called once the view dimensions are available.
     * @param cameraManager A reference to the CameraManager for camera zoom control.
     */
    fun initializeComponents(cameraManager: CameraManager) {
        if (areComponentsInitialized || width == 0 || height == 0) return // Already initialized or no dimensions

        cameraManagerRef = cameraManager // Store reference

        gestureHandler = GestureHandler(
            context, appState, config, listener,
            onZoomChangedByGesture = { newZoomFactor ->
                setZoomFactorInternal(newZoomFactor, true)
            },
            onRotationChangedByGesture = { newRotationAngle ->
                setProtractorRotationAngleInternal(newRotationAngle, true)
            }
        )

        drawingCoordinator = DrawingCoordinator(
            appState, appPaints, config,
            viewWidthProvider = { width }, // Provide current view width
            viewHeightProvider = { height } // Provide current view height
        )

        // Initialize GestureDetector for tap events
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleSingleTap(e.x, e.y) // Handle tap for ball selection
                return true
            }
        })

        areComponentsInitialized = true
        Log.d(TAG, "MainOverlayView components initialized.")
    }


    /**
     * Applies Material 3 color scheme to the custom drawing paints.
     * This should be called after `onSizeChanged` or once view dimensions are set.
     *
     * @param colorScheme The Material 3 ColorScheme from Compose.
     */
    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        // Ensure components are ready before applying colors.
        // If initializeComponents has not been called externally yet,
        // it cannot proceed here.
        if (!areComponentsInitialized) {
            Log.w(TAG, "applyMaterialYouColors called before components initialized. Skipping.")
            return
        }
        appPaints.applyMaterialYouColors(colorScheme)
        invalidate() // Redraw with new colors
    }

    /**
     * Called when the size of the view changes. Initializes or re-initializes
     * components that depend on view dimensions.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // NOTE: initializeComponents must be called by MainActivity AFTER CameraManager is ready
            // because it now depends on cameraManagerRef.
            // We just ensure appState is initialized/updated with new dimensions here.
            appState.initialize(w, h)
            invalidate() // Request a redraw
        }
    }

    /**
     * The main drawing dispatch method. Calls the DrawingCoordinator to render elements.
     */
    override fun onDraw(canvas: Canvas) { // android.graphics.Canvas
        super.onDraw(canvas)
        // Guard against drawing before components are ready
        if (!areComponentsInitialized || !appState.isInitialized) {
            // Cannot call initializeComponents here as it requires cameraManagerRef now.
            // MainActivity is responsible for calling it.
            return
        }

        drawingCoordinator.onDraw(canvas)
    }

    /**
     * Handles touch events for gestures (pan, zoom, tap).
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Guard against uninitialized state
        if (!areComponentsInitialized || !appState.isInitialized) return false

        // Pass touch events to GestureDetector first for tap detection
        val gestureHandled = gestureDetector.onTouchEvent(event)
        // Then pass to custom GestureHandler for pan/zoom.
        // Only allow pan/zoom gestures if in AIMING mode.
        val panZoomHandled = if (appState.currentMode == SelectionMode.AIMING) {
            gestureHandler.onTouchEvent(event)
        } else {
            false
        }


        // Consume event if either handler processed it, or fall back to super
        return gestureHandled || panZoomHandled || super.onTouchEvent(event)
    }

    // --- New methods for ball tracking integration ---

    /**
     * Receives a list of detected balls and camera frame dimensions from the CameraManager.
     * Updates AppState.trackedBalls and attempts to manage selected balls.
     *
     * @param balls The list of detected Ball objects (with coordinates in camera frame pixels).
     * @param frameWidth The width of the camera frame in pixels.
     * @param frameHeight The height of the camera frame in pixels.
     */
    fun updateTrackedBalls(balls: List<Ball>, frameWidth: Int, frameHeight: Int) {
        if (!appState.isInitialized) return

        appState.frameWidth = frameWidth // Store camera frame width
        appState.frameHeight = frameHeight // Store camera frame height
        appState.trackedBalls = balls // Store all detected balls

        // Auto-select if currently in a selection mode and no ball is selected
        when (appState.currentMode) {
            SelectionMode.SELECTING_CUE_BALL -> {
                if (appState.selectedCueBallId == null) {
                    val firstBall = balls.firstOrNull()
                    if (firstBall != null) {
                        appState.updateSelectedCueBall(firstBall)
                        listener?.onCueBallSelected(firstBall.id)
                        Log.d(TAG, "Auto-selected cue ball: ${firstBall.id}")
                    }
                }
            }
            SelectionMode.SELECTING_TARGET_BALL -> {
                if (appState.selectedTargetBallId == null) {
                    // Try to auto-select a target ball that isn't the cue ball
                    val firstNonCueBall = balls.firstOrNull { it.id != appState.selectedCueBallId }
                    if (firstNonCueBall != null) {
                        appState.updateSelectedTargetBall(firstNonCueBall)
                        listener?.onTargetBallSelected(firstNonCueBall.id)
                        Log.d(TAG, "Auto-selected target ball: ${firstNonCueBall.id}")
                    }
                }
            }
            else -> { /* In AIMING mode, no auto-selection */ }
        }

        invalidate() // Request a redraw to show updated positions
    }

    /**
     * Sets the camera's zoom capabilities in AppState.
     */
    fun updateCameraZoomCapabilities(minZoom: Float, maxZoom: Float) {
        appState.updateCameraZoomCapabilities(minZoom, maxZoom)
    }

    /**
     * Handles a single tap event on the overlay for ball selection.
     * @param tapX The X-coordinate of the tap in MainOverlayView pixels.
     * @param tapY The Y-coordinate of the tap in MainOverlayView pixels.
     */
    private fun handleSingleTap(tapX: Float, tapY: Float) {
        var closestBall: Ball? = null
        var minDistance = Float.MAX_VALUE

        for (ball in appState.trackedBalls) {
            val dist = hypot(tapX - ball.x, tapY - ball.y)
            if (dist <= ball.radius + 20f) { // Add 20 pixels tolerance for easier tapping
                if (dist < minDistance) {
                    minDistance = dist
                    closestBall = ball
                }
            }
        }

        val previousMode = appState.currentMode
        var modeChanged = false

        when (appState.currentMode) {
            SelectionMode.SELECTING_CUE_BALL -> {
                if (closestBall != null) {
                    appState.updateSelectedCueBall(closestBall)
                    appState.currentMode = SelectionMode.SELECTING_TARGET_BALL
                    listener?.onCueBallSelected(closestBall.id)
                    Log.d(TAG, "User selected cue ball: ${closestBall.id}. Mode changed to ${appState.currentMode}")
                    modeChanged = true
                } else {
                    Log.d(TAG, "Tap ignored: No ball found, expecting cue ball selection.")
                }
            }
            SelectionMode.SELECTING_TARGET_BALL -> {
                if (closestBall != null) {
                    if (closestBall.id == appState.selectedCueBallId) {
                        // Tapped on cue ball again while selecting target, reset everything
                        appState.clearSelectedCueBall()
                        appState.clearSelectedTargetBall()
                        appState.currentMode = SelectionMode.SELECTING_CUE_BALL
                        listener?.onCueBallSelected(null)
                        listener?.onTargetBallSelected(null)
                        Log.d(TAG, "User tapped cue ball again, resetting selection. Mode changed to ${appState.currentMode}")
                        modeChanged = true
                    } else {
                        // Tapped a different ball, select as target
                        appState.updateSelectedTargetBall(closestBall)
                        appState.currentMode = SelectionMode.AIMING
                        listener?.onTargetBallSelected(closestBall.id)
                        Log.d(TAG, "User selected target ball: ${closestBall.id}. Mode changed to ${appState.currentMode}")
                        modeChanged = true
                    }
                } else {
                    // Tapped empty space, deselect target (if any) and go back to selecting cue
                    if (appState.selectedTargetBallId != null) {
                        appState.clearSelectedTargetBall()
                        listener?.onTargetBallSelected(null)
                        Log.d(TAG, "User deselected target ball. Still expecting target selection.")
                    } else {
                        Log.d(TAG, "Tap ignored: No ball found, expecting target ball selection.")
                    }
                }
            }
            SelectionMode.AIMING -> {
                if (closestBall != null) {
                    if (closestBall.id == appState.selectedCueBallId) {
                        // Tapped on cue ball, reset entire selection flow
                        appState.clearSelectedCueBall()
                        appState.clearSelectedTargetBall()
                        appState.currentMode = SelectionMode.SELECTING_CUE_BALL
                        listener?.onCueBallSelected(null)
                        listener?.onTargetBallSelected(null)
                        Log.d(TAG, "User tapped cue ball during aiming, resetting selection. Mode changed to ${appState.currentMode}")
                        modeChanged = true
                    } else if (closestBall.id == appState.selectedTargetBallId) {
                        // Tapped on current target ball, deselect it and go back to selecting target
                        appState.clearSelectedTargetBall()
                        appState.currentMode = SelectionMode.SELECTING_TARGET_BALL
                        listener?.onTargetBallSelected(null)
                        Log.d(TAG, "User deselected target ball. Mode changed to ${appState.currentMode}")
                        modeChanged = true
                    } else {
                        // Tapped a new ball, select as new target
                        appState.updateSelectedTargetBall(closestBall)
                        listener?.onTargetBallSelected(closestBall.id)
                        Log.d(TAG, "User selected new target ball: ${closestBall.id}.")
                    }
                } else {
                    // Tapped empty space, deselect target and go back to selecting target
                    if (appState.selectedTargetBallId != null) {
                        appState.clearSelectedTargetBall()
                        appState.currentMode = SelectionMode.SELECTING_TARGET_BALL
                        listener?.onTargetBallSelected(null)
                        Log.d(TAG, "User tapped empty space, deselected target ball. Mode changed to ${appState.currentMode}")
                        modeChanged = true
                    } else {
                        Log.d(TAG, "Tap ignored: No ball found, already in AIMING mode with no target selected.")
                    }
                }
            }
        }
        if (modeChanged) {
            listener?.onSelectionModeChanged(appState.currentMode)
        }
        listener?.onUserInteraction() // Always notify of user interaction
        invalidate() // Redraw after selection change
    }

    // --- Public API for controlling the view's state (now proxying to CameraManager for zoom) ---

    /**
     * Sets the camera zoom factor. This will be clamped by camera capabilities.
     * @param factor The desired zoom factor (e.g., 1.0f for no optical zoom).
     */
    fun setZoomFactor(factor: Float) {
        setZoomFactorInternal(factor, false)
    }

    /**
     * Internal method to set the camera zoom factor, with a flag for user-initiated changes.
     */
    private fun setZoomFactorInternal(factor: Float, isUserInitiatedInView: Boolean) {
        if (!areComponentsInitialized || !appState.isInitialized) return

        // Update AppState's zoomFactor (which now represents camera zoom ratio)
        if (appState.updateZoomFactor(factor)) {
            // Apply the new zoom ratio to the CameraX camera
            if (::cameraManagerRef.isInitialized) {
                cameraManagerRef.setCameraZoomRatio(appState.zoomFactor)
            }
            if (isUserInitiatedInView) {
                listener?.onZoomChanged(appState.zoomFactor)
                listener?.onUserInteraction()
            } else {
                listener?.onZoomChanged(appState.zoomFactor)
            }
            invalidate() // Request a redraw
        }
    }

    /**
     * Gets the current camera zoom factor.
     * @return The current camera zoom factor.
     */
    fun getZoomFactor(): Float = if (appState.isInitialized) appState.zoomFactor else 1.0f

    /**
     * Gets the minimum supported camera zoom factor.
     */
    fun getMinCameraZoomFactor(): Float = if (appState.isInitialized) appState.minCameraZoomRatio else 1.0f

    /**
     * Gets the maximum supported camera zoom factor.
     */
    fun getMaxCameraZoomFactor(): Float = if (appState.isInitialized) appState.maxCameraZoomRatio else 1.0f


    /**
     * Sets the rotation angle of the protractor overlay.
     * @param angle The desired rotation angle in degrees.
     */
    fun setProtractorRotationAngle(angle: Float) {
        setProtractorRotationAngleInternal(angle, false)
    }

    /**
     * Internal method to set the rotation angle, with a flag for user-initiated changes.
     */
    private fun setProtractorRotationAngleInternal(angle: Float, isUserInitiatedInView: Boolean) {
        if (!areComponentsInitialized || !appState.isInitialized) return

        if (appState.updateProtractorRotationAngle(angle)) {
            if (isUserInitiatedInView) {
                listener?.onRotationChanged(appState.protractorRotationAngle)
                listener?.onUserInteraction()
            } else {
                listener?.onRotationChanged(appState.protractorRotationAngle)
            }
            invalidate()
        }
    }

    /**
     * Gets the current rotation angle of the protractor overlay.
     * @return The current rotation angle in degrees.
     */
    fun getProtractorRotationAngle(): Float = if (appState.isInitialized) appState.protractorRotationAngle else config.DEFAULT_ROTATION_ANGLE

    /**
     * Sets the device pitch angle. This influences the 3D projection of the protractor.
     * @param rawPitchAngle The raw pitch angle from the device sensor.
     */
    fun setDevicePitchAngle(rawPitchAngle: Float) {
        if (!areComponentsInitialized || !appState.isInitialized) return

        if (appState.updateDevicePitchAngle(rawPitchAngle)) {
            invalidate() // Request redraw if pitch changed significantly
        }
    }

    /**
     * Gets the current effective pitch angle.
     * @return The current pitch angle in degrees.
     */
    fun getCurrentPitchAngle(): Float = if (appState.isInitialized) appState.currentPitchAngle else 0.0f

    /**
     * Gets the current center point of the protractor's target circle in view coordinates.
     * @return A PointF representing the target circle's center.
     */
    fun getPlaneTargetCenter(): PointF = if (appState.isInitialized) PointF(appState.targetCircleCenter.x, appState.targetCircleCenter.y) else PointF()

    /**
     * Resets all user interactions (zoom, rotation) and ball selections to defaults.
     */
    fun resetInteractionsToDefaults() {
        if (!areComponentsInitialized || !appState.isInitialized) {
            // If not initialized, try to initialize it now (e.g., if reset is pressed very early)
            if (width > 0 && height > 0 && ::cameraManagerRef.isInitialized) {
                initializeComponents(cameraManagerRef) // Re-initialize with known cameraManagerRef
                appState.initialize(width, height)
            } else {
                return // Cannot reset if not sized and CameraManager not ready
            }
        }

        appState.resetInteractions() // Reset AppState's interaction properties
        appPaints.resetDynamicPaintProperties() // Reset any dynamic paint states (e.g., error colors, glows)

        // Ensure camera zoom is reset to default (1.0f)
        if (::cameraManagerRef.isInitialized) {
            cameraManagerRef.setCameraZoomRatio(1.0f)
        }

        // Notify listeners of the reset state
        listener?.onZoomChanged(appState.zoomFactor)
        listener?.onRotationChanged(appState.protractorRotationAngle)
        listener?.onCueBallSelected(appState.selectedCueBallId)
        listener?.onTargetBallSelected(appState.selectedTargetBallId) // Notify about deselection
        listener?.onSelectionModeChanged(appState.currentMode)
        listener?.onUserInteraction()
        invalidate()
    }

    /**
     * Toggles the visibility of helper text labels on the overlay.
     */
    fun toggleHelperTextVisibility() {
        if (!areComponentsInitialized || !appState.isInitialized) return

        appState.toggleHelperTextVisibility()
        listener?.onUserInteraction() // Mark as user interaction for UI updates
        invalidate()
    }

    /**
     * Gets the current visibility state of helper texts.
     * @return True if helper texts are visible, false otherwise.
     */
    fun getAreHelperTextsVisible(): Boolean = if (appState.isInitialized) appState.areHelperTextsVisible else true

    /**
     * Gets the current selection mode.
     */
    fun getSelectionMode(): SelectionMode = if (appState.isInitialized) appState.currentMode else SelectionMode.SELECTING_CUE_BALL
}
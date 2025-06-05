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
import com.hereliesaz.cuedetat.drawing.DrawingCoordinator
import com.hereliesaz.cuedetat.view.gesture.GestureHandler
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball // Import the Ball data class
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
        fun onTargetBallSelected(ballId: String?) // New: Notify when a target ball is selected or deselected
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

    // Flag to ensure components are initialized only once after size is known
    private var areComponentsInitialized = false

    /**
     * Initializes core drawing and interaction components.
     * This is called once the view dimensions are available.
     */
    private fun initializeDrawingComponents() {
        if (areComponentsInitialized || width == 0 || height == 0) return // Already initialized or no dimensions

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
        // Ensure components are ready, especially if this is called before onSizeChanged
        if (width > 0 && height > 0 && !areComponentsInitialized) {
            initializeDrawingComponents()
            if (!appState.isInitialized) { // Initialize AppState if not already (e.g., first launch)
                appState.initialize(width, height)
            }
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
            initializeDrawingComponents() // Initialize or re-initialize if components depend on size
            appState.initialize(w, h)    // Initialize or re-initialize AppState with new dimensions
            invalidate() // Request a redraw
        }
    }

    /**
     * The main drawing dispatch method. Calls the DrawingCoordinator to render elements.
     */
    override fun onDraw(canvas: Canvas) { // android.graphics.Canvas
        super.onDraw(canvas)
        // Guard against drawing before components are ready
        if (!areComponentsInitialized) {
            if (width > 0 && height > 0) {
                initializeDrawingComponents()
                if(!appState.isInitialized) appState.initialize(width, height)
            } else {
                return // Not ready to draw
            }
        }
        // Ensure AppState is initialized before drawingCoordinator uses it
        if (!appState.isInitialized) return

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
        // Then pass to custom GestureHandler for pan/zoom
        val panZoomHandled = gestureHandler.onTouchEvent(event)

        // Consume event if either handler processed it, or fall back to super
        return gestureHandled || panZoomHandled || super.onTouchEvent(event)
    }

    // --- New methods for ball tracking integration ---

    /**
     * Receives a list of detected balls and camera frame dimensions from the CameraManager.
     * Updates AppState and attempts to select a target ball if none is selected.
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

        // Logic to automatically select a target ball if none is selected,
        // or if the previously selected ball is no longer detected.
        val selectedBallId = appState.selectedTargetBallId
        val currentSelectedBallStillDetected = balls.find { it.id == selectedBallId }

        if (selectedBallId == null || currentSelectedBallStillDetected == null) {
            val newSelectedBall = balls.firstOrNull() // Automatically select the first detected ball
            if (newSelectedBall != null && appState.selectedTargetBallId != newSelectedBall.id) {
                appState.selectedTargetBallId = newSelectedBall.id
                Log.d(TAG, "Auto-selected target ball: ${newSelectedBall.id}")
                listener?.onTargetBallSelected(newSelectedBall.id)
            } else if (newSelectedBall == null && appState.selectedTargetBallId != null) {
                // No balls detected, deselect current if any
                appState.selectedTargetBallId = null
                Log.d(TAG, "No balls detected, target deselected.")
                listener?.onTargetBallSelected(null)
            }
        }

        // Update the AppState's target ball properties based on the selected/tracked ball.
        updateTargetBallPositionFromTracking()
        invalidate() // Request a redraw to show updated positions
    }

    /**
     * Handles a single tap event on the overlay.
     * Determines if a tap was on a detected ball and selects it as the target.
     *
     * @param tapX The X-coordinate of the tap in MainOverlayView pixels.
     * @param tapY The Y-coordinate of the tap in MainOverlayView pixels.
     */
    private fun handleSingleTap(tapX: Float, tapY: Float) {
        var closestBall: Ball? = null
        var minDistance = Float.MAX_VALUE

        // Calculate scaling factors from camera frame to MainOverlayView pixels
        val screenScaleX = width.toFloat() / appState.frameWidth
        val screenScaleY = height.toFloat() / appState.frameHeight

        for (ball in appState.trackedBalls) {
            // ML Kit BallDetector already provides scaled coordinates if it handles it.
            // But if BallDetector gives coordinates from its original frame, we scale here.
            // Assuming Ball.x, Ball.y, Ball.radius are already scaled to the MainOverlayView dimensions.
            val mappedBallX = ball.x // If ball.x is already scaled, use directly
            val mappedBallY = ball.y // If ball.y is already scaled, use directly
            val mappedBallRadius = ball.radius // If ball.radius is already scaled, use directly

            // Check if the tap is within the ball's projected screen area (with a small tolerance)
            val dist = hypot(tapX - mappedBallX, tapY - mappedBallY)
            if (dist <= mappedBallRadius + 20f) { // Add 20 pixels tolerance for easier tapping
                if (dist < minDistance) {
                    minDistance = dist
                    closestBall = ball
                }
            }
        }

        // Update selected target ball based on tap result
        if (closestBall != null && appState.selectedTargetBallId != closestBall.id) {
            appState.selectedTargetBallId = closestBall.id
            updateTargetBallPositionFromTracking() // Immediately update the protractor's target
            listener?.onTargetBallSelected(closestBall.id) // Notify listener
            invalidate() // Request redraw
            Log.d(TAG, "User selected target ball: ${closestBall.id}")
        } else if (closestBall == null && appState.selectedTargetBallId != null) {
            // If tapped outside any ball, and a ball was previously selected, deselect it.
            appState.clearTrackedTargetBallData() // Clear tracked data, resets target to center
            listener?.onTargetBallSelected(null) // Notify listener
            invalidate() // Request redraw
            Log.d(TAG, "User deselected target ball.")
        }
    }

    /**
     * Updates the AppState's target ball position and logical radius
     * based on the currently selected tracked ball.
     */
    private fun updateTargetBallPositionFromTracking() {
        val selectedBall = appState.trackedBalls.find { it.id == appState.selectedTargetBallId }

        if (selectedBall != null) {
            // The BallDetector now provides coordinates and radius already scaled to the
            // MainOverlayView's expected frame dimensions (based on IMAGE_ANALYSIS_WIDTH/HEIGHT).
            // So, directly use selectedBall.x, .y, .radius.
            val newTargetX = selectedBall.x
            val newTargetY = selectedBall.y
            val newTargetRadius = selectedBall.radius

            // Update AppState with the scaled tracked data.
            // AppState will then re-calculate `currentLogicalRadius` and `cueCircleCenter`.
            appState.setTrackedTargetBallData(newTargetX, newTargetY, newTargetRadius)
        } else {
            // If no ball is selected or detected, reset the target ball data in AppState
            appState.clearTrackedTargetBallData()
        }
    }


    // --- Public API for controlling the view's state ---

    /**
     * Sets the zoom factor of the protractor overlay.
     * @param factor The desired zoom factor.
     */
    fun setZoomFactor(factor: Float) {
        setZoomFactorInternal(factor, false)
    }

    /**
     * Internal method to set the zoom factor, with a flag for user-initiated changes.
     */
    private fun setZoomFactorInternal(factor: Float, isUserInitiatedInView: Boolean) {
        if (width == 0 || height == 0) return // Not ready if dimensions are zero
        // Ensure components and AppState are initialized
        if (!areComponentsInitialized) initializeDrawingComponents()
        if (!appState.isInitialized) appState.initialize(width, height)

        if (appState.updateZoomFactor(factor)) { // Update AppState and check if state changed
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
     * Gets the current zoom factor of the protractor overlay.
     * @return The current zoom factor.
     */
    fun getZoomFactor(): Float = if (appState.isInitialized) appState.zoomFactor else config.DEFAULT_ZOOM_FACTOR

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
        if (width == 0 || height == 0) return
        if (!areComponentsInitialized) initializeDrawingComponents()
        if (!appState.isInitialized) appState.initialize(width, height)

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
        if (width == 0 || height == 0) return
        if (!areComponentsInitialized) initializeDrawingComponents()
        if (!appState.isInitialized) appState.initialize(width, height)

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
     * Resets all user interactions (zoom, rotation) and target ball selection to defaults.
     */
    fun resetInteractionsToDefaults() {
        if (width == 0 || height == 0) { // Cannot reset if not sized
            return
        }
        if (!areComponentsInitialized) initializeDrawingComponents()
        if (!appState.isInitialized) appState.initialize(width, height)

        appState.resetInteractions() // Reset AppState's interaction properties
        appPaints.resetDynamicPaintProperties() // Reset any dynamic paint states (e.g., error colors, glows)

        // Notify listeners of the reset state
        listener?.onZoomChanged(appState.zoomFactor)
        listener?.onRotationChanged(appState.protractorRotationAngle)
        listener?.onTargetBallSelected(appState.selectedTargetBallId) // Notify about deselection
        listener?.onUserInteraction()
        invalidate()
    }

    /**
     * Toggles the visibility of helper text labels on the overlay.
     */
    fun toggleHelperTextVisibility() {
        if (!appState.isInitialized && width > 0 && height > 0) {
            if (!areComponentsInitialized) initializeDrawingComponents()
            appState.initialize(width, height)
        }
        if (!appState.isInitialized) return

        appState.toggleHelperTextVisibility()
        listener?.onUserInteraction() // Mark as user interaction for UI updates
        invalidate()
    }

    /**
     * Gets the current visibility state of helper texts.
     * @return True if helper texts are visible, false otherwise.
     */
    fun getAreHelperTextsVisible(): Boolean = if (appState.isInitialized) appState.areHelperTextsVisible else true
}
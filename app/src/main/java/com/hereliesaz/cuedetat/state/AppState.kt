// app/src/main/java/com/hereliesaz/cuedetat/state/AppState.kt
package com.hereliesaz.cuedetat.state

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball // Import Ball class
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AppState(val config: AppConfig) { // Changed from 'private val' to 'val'

    // --- View Dimensions & Initialization ---
    var viewWidth: Int = 0
        private set
    var viewHeight: Int = 0
        private set
    var isInitialized: Boolean = false
        private set

    // --- Ball Tracking Data (Raw from ML Kit, scaled to MainOverlayView) ---
    var trackedBalls: List<Ball> = emptyList() // All detected balls from the camera frame
    var frameWidth: Int = 0 // Raw pixel dimensions of the camera frame used for tracking
    var frameHeight: Int = 0

    // --- Selected Ball Data (Screen-space, in MainOverlayView pixels) ---
    var selectedCueBallId: String? = null
        private set
    var selectedCueBallScreenCenter: PointF? = null
        private set
    var selectedCueBallScreenRadius: Float = 0f
        private set

    var selectedTargetBallId: String? = null
        private set
    var selectedTargetBallScreenCenter: PointF? = null
        private set
    var selectedTargetBallScreenRadius: Float = 0f
        private set

    // --- Protractor Plane Elements (Logical, derived from selected balls) ---
    // targetCircleCenter and cueCircleCenter are the *logical centers* of the protractor's balls
    // which represent the ghost balls on the 2D plane.
    var targetCircleCenter: PointF = PointF()
        private set
    var cueCircleCenter: PointF = PointF()
        private set
    // currentLogicalRadius now represents the effective radius of the target ball in logical units.
    // Its size directly reflects the detected pixel radius, as CameraX zoom controls the overall scale.
    var currentLogicalRadius: Float = 1f
        private set


    // --- User Interaction State ---
    // This zoomFactor now directly controls CameraX's zoom ratio (1.0f is no optical zoom).
    var zoomFactor: Float = 1.0f // Default camera zoom is 1.0x (no optical zoom)
        private set
    // CameraX capabilities
    var minCameraZoomRatio: Float = 1.0f
        private set
    var maxCameraZoomRatio: Float = 1.0f
        private set

    var protractorRotationAngle: Float = config.DEFAULT_ROTATION_ANGLE
        private set
    var currentMode: SelectionMode = SelectionMode.SELECTING_CUE_BALL // New: Initial selection mode

    // --- Device Orientation State ---
    var currentPitchAngle: Float = 0.0f
        private set
    var smoothedPitchAngle: Float = 0.0f
        private set

    // --- Graphics Matrices & Camera for 3D effect ---
    val graphicsCamera: Camera = Camera()
    val pitchMatrix: Matrix = Matrix()
    val inversePitchMatrix: Matrix = Matrix()

    // --- UI State ---
    var areHelperTextsVisible: Boolean = true

    enum class SelectionMode {
        SELECTING_CUE_BALL,
        SELECTING_TARGET_BALL,
        AIMING
    }

    /**
     * Initializes or re-initializes the AppState with current view dimensions.
     * Sets default positions and radius if no ball is tracked or selected.
     */
    fun initialize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        viewWidth = width
        viewHeight = height

        // Update protractor's logical base based on selected target ball if available,
        // otherwise default to screen center.
        if (selectedTargetBallScreenCenter != null && selectedTargetBallScreenRadius > 0f) {
            targetCircleCenter.set(selectedTargetBallScreenCenter!!.x, selectedTargetBallScreenCenter!!.y)
            // currentLogicalRadius is now just the detected screen radius, as camera zoom handles scale.
            currentLogicalRadius = selectedTargetBallScreenRadius
        } else {
            targetCircleCenter.set(viewWidth / 2f, viewHeight / 2f) // Default to screen center
            // Default logical radius is a percentage of view size (if no ball is selected).
            currentLogicalRadius = min(viewWidth, viewHeight) * 0.15f
        }

        // Ensure a minimum radius to prevent division by zero or infinitesimally small elements
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f

        smoothedPitchAngle = currentPitchAngle // Initialize smoothed pitch
        isInitialized = true
        updateCueBallPlanePosition() // Position logical cue ball relative to the (potentially tracked) target
    }

    /**
     * Updates the camera's zoom capabilities.
     */
    fun updateCameraZoomCapabilities(minZoom: Float, maxZoom: Float) {
        minCameraZoomRatio = minZoom
        maxCameraZoomRatio = maxZoom
        // Ensure current zoom is within new bounds if capabilities change mid-app
        zoomFactor = zoomFactor.coerceIn(minCameraZoomRatio, maxCameraZoomRatio)
    }

    /**
     * Updates the selected cue ball's tracked data.
     * @param ball The selected cue ball.
     */
    fun updateSelectedCueBall(ball: Ball) {
        selectedCueBallId = ball.id
        selectedCueBallScreenCenter = PointF(ball.x, ball.y)
        selectedCueBallScreenRadius = ball.radius
    }

    /**
     * Clears the selected cue ball data.
     */
    fun clearSelectedCueBall() {
        selectedCueBallId = null
        selectedCueBallScreenCenter = null
        selectedCueBallScreenRadius = 0f
    }

    /**
     * Updates the selected target ball's tracked data and repositions the protractor.
     * @param ball The selected target ball.
     */
    fun updateSelectedTargetBall(ball: Ball) {
        selectedTargetBallId = ball.id
        selectedTargetBallScreenCenter = PointF(ball.x, ball.y)
        selectedTargetBallScreenRadius = ball.radius

        // Re-initialize protractor base (logical target center and radius) to match the new target ball
        initialize(viewWidth, viewHeight)
    }

    /**
     * Clears the selected target ball data and resets protractor position to default.
     */
    fun clearSelectedTargetBall() {
        selectedTargetBallId = null
        selectedTargetBallScreenCenter = null
        selectedTargetBallScreenRadius = 0f
        initialize(viewWidth, viewHeight) // Re-initialize to set defaults if no tracked ball
    }


    /**
     * Updates the camera zoom factor. This value is clamped by camera capabilities.
     * @param newFactor The new desired camera zoom factor (e.g., 1.0f for no zoom, 2.0f for 2x zoom).
     * @return True if the zoom factor changed significantly, false otherwise.
     */
    fun updateZoomFactor(newFactor: Float): Boolean {
        val coercedFactor = newFactor.coerceIn(minCameraZoomRatio, maxCameraZoomRatio)
        // Only update if there's a significant change to prevent unnecessary CameraX calls
        if (abs(zoomFactor - coercedFactor) < 0.001f) return false

        zoomFactor = coercedFactor
        // currentLogicalRadius is NOT updated here, it's updated in initialize() or setTrackedTargetBallData()
        // when a ball is detected/selected, using its *current detected pixel radius*.
        // The camera zoom *already* affects the detected pixel radius.
        return true
    }

    /**
     * Updates the rotation angle of the protractor.
     *
     * @param newAngle The new desired rotation angle in degrees.
     * @return True if the rotation angle changed significantly, false otherwise.
     */
    fun updateProtractorRotationAngle(newAngle: Float): Boolean {
        var normalizedAngle = newAngle % 360f
        if (normalizedAngle < 0) normalizedAngle += 360f // Normalize angle to 0-360 degrees

        // Only update if there's a significant change
        if (abs(protractorRotationAngle - normalizedAngle) < 0.01f) return false

        protractorRotationAngle = normalizedAngle
        updateCueBallPlanePosition() // Recalculate logical cue ball position as rotation changed
        return true
    }

    /**
     * Updates the device's pitch angle, applying a smoothing factor.
     *
     * @param newRawPitch The raw pitch angle from the sensor.
     * @return True if the smoothed pitch angle changed significantly, false otherwise.
     */
    fun updateDevicePitchAngle(newRawPitch: Float): Boolean {
        val newSmoothedPitch = (config.PITCH_SMOOTHING_FACTOR * newRawPitch) +
                ((1.0f - config.PITCH_SMOOTHING_FACTOR) * smoothedPitchAngle)

        // Only update if there's a significant change to prevent excessive redraws
        if (abs(currentPitchAngle - newSmoothedPitch) > 0.05f) {
            currentPitchAngle = newSmoothedPitch
            smoothedPitchAngle = newSmoothedPitch
            return true
        }
        smoothedPitchAngle = newSmoothedPitch // Always update smoothed value for next iteration
        return false
    }

    /**
     * Recalculates the position of the logical cue ball (ghost ball) based on the current logical target ball
     * position, logical radius, and protractor rotation.
     */
    private fun updateCueBallPlanePosition() {
        if (!isInitialized || currentLogicalRadius <= 0.01f) return

        val angleRad = Math.toRadians(protractorRotationAngle.toDouble())
        val distanceFromTarget = 2 * currentLogicalRadius // Logical cue ball is positioned two radii away from the target

        // Calculate logical cue ball's position relative to the logical target ball's center,
        // rotated by the protractor's current angle.
        cueCircleCenter.x = targetCircleCenter.x - (distanceFromTarget * sin(angleRad)).toFloat()
        cueCircleCenter.y = targetCircleCenter.y + (distanceFromTarget * cos(angleRad)).toFloat()
    }

    /**
     * Resets user-controlled interaction states (zoom, rotation, and target ball selection)
     * to their default values defined in `AppConfig`.
     */
    fun resetInteractions() {
        clearSelectedCueBall()
        clearSelectedTargetBall() // This calls initialize internally to reset targetCircleCenter etc.
        currentMode = SelectionMode.SELECTING_CUE_BALL // Reset to initial mode
        // Reset camera zoom to default (1.0f)
        updateZoomFactor(1.0f) // This will trigger CameraX zoom reset
        updateProtractorRotationAngle(config.DEFAULT_ROTATION_ANGLE)
    }

    /**
     * Toggles the visibility of all helper text labels on the overlay.
     */
    fun toggleHelperTextVisibility() {
        areHelperTextsVisible = !areHelperTextsVisible
    }
}
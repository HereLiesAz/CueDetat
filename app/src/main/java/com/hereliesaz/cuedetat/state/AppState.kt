package com.hereliesaz.cuedetat.state

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AppState(val config: AppConfig) {

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
    var selectedCueBall: Ball? = null
        private set
    var selectedTargetBall: Ball? = null
        private set

    // --- Protractor Plane Elements (Logical, derived from selected balls) ---
    var targetCircleCenter: PointF = PointF()
        private set
    var cueCircleCenter: PointF = PointF()
        private set
    var logicalBallRadius: Float = 1f // Renamed from currentLogicalRadius
        private set


    // --- User Interaction State ---
    var zoomFactor: Float = config.DEFAULT_ZOOM_FACTOR
        private set
    var minCameraZoomRatio: Float = 1.0f
        private set
    var maxCameraZoomRatio: Float = 1.0f
        private set

    var protractorRotationAngle: Float = config.DEFAULT_ROTATION_ANGLE
        private set
    var currentMode: SelectionMode = SelectionMode.SELECTING_CUE_BALL

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
    var areHelperTextsVisible: Boolean = false // Changed default to false
        private set

    enum class SelectionMode {
        SELECTING_CUE_BALL,
        SELECTING_TARGET_BALL,
        AIMING
    }

    /**
     * Initializes or re-initializes the AppState with current view dimensions.
     * Ensures default manual ball placement if no balls are currently selected.
     * Sets `logicalBallRadius` based on the selected target ball or default.
     */
    fun initialize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        viewWidth = width
        viewHeight = height

        // Initialize logicalBallRadius first, as it dictates the size of manually placed balls
        // and also the base size for the protractor plane.
        if (selectedTargetBall != null) {
            logicalBallRadius = selectedTargetBall!!.radius
        } else if (selectedCueBall != null) { // If only cue ball is selected, use its radius
            logicalBallRadius = selectedCueBall!!.radius
        } else {
            logicalBallRadius = config.DEFAULT_MANUAL_BALL_RADIUS
        }

        // Initialize target ball if not already selected.
        // If it's a new manual placement, its radius will use the newly set logicalBallRadius.
        if (selectedTargetBall == null) {
            setManualTargetBall(viewWidth / 2f, viewHeight * 0.25f) // Default position
        } else {
            // Update logical center based on existing selectedTargetBall
            targetCircleCenter.set(selectedTargetBall!!.x, selectedTargetBall!!.y)
            // Ensure its radius is consistent with logicalBallRadius if it changed
            if (selectedTargetBall!!.radius != logicalBallRadius) {
                selectedTargetBall = selectedTargetBall!!.copy(radius = logicalBallRadius)
            }
        }

        // Initialize cue ball if not already selected.
        if (selectedCueBall == null) {
            setManualCueBall(viewWidth / 2f, viewHeight * 0.75f) // Default position
        } else {
            // Ensure its radius is consistent with logicalBallRadius if it changed
            if (selectedCueBall!!.radius != logicalBallRadius) {
                selectedCueBall = selectedCueBall!!.copy(radius = logicalBallRadius)
            }
        }

        // Ensure a minimum radius to prevent division by zero or infinitesimally small elements
        if (logicalBallRadius <= 0.01f) logicalBallRadius = 0.01f

        smoothedPitchAngle = currentPitchAngle
        isInitialized = true
        updateCueBallPlanePosition()
    }

    /**
     * Updates the camera's zoom capabilities.
     */
    fun updateCameraZoomCapabilities(minZoom: Float, maxZoom: Float) {
        minCameraZoomRatio = minZoom
        maxCameraZoomRatio = maxZoom
        zoomFactor = zoomFactor.coerceIn(minZoom, maxZoom)
    }

    /**
     * Updates the selected cue ball's tracked data.
     * Updates `logicalBallRadius` to match the new detected ball's size.
     * @param ball The selected cue ball.
     */
    fun updateSelectedCueBall(ball: Ball) {
        selectedCueBall = ball
        // Update logicalBallRadius to match the new detected ball's radius
        logicalBallRadius = ball.radius
        // Re-initialize to ensure all logical elements resize based on this new radius
        initialize(viewWidth, viewHeight)
    }

    /**
     * Clears the selected cue ball data.
     * Resets `logicalBallRadius` to default if no other ball is selected.
     */
    fun clearSelectedCueBall() {
        selectedCueBall = null
        // If target ball is also cleared or not selected, revert to default radius
        if (selectedTargetBall == null) {
            logicalBallRadius = config.DEFAULT_MANUAL_BALL_RADIUS
        }
        initialize(viewWidth, viewHeight) // Re-initialize to set defaults/recalculate
    }

    /**
     * Sets the cue ball data based on a manual tap location.
     * Uses `logicalBallRadius` for its size.
     */
    fun setManualCueBall(x: Float, y: Float) {
        selectedCueBall = Ball("MANUAL_CUE_BALL_${System.currentTimeMillis()}", x, y, logicalBallRadius)
        initialize(viewWidth, viewHeight) // Re-initialize to ensure consistency
    }

    /**
     * Updates the selected target ball's tracked data and repositions the protractor.
     * Updates `logicalBallRadius` to match the new detected ball's size.
     * @param ball The selected target ball.
     */
    fun updateSelectedTargetBall(ball: Ball) {
        selectedTargetBall = ball
        // Update logicalBallRadius to match the new detected ball's radius
        logicalBallRadius = ball.radius
        initialize(viewWidth, viewHeight) // Re-initialize to update logical target center and radius
    }

    /**
     * Clears the selected target ball data and resets protractor position to default.
     * Resets `logicalBallRadius` to default if no other ball is selected.
     */
    fun clearSelectedTargetBall() {
        selectedTargetBall = null
        // If cue ball is also cleared or not selected, revert to default radius
        if (selectedCueBall == null) {
            logicalBallRadius = config.DEFAULT_MANUAL_BALL_RADIUS
        }
        initialize(viewWidth, viewHeight) // Re-initialize to set defaults if no tracked ball
    }

    /**
     * Sets the target ball data based on a manual tap location.
     * Uses `logicalBallRadius` for its size.
     */
    fun setManualTargetBall(x: Float, y: Float) {
        selectedTargetBall = Ball("MANUAL_TARGET_BALL_${System.currentTimeMillis()}", x, y, logicalBallRadius)
        initialize(viewWidth, viewHeight) // Re-initialize to update logical target center and radius
    }

    /**
     * Updates the camera zoom factor.
     */
    fun updateZoomFactor(newFactor: Float): Boolean {
        val coercedFactor = newFactor.coerceIn(minCameraZoomRatio, maxCameraZoomRatio)
        if (abs(zoomFactor - coercedFactor) < 0.001f) return false
        zoomFactor = coercedFactor
        // Invalidate to trigger redraw with new zoom, which affects logicalBallRadius's visual size
        return true
    }

    /**
     * Updates the rotation angle of the protractor.
     */
    fun updateProtractorRotationAngle(newAngle: Float): Boolean {
        var normalizedAngle = newAngle % 360f
        if (normalizedAngle < 0) normalizedAngle += 360f
        if (abs(protractorRotationAngle - normalizedAngle) < 0.01f) return false
        protractorRotationAngle = normalizedAngle
        updateCueBallPlanePosition()
        return true
    }

    /**
     * Updates the device's pitch angle, applying a smoothing factor.
     */
    fun updateDevicePitchAngle(newRawPitch: Float): Boolean {
        val newSmoothedPitch = (config.PITCH_SMOOTHING_FACTOR * newRawPitch) +
                ((1.0f - config.PITCH_SMOOTHING_FACTOR) * smoothedPitchAngle)

        if (abs(currentPitchAngle - newSmoothedPitch) > 0.05f) {
            currentPitchAngle = newSmoothedPitch
            smoothedPitchAngle = newSmoothedPitch
            return true
        }
        smoothedPitchAngle = newSmoothedPitch
        return false
    }

    /**
     * Recalculates the position of the logical cue ball (ghost ball) based on the current logical target ball
     * position, logical radius, and protractor rotation.
     */
    private fun updateCueBallPlanePosition() {
        if (!isInitialized || logicalBallRadius <= 0.01f) return

        val angleRad = Math.toRadians(protractorRotationAngle.toDouble())
        val distanceFromTarget = 2 * logicalBallRadius

        cueCircleCenter.x = targetCircleCenter.x - (distanceFromTarget * sin(angleRad)).toFloat()
        cueCircleCenter.y = targetCircleCenter.y + (distanceFromTarget * cos(angleRad)).toFloat()
    }

    /**
     * Resets user-controlled interaction states (zoom, rotation, and ball selection)
     * to their default values defined in `AppConfig`.
     */
    fun resetInteractions() {
        selectedCueBall = null
        selectedTargetBall = null
        // Reset logicalBallRadius before re-initializing
        logicalBallRadius = config.DEFAULT_MANUAL_BALL_RADIUS
        initialize(viewWidth, viewHeight) // Re-initialize to set default manual balls

        currentMode = SelectionMode.SELECTING_CUE_BALL
        updateZoomFactor(config.DEFAULT_ZOOM_FACTOR)
        updateProtractorRotationAngle(config.DEFAULT_ROTATION_ANGLE)
    }

    /**
     * Toggles the visibility of all helper text labels on the overlay.
     */
    fun toggleHelperTextVisibility() {
        areHelperTextsVisible = !areHelperTextsVisible
    }
}
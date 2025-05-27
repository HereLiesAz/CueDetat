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

    // --- Protractor Plane Elements (now dynamic based on tracking) ---
    // targetCircleCenter now represents the dynamically tracked position of the target ball
    var targetCircleCenter: PointF = PointF()
        private set
    var cueCircleCenter: PointF = PointF()
        private set
    // currentLogicalRadius now represents the effective radius of the target ball in logical units,
    // which is derived from the tracked ball's pixel radius and the zoom factor.
    var currentLogicalRadius: Float = 1f
        private set

    // --- Ball Tracking Data ---
    var trackedBalls: List<Ball> = emptyList() // All detected balls from the camera frame
    var selectedTargetBallId: String? = null // ID of the currently selected target ball

    // Raw pixel dimensions of the camera frame used for tracking
    var frameWidth: Int = 0
    var frameHeight: Int = 0

    // Storing the raw tracked target ball's properties for consistent radius calculation
    // These are in the MainOverlayView's pixel space, not camera frame pixel space.
    private var rawTrackedTargetCenterX: Float = 0f
    private var rawTrackedTargetCenterY: Float = 0f
    private var rawTrackedTargetRadiusPx: Float = 0f


    // --- User Interaction State ---
    var zoomFactor: Float = config.DEFAULT_ZOOM_FACTOR
        private set
    var protractorRotationAngle: Float = config.DEFAULT_ROTATION_ANGLE
        private set

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

    /**
     * Initializes or re-initializes the AppState with current view dimensions.
     * Sets default positions and radius if no ball is tracked.
     */
    fun initialize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        viewWidth = width
        viewHeight = height

        // If a target ball is currently selected/tracked, use its latest mapped position and radius.
        // Otherwise, set a default center and logical radius for the protractor.
        if (selectedTargetBallId != null && rawTrackedTargetRadiusPx > 0f) {
            targetCircleCenter.set(rawTrackedTargetCenterX, rawTrackedTargetCenterY)
            currentLogicalRadius = rawTrackedTargetRadiusPx * zoomFactor // Apply zoom to tracked radius
        } else {
            // Default position if no ball is tracked (e.g., screen center)
            targetCircleCenter.set(viewWidth / 2f, viewHeight / 2f)
            // Default logical radius based on a percentage of the view size, scaled by zoom factor
            currentLogicalRadius = (min(viewWidth, viewHeight) * 0.15f) * zoomFactor
        }

        // Ensure a minimum radius to prevent division by zero or infinitesimally small elements
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f

        smoothedPitchAngle = currentPitchAngle // Initialize smoothed pitch
        isInitialized = true
        updateCueBallPlanePosition() // Position cue ball relative to the (potentially tracked) target
    }

    /**
     * Updates the raw tracked target ball's data (position and radius) in MainOverlayView pixel space.
     * This will cause the `targetCircleCenter` to update and `currentLogicalRadius` to be recalculated.
     *
     * @param x The tracked ball's center X-coordinate in MainOverlayView pixel space.
     * @param y The tracked ball's center Y-coordinate in MainOverlayView pixel space.
     * @param radiusPx The tracked ball's radius in MainOverlayView pixel space.
     */
    fun setTrackedTargetBallData(x: Float, y: Float, radiusPx: Float) {
        rawTrackedTargetCenterX = x
        rawTrackedTargetCenterY = y
        rawTrackedTargetRadiusPx = radiusPx

        targetCircleCenter.set(x, y) // Update target center immediately to follow the tracked ball

        // Recalculate effective logical radius based on new raw radius and current zoom
        currentLogicalRadius = rawTrackedTargetRadiusPx * zoomFactor
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f // Minimum size
        updateCueBallPlanePosition() // Re-position cue ball as target/radius changed
    }

    /**
     * Clears the currently selected target ball and its tracked data,
     * resetting the protractor's target to a default position and size.
     */
    fun clearTrackedTargetBallData() {
        rawTrackedTargetCenterX = 0f
        rawTrackedTargetCenterY = 0f
        rawTrackedTargetRadiusPx = 0f
        selectedTargetBallId = null
        // Re-initialize to set defaults if no tracked ball
        initialize(viewWidth, viewHeight) // Re-initialize with current view dimensions to reset target to center
    }


    /**
     * Updates the zoom factor of the protractor and recalculates `currentLogicalRadius`.
     *
     * @param newFactor The new desired zoom factor.
     * @return True if the zoom factor changed significantly, false otherwise.
     */
    fun updateZoomFactor(newFactor: Float): Boolean {
        val coercedFactor = newFactor.coerceIn(config.MIN_ZOOM_FACTOR, config.MAX_ZOOM_FACTOR)
        // Only update if there's a significant change to prevent unnecessary redraws
        if (abs(zoomFactor - coercedFactor) < 0.001f && currentLogicalRadius > 0.01f) return false

        zoomFactor = coercedFactor
        // Recalculate `currentLogicalRadius` based on the raw tracked radius (if available)
        // and the new zoom factor.
        if (rawTrackedTargetRadiusPx > 0f) {
            currentLogicalRadius = rawTrackedTargetRadiusPx * zoomFactor
        } else {
            // Fallback if no ball is tracked, use a default radius based on view size and zoom
            currentLogicalRadius = (min(viewWidth, viewHeight) * 0.15f) * zoomFactor
        }
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f // Ensure minimum size

        updateCueBallPlanePosition() // Recalculate cue ball position as logical radius changed
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
        updateCueBallPlanePosition() // Recalculate cue ball position as rotation changed
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
     * Recalculates the position of the cue ball based on the current target ball
     * position, logical radius, and protractor rotation.
     */
    private fun updateCueBallPlanePosition() {
        if (!isInitialized || currentLogicalRadius <= 0.01f) return

        val angleRad = Math.toRadians(protractorRotationAngle.toDouble())
        val distanceFromTarget = 2 * currentLogicalRadius // Cue ball is positioned two radii away from the target

        // Calculate cue ball's position relative to the target ball's center,
        // rotated by the protractor's current angle.
        cueCircleCenter.x = targetCircleCenter.x - (distanceFromTarget * sin(angleRad)).toFloat()
        cueCircleCenter.y = targetCircleCenter.y + (distanceFromTarget * cos(angleRad)).toFloat()
    }

    /**
     * Resets user-controlled interaction states (zoom, rotation, and target ball selection)
     * to their default values defined in `AppConfig`.
     */
    fun resetInteractions() {
        selectedTargetBallId = null // Clear selection on reset
        rawTrackedTargetRadiusPx = 0f // Clear raw tracked radius
        updateZoomFactor(config.DEFAULT_ZOOM_FACTOR) // This will also re-calculate currentLogicalRadius
        updateProtractorRotationAngle(config.DEFAULT_ROTATION_ANGLE)
    }

    /**
     * Toggles the visibility of all helper text labels on the overlay.
     */
    fun toggleHelperTextVisibility() {
        areHelperTextsVisible = !areHelperTextsVisible
    }
}
package com.hereliesaz.cuedetat.protractor

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Class constructor accepts the ProtractorConfig object instance
class ProtractorState(private val config: ProtractorConfig) {
    var targetCircleCenter: PointF = PointF()
        private set
    var cueCircleCenter: PointF = PointF()
        private set
    var baseCircleDiameter: Float = 0f
        private set
    var currentLogicalRadius: Float = 1f
        private set
    // Use config object directly for defaults
    var zoomFactor: Float = config.DEFAULT_ZOOM_FACTOR
        private set
    var protractorRotationAngle: Float = config.DEFAULT_ROTATION_ANGLE
        private set
    var currentPitchAngle: Float = 0.0f
        private set
    var smoothedPitchAngle: Float = 0.0f
        private set

    var isInitialized: Boolean = false
        private set
    var areTextLabelsVisible: Boolean = true

    val mGraphicsCamera: Camera = Camera()
    val mPitchMatrix: Matrix = Matrix()
    val mInversePitchMatrix: Matrix = Matrix()

    // Inside ProtractorState
    val mTextCamera: Camera = Camera() // New camera for text effects
    val mTextTransformMatrix: Matrix = Matrix() // New matrix for text

    fun initialize(viewWidth: Int, viewHeight: Int) {
        targetCircleCenter.set(viewWidth / 2f, viewHeight / 2f)
        baseCircleDiameter = min(viewWidth, viewHeight) * 0.30f
        currentLogicalRadius = (baseCircleDiameter / 2f) * zoomFactor // Use current zoomFactor
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f
        smoothedPitchAngle = currentPitchAngle
        isInitialized = true
        updateCueBallPosition()
    }

    fun updateZoomFactor(newFactor: Float): Boolean {
        // Use config object for MIN/MAX
        val coercedFactor = newFactor.coerceIn(config.MIN_ZOOM_FACTOR, config.MAX_ZOOM_FACTOR)
        if (abs(zoomFactor - coercedFactor) < 0.001f) return false
        zoomFactor = coercedFactor
        currentLogicalRadius = (baseCircleDiameter / 2f) * zoomFactor
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f
        updateCueBallPosition()
        return true
    }

    fun updateProtractorRotationAngle(newAngle: Float): Boolean {
        var normAng = newAngle % 360f
        if (normAng < 0) normAng += 360f
        if (abs(protractorRotationAngle - normAng) < 0.01f) return false
        protractorRotationAngle = normAng
        updateCueBallPosition()
        return true
    }

    fun updatePitchAngle(newAngle: Float): Boolean {
        val newPitch = newAngle.coerceIn(-85f, 90f)
        // Use config object for smoothing factor
        smoothedPitchAngle = (config.PITCH_SMOOTHING_FACTOR * newPitch) +
                ((1.0f - config.PITCH_SMOOTHING_FACTOR) * smoothedPitchAngle)

        if (abs(currentPitchAngle - smoothedPitchAngle) > 0.05f) {
            currentPitchAngle = smoothedPitchAngle
            return true
        }
        return false
    }

    fun reset() {
        // Use config object for defaults
        updateZoomFactor(config.DEFAULT_ZOOM_FACTOR)
        updateProtractorRotationAngle(config.DEFAULT_ROTATION_ANGLE)
        areTextLabelsVisible = true
    }

    fun toggleHelperTextVisibility() {
        areTextLabelsVisible = !areTextLabelsVisible
    }

    fun updateCueBallPosition() {
        if (!isInitialized || currentLogicalRadius <= 0) return
        val angleRad = Math.toRadians(protractorRotationAngle.toDouble())
        val distance = 2 * currentLogicalRadius
        cueCircleCenter.x = targetCircleCenter.x - (distance * sin(angleRad)).toFloat()
        cueCircleCenter.y = targetCircleCenter.y + (distance * cos(angleRad)).toFloat()
    }
}
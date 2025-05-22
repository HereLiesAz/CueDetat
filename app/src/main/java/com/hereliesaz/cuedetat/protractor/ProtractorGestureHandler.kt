package com.hereliesaz.cuedetat.protractor

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.hereliesaz.cuedetat.protractor.ProtractorOverlayView.*
import kotlin.math.abs

// Class constructor accepts the ProtractorConfig object instance
class ProtractorGestureHandler(
    context: Context,
    private val state: ProtractorState,
    private val config: ProtractorConfig, // Accept the object instance
    private val listener: ProtractorStateListener?,
    private val onZoomChangedByGesture: (Float) -> Unit,
    private val onRotationChangedByGesture: (Float) -> Unit
) {
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private enum class InteractionMode { NONE, PINCH_ZOOMING, PAN_TO_ROTATE }
    private var currentInteractionMode = InteractionMode.NONE
    private var isPinching = false

    val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            currentInteractionMode = InteractionMode.PINCH_ZOOMING
            isPinching = true
            listener?.onUserInteraction()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (currentInteractionMode != InteractionMode.PINCH_ZOOMING) return false

            val oldZoom = state.zoomFactor
            val newZoomUncoerced = state.zoomFactor * detector.scaleFactor
            // Use config object for MIN/MAX
            val newZoom = newZoomUncoerced.coerceIn(config.MIN_ZOOM_FACTOR, config.MAX_ZOOM_FACTOR)

            val significantChange = abs(oldZoom - newZoom) >= 0.001f
            val hittingBounds = newZoomUncoerced != newZoom
            val actualScaleFactorApplied = detector.scaleFactor != 1.0f

            if (significantChange || hittingBounds || actualScaleFactorApplied) {
                onZoomChangedByGesture(newZoomUncoerced)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (currentInteractionMode == InteractionMode.PINCH_ZOOMING) {
                currentInteractionMode = InteractionMode.NONE
            }
            isPinching = false
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!state.isInitialized) return false
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)

        if (isPinching && event.actionMasked != MotionEvent.ACTION_DOWN) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL){
            }
            return true
        }

        val touchX = event.x

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scaleGestureDetector.isInProgress) {
                    currentInteractionMode = InteractionMode.PAN_TO_ROTATE
                    listener?.onUserInteraction()
                }
                lastTouchX = touchX
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (currentInteractionMode == InteractionMode.PAN_TO_ROTATE && !isPinching) {
                    val dx = touchX - lastTouchX
                    // Use config object for sensitivity
                    val angleDelta = dx * config.PAN_ROTATE_SENSITIVITY
                    onRotationChangedByGesture(state.protractorRotationAngle + angleDelta)
                    lastTouchX = touchX
                    lastTouchY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasPanning = currentInteractionMode == InteractionMode.PAN_TO_ROTATE
                if (wasPanning) {
                    currentInteractionMode = InteractionMode.NONE
                }
                return wasPanning || scaleHandled
            }
        }
        return scaleHandled
    }
}
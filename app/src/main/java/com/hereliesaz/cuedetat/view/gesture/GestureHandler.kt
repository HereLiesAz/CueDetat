package com.hereliesaz.cuedetat.view.gesture

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.view.MainOverlayView.AppStateListener // Assuming AppStateListener is nested or accessible
import kotlin.math.abs

class GestureHandler(
    context: Context,
    private val appState: AppState,
    private val config: AppConfig,
    private val listener: AppStateListener?, // Listener for state changes originating from gestures
    private val onZoomChangedByGesture: (Float) -> Unit,
    private val onRotationChangedByGesture: (Float) -> Unit
) {
    private var lastTouchX = 0f
    private var lastTouchY = 0f // Kept for potential future use (e.g., two-finger pan)

    private enum class InteractionMode { NONE, PINCH_ZOOMING, PAN_TO_ROTATE }
    private var currentInteractionMode = InteractionMode.NONE
    private var isPinching = false // Flag to manage pinch state across events

    val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            // If already panning, don't let scale begin easily, or handle mode transition
            // For now, pinch takes precedence if it begins.
            currentInteractionMode = InteractionMode.PINCH_ZOOMING
            isPinching = true
            listener?.onUserInteraction() // Notify general interaction
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (currentInteractionMode != InteractionMode.PINCH_ZOOMING) return false

            val oldZoom = appState.zoomFactor
            // Detector scaleFactor is relative to the start of the current scale gesture
            val newZoomUncoerced = appState.zoomFactor * detector.scaleFactor

            // Clamping is handled by appState.updateZoomFactor, but we can check here too
            // val newZoom = newZoomUncoerced.coerceIn(config.MIN_ZOOM_FACTOR, config.MAX_ZOOM_FACTOR)

            // Check if the change is significant enough or if it's trying to scale
            // The actual updateZoomFactor in AppState will handle coercion and actual change detection.
            // We call onZoomChangedByGesture with the uncoerced factor, letting the MainOverlayView's
            // internal logic handle the update through AppState.
            if (detector.scaleFactor != 1.0f) { // If there's any scaling attempt
                onZoomChangedByGesture(newZoomUncoerced) // Pass the raw desired factor
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
        if (!appState.isInitialized) return false

        // Let ScaleGestureDetector inspect all events.
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)

        // If a scale gesture is in progress (pinching), it usually consumes the events.
        // We manage isPinching to prevent pan during a scale.
        if (scaleGestureDetector.isInProgress) {
            isPinching = true // Ensure this is set if scale is ongoing
            // When scaling ends, onScaleEnd will reset isPinching.
            return true // Scale gesture consumed the event
        }
        // If scale was in progress but just ended with this event (e.g. UP),
        // onScaleEnd would have set isPinching to false.

        val touchX = event.x

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // If not starting a scale gesture, consider it a pan start.
                if (!isPinching) { // isPinching should be false here if scale isn't in progress
                    currentInteractionMode = InteractionMode.PAN_TO_ROTATE
                    listener?.onUserInteraction()
                }
                lastTouchX = touchX
                lastTouchY = event.y // Store Y for potential future use
                // Return true if we initiated a pan, or if scale detector might handle it (though less likely on ACTION_DOWN alone if not multi-touch)
                return true // Consume ACTION_DOWN to receive subsequent MOVE/UP events
            }
            MotionEvent.ACTION_MOVE -> {
                if (currentInteractionMode == InteractionMode.PAN_TO_ROTATE && !isPinching) {
                    val dx = touchX - lastTouchX
                    if (abs(dx) > 0.1f) { // Only rotate if there's a meaningful delta
                        val angleDelta = dx * config.PAN_ROTATE_SENSITIVITY
                        onRotationChangedByGesture(appState.protractorRotationAngle + angleDelta)
                        listener?.onUserInteraction() // Notify general interaction for rotation
                    }
                    lastTouchX = touchX
                    lastTouchY = event.y
                    return true // Pan move consumed the event
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasInteracting = (currentInteractionMode != InteractionMode.NONE) || isPinching
                currentInteractionMode = InteractionMode.NONE
                isPinching = false // Ensure pinch flag is reset on UP/CANCEL
                // Return true if we were panning or if scale detector handled something (e.g. quick pinch then up)
                return wasInteracting || scaleHandled
            }
        }
        // If not handled by specific actions above, and scale detector didn't handle, return false.
        // However, scaleHandled might be true from inspecting the event even if our logic paths weren't hit.
        return scaleHandled
    }
}
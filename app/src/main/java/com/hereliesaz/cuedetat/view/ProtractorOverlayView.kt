package com.hereliesaz.cuedetat.view

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

/**
 * A custom View that hosts the drawing operations and now handles all touch input
 * for rotation and zooming.
 */
class ProtractorOverlayView(context: Context) : View(context) {

    private val renderer = OverlayRenderer()
    private val paints = PaintCache()
    private var state = OverlayState()

    // Callbacks to the ViewModel
    var onSizeChanged: ((Int, Int) -> Unit)? = null
    var onRotationChange: ((Float) -> Unit)? = null
    var onZoomChange: ((Float) -> Unit)? = null

    private var lastTouchX = 0f
    private var pointerId = -1

    // Gesture detector for pinch-to-zoom
    private val scaleGestureDetector = ScaleGestureDetector(context, PinchListener())

    inner class PinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newZoom = state.zoomFactor * detector.scaleFactor
            // Clamp the zoom factor to a reasonable range
            onZoomChange?.invoke(newZoom.coerceIn(0.1f, 4.0f))
            return true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, state, paints)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChanged?.invoke(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (scaleGestureDetector.isInProgress) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = event.getPointerId(0)
                lastTouchX = event.x
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerId != -1) {
                    val pointerIndex = event.findPointerIndex(pointerId)
                    if (pointerIndex != -1) {
                        val newX = event.getX(pointerIndex)
                        val dx = newX - lastTouchX
                        lastTouchX = newX

                        // MODIFIED: Inverted the rotation delta to match user expectation.
                        val rotationDelta = -dx * 0.2f
                        var newRotation = state.rotationAngle + rotationDelta

                        newRotation %= 360f
                        if (newRotation < 0) newRotation += 360f

                        onRotationChange?.invoke(newRotation)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pointerId = -1
            }
        }
        return true
    }

    fun updateState(newState: OverlayState) {
        this.state = newState
        paints.updateColors(newState.dynamicColorScheme)
        invalidate()
    }
}

package com.hereliesaz.cuedetat.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

@SuppressLint("ViewConstructor")
class ProtractorOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val renderer = OverlayRenderer()
    private val paints = PaintCache()
    private var internalState: OverlayState = OverlayState()

    var onSizeChanged: (Int, Int) -> Unit = { _, _ -> }
    var onProtractorRotationChange: (Float) -> Unit = {}
    var onProtractorUnitMoved: (PointF) -> Unit = {}
    var onActualCueBallScreenMoved: (PointF) -> Unit = {}
    var onBankingAimTargetScreenDrag: (PointF) -> Unit = {}
    var onScale: (Float) -> Unit = {}
    var onGestureStarted: () -> Unit = {}
    var onGestureEnded: () -> Unit = {}

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onScale(detector.scaleFactor)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            onProtractorRotationChange(internalState.protractorUnit.rotationDegrees - (distanceX / 5f))
            return true
        }
    })

    fun updateState(newState: OverlayState, isDark: Boolean) {
        this.internalState = newState
        paints.updateColors(newState, isDark)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChanged(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, internalState, paints, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onGestureStarted()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onGestureEnded()
        }
        return handled || super.onTouchEvent(event)
    }
}

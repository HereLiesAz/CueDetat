// app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlayView.kt
package com.hereliesaz.cuedetat.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

@SuppressLint("ClickableViewAccessibility")
class ProtractorOverlayView(context: Context) : View(context) {

    private val renderer = OverlayRenderer()
    private val paints = PaintCache()
    private var state = OverlayState()
    private var barbaroTypeface: Typeface? = null

    // Callbacks to ViewModel
    var onSizeChanged: ((Int, Int) -> Unit)? = null
    var onPan: ((Float, Float) -> Unit)? = null
    var onUnitMove: ((PointF) -> Unit)? = null
    var onActualCueBallMoved: ((PointF) -> Unit)? = null
    var onScale: ((Float) -> Unit)? = null

    // Gesture Handling
    private val scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private enum class TouchMode { NONE, PANNING, DRAGGING_PROTRACTOR, DRAGGING_CUE_BALL }

    private var mode = TouchMode.NONE

    init {
        if (!isInEditMode) {
            barbaroTypeface = ResourcesCompat.getFont(context, R.font.barbaro)
            paints.setTypeface(barbaroTypeface)
        }
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onScale?.invoke(detector.scaleFactor)
                return true
            }
        }
        scaleGestureDetector = ScaleGestureDetector(context, listener)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, state, paints, barbaroTypeface)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChanged?.invoke(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (scaleGestureDetector.isInProgress) {
            mode = TouchMode.NONE
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)

                // --- Hit Testing ---
                val touchPoint = PointF(event.x, event.y)
                if (state.hasInverseMatrix) {
                    // Check for protractor unit touch first, as it's the primary tool
                    val pTGC = DrawingUtils.mapPoint(
                        state.protractorUnit.center,
                        state.worldToScreenMatrix
                    )
                    val targetRadiusInfo =
                        DrawingUtils.getPerspectiveRadiusAndLift(state.protractorUnit, state)
                    // Use a larger tap radius for easier interaction
                    if (DrawingUtils.distance(touchPoint, pTGC) < targetRadiusInfo.radius * 2.0f) {
                        mode = TouchMode.DRAGGING_PROTRACTOR
                        return true
                    }

                    // Check for actual cue ball touch if it exists
                    state.actualCueBall?.let {
                        val pACB = DrawingUtils.mapPoint(it.center, state.worldToScreenMatrix)
                        val cueRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, state)
                        if (DrawingUtils.distance(touchPoint, pACB) < cueRadiusInfo.radius * 2.0f) {
                            mode = TouchMode.DRAGGING_CUE_BALL
                            return true
                        }
                    }
                }

                // If nothing was hit, default to panning
                mode = TouchMode.PANNING
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return true

                val currentX = event.getX(pointerIndex)
                val currentY = event.getY(pointerIndex)
                val currentPoint = PointF(currentX, currentY)

                when (mode) {
                    TouchMode.PANNING -> {
                        val dx = currentX - lastTouchX
                        val dy = currentY - lastTouchY
                        onPan?.invoke(dx, dy)
                    }

                    TouchMode.DRAGGING_PROTRACTOR -> onUnitMove?.invoke(currentPoint)
                    TouchMode.DRAGGING_CUE_BALL -> onActualCueBallMoved?.invoke(currentPoint)
                    TouchMode.NONE -> { /* Do nothing */
                    }
                }
                lastTouchX = currentX
                lastTouchY = currentY
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = TouchMode.NONE
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerIndex = event.actionIndex
                if (event.getPointerId(upPointerIndex) == activePointerId) {
                    val newPointerIndex = if (upPointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
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

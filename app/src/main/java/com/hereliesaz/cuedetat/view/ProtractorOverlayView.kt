// app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlayView.kt
package com.hereliesaz.cuedetat.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ProtractorOverlayView(context: Context) : View(context) {

    private enum class InteractionMode {
        NONE, SCALING, ROTATING_PROTRACTOR, MOVING_PROTRACTOR_UNIT, MOVING_ACTUAL_CUE_BALL, AIMING_BANK_SHOT
    }

    private val paints = PaintCache()
    private val renderer = OverlayRenderer()
    private var canonicalState = OverlayState()
    private var barbaroTypeface: Typeface? = null

    var onSizeChanged: ((Int, Int) -> Unit)? = null
    var onProtractorRotationChange: ((Float) -> Unit)? = null
    var onProtractorUnitMoved: ((PointF) -> Unit)? = null
    var onActualCueBallScreenMoved: ((PointF) -> Unit)? = null
    var onScale: ((Float) -> Unit)? = null
    var onGestureStarted: (() -> Unit)? = null
    var onGestureEnded: (() -> Unit)? = null
    var onBankingAimTargetScreenDrag: ((PointF) -> Unit)? = null

    private val scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX_single = 0f
    private var lastTouchY_single = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val draggableElementSlop = touchSlop * 7.0f

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var interactionMode = InteractionMode.NONE
    private var gestureInProgress = false

    init {
        if (!isInEditMode) {
            barbaroTypeface = ResourcesCompat.getFont(context, R.font.barbaro)
            paints.setTypeface(barbaroTypeface)
        }
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                interactionMode = InteractionMode.SCALING
                if (!gestureInProgress) {
                    onGestureStarted?.invoke()
                    gestureInProgress = true
                }
                Log.d("ProtractorOverlayView", "onScaleBegin: SCALING mode activated")
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (interactionMode == InteractionMode.SCALING) {
                    onScale?.invoke(detector.scaleFactor)
                    return true
                }
                return false
            }
        }
        scaleGestureDetector = ScaleGestureDetector(context, scaleListener)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, canonicalState, paints, barbaroTypeface)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChanged?.invoke(w, h)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (canonicalState.isSpatiallyLocked && !scaleGestureDetector.isInProgress) {
            return true // Consume all non-scaling touches when locked
        }
        if (scaleGestureDetector.isInProgress) {
            return true // Scaling gesture is being handled
        }

        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!gestureInProgress) {
                    onGestureStarted?.invoke(); gestureInProgress = true
                }
                activePointerId = event.getPointerId(0)
                lastTouchX_single = event.getX(0)
                lastTouchY_single = event.getY(0)
                determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) return true
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return true

                val currentX = event.getX(pointerIndex)
                val currentY = event.getY(pointerIndex)
                val dx = currentX - lastTouchX_single

                when (interactionMode) {
                    InteractionMode.ROTATING_PROTRACTOR -> {
                        if (abs(dx) > touchSlop/2) {
                            onProtractorRotationChange?.invoke(canonicalState.protractorUnit.rotationDegrees - (dx * 0.3f))
                        }
                    }
                    InteractionMode.MOVING_PROTRACTOR_UNIT -> onProtractorUnitMoved?.invoke(PointF(currentX, currentY))
                    InteractionMode.MOVING_ACTUAL_CUE_BALL -> onActualCueBallScreenMoved?.invoke(PointF(currentX, currentY))
                    InteractionMode.AIMING_BANK_SHOT -> onBankingAimTargetScreenDrag?.invoke(PointF(currentX, currentY))
                    else -> {} // NONE, SCALING
                }
                lastTouchX_single = currentX
                lastTouchY_single = currentY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (gestureInProgress) {
                    onGestureEnded?.invoke(); gestureInProgress = false
                }
                interactionMode = InteractionMode.NONE
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
        return true
    }

    private fun determineSingleTouchMode(touchX: Float, touchY: Float) {
        val touchPoint = PointF(touchX, touchY)
        interactionMode = InteractionMode.NONE // Reset

        if (canonicalState.hasInverseMatrix) { // Need matrix to project points
            if (canonicalState.isBankingMode) {
                // Check for touch on the banking ball
                canonicalState.actualCueBall?.let {
                    val ballScreenPos = DrawingUtils.mapPoint(it.logicalPosition, canonicalState.pitchMatrix)
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, canonicalState)
                    if (DrawingUtils.distance(touchPoint, ballScreenPos) < radiusInfo.radius + draggableElementSlop) {
                        interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                        return
                    }
                }
                // If not on the ball, the gesture is for aiming
                interactionMode = InteractionMode.AIMING_BANK_SHOT
            } else { // Protractor Mode
                // Check for touch on the Actual Cue Ball first (if it exists)
                canonicalState.actualCueBall?.let {
                    val ballScreenPos = DrawingUtils.mapPoint(it.logicalPosition, canonicalState.pitchMatrix)
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, canonicalState)
                    if (DrawingUtils.distance(touchPoint, ballScreenPos) < radiusInfo.radius + draggableElementSlop) {
                        interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                        return
                    }
                }

                // Check for touch on the Protractor Unit (Target Ball)
                val unitScreenPos = DrawingUtils.mapPoint(canonicalState.protractorUnit.logicalPosition, canonicalState.pitchMatrix)
                val unitRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(canonicalState.protractorUnit, canonicalState)
                if (DrawingUtils.distance(touchPoint, unitScreenPos) < unitRadiusInfo.radius + draggableElementSlop) {
                    interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT
                    return
                }

                // If not on any ball, the gesture is for rotation
                interactionMode = InteractionMode.ROTATING_PROTRACTOR
            }
        }
    }

    fun updateState(newState: OverlayState, systemIsDark: Boolean) {
        this.canonicalState = newState
        this.paints.updateColors(newState, systemIsDark)
        invalidate()
    }
}
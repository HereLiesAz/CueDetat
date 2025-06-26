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
                // Spatial lock does NOT prevent scaling.
                interactionMode = InteractionMode.SCALING
                if (!gestureInProgress) {
                    onGestureStarted?.invoke()
                    gestureInProgress = true
                }
                Log.d("ProtractorOverlayView", "onScaleBegin: SCALING mode activated")
                return true // Always handle scale if detected
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (interactionMode == InteractionMode.SCALING) {
                    onScale?.invoke(detector.scaleFactor)
                    return true
                }
                return false
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                super.onScaleEnd(detector)
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
        // Pass to ScaleGestureDetector first. It will update its state.
        val scaleEventHandledByDetector = scaleGestureDetector.onTouchEvent(event)

        // If spatially locked and not a scaling gesture, consume most other touches.
        // Scaling IS allowed when locked.
        if (canonicalState.isSpatiallyLocked && !scaleGestureDetector.isInProgress) {
            val currentAction = event.actionMasked
            if (currentAction == MotionEvent.ACTION_DOWN) {
                if (!gestureInProgress) { onGestureStarted?.invoke(); gestureInProgress = true }
                // Set activePointerId for potential gesture end, even if locked
                activePointerId = event.getPointerId(0)
                lastTouchX_single = event.getX(0)
                lastTouchY_single = event.getY(0)
                interactionMode = InteractionMode.NONE // No specific interaction if locked
            } else if (currentAction == MotionEvent.ACTION_UP || currentAction == MotionEvent.ACTION_CANCEL) {
                if (gestureInProgress) { onGestureEnded?.invoke(); gestureInProgress = false }
                interactionMode = InteractionMode.NONE
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            return true // Consume to prevent further processing for non-scaling touches when locked
        }

        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        // If ScaleGestureDetector is in progress (scaling), it primarily handles the event.
        // However, we still need to manage ACTION_UP/CANCEL or POINTER_UP to reset our state.
        if (scaleGestureDetector.isInProgress || (interactionMode == InteractionMode.SCALING && action == MotionEvent.ACTION_MOVE)) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ||
                (action == MotionEvent.ACTION_POINTER_UP && event.pointerCount <= 2)) {
                // Fall through to handle these specific end/transition events for scaling
            } else {
                return true // ScaleGestureDetector is actively scaling or was the primary handler
            }
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!gestureInProgress) {
                    onGestureStarted?.invoke(); gestureInProgress = true
                }
                // Set primary pointer for potential single touch drag
                activePointerId = event.getPointerId(0)
                lastTouchX_single = event.getX(0)
                lastTouchY_single = event.getY(0)

                // Only determine single touch mode if not already in scaling
                // (which shouldn't happen here due to the gate above, but good for safety)
                if (interactionMode != InteractionMode.SCALING) {
                    determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                }
                Log.d("ProtractorOverlayView", "ACTION_DOWN: Mode=$interactionMode, activePointerId=$activePointerId, spatiallyLocked=${canonicalState.isSpatiallyLocked}")
            }
            MotionEvent.ACTION_MOVE -> {
                // This handles single pointer moves if not locked and not scaling.
                if (event.pointerCount == 1 && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val currentEventPointerIndex = event.findPointerIndex(activePointerId)
                    if (currentEventPointerIndex == -1) return true // Stale active pointer ID

                    val currentX = event.getX(currentEventPointerIndex)
                    val currentY = event.getY(currentEventPointerIndex)
                    val dx = currentX - lastTouchX_single

                    // If mode is NONE, attempt to determine mode based on movement surpassing slop
                    if (interactionMode == InteractionMode.NONE && (abs(dx) > touchSlop || abs(currentY - lastTouchY_single) > touchSlop)) {
                        determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                        Log.d("ProtractorOverlayView", "ACTION_MOVE: Re-determined Mode=$interactionMode due to slop")
                    }

                    when (interactionMode) {
                        InteractionMode.ROTATING_PROTRACTOR -> {
                            if (abs(dx) > touchSlop/2) {
                                onProtractorRotationChange?.invoke(canonicalState.protractorUnit.rotationDegrees - (dx * 0.3f))
                                lastTouchX_single = currentX
                            }
                        }
                        InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                            onProtractorUnitMoved?.invoke(PointF(currentX, currentY))
                            lastTouchX_single = currentX; lastTouchY_single = currentY
                        }
                        InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                            onActualCueBallScreenMoved?.invoke(PointF(currentX, currentY))
                            lastTouchX_single = currentX; lastTouchY_single = currentY
                        }
                        InteractionMode.AIMING_BANK_SHOT -> {
                            onBankingAimTargetScreenDrag?.invoke(PointF(currentX, currentY))
                            lastTouchX_single = currentX; lastTouchY_single = currentY
                        }
                        InteractionMode.SCALING, InteractionMode.NONE -> { /* No specific single touch action */ }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (gestureInProgress) {
                    onGestureEnded?.invoke(); gestureInProgress = false
                }
                Log.d("ProtractorOverlayView", "ACTION_UP/CANCEL: Resetting mode. Old mode=$interactionMode")
                interactionMode = InteractionMode.NONE
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // A new pointer has come down. ScaleGestureDetector's onScaleBegin
                // (called by scaleGestureDetector.onTouchEvent(event) at the top)
                // should handle setting interactionMode = SCALING if it's a scale.
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerId = event.getPointerId(pointerIndex)
                Log.d("ProtractorOverlayView", "ACTION_POINTER_UP: upPointerId=$upPointerId, activePointerId=$activePointerId, event.pointerCount=${event.pointerCount}, oldMode=$interactionMode")

                if (interactionMode == InteractionMode.SCALING) {
                    // If scaling was active and this UP event means only one pointer remains,
                    // transition to single touch state.
                    if (event.pointerCount == 2) { // pointerCount includes the one going up, so 2 means 1 will remain
                        interactionMode = InteractionMode.NONE // Scaling ends
                        // Find the remaining pointer and set it as active for single touch
                        for (i in 0 until event.pointerCount) {
                            val id = event.getPointerId(i)
                            if (id != upPointerId) {
                                activePointerId = id
                                lastTouchX_single = event.getX(i)
                                lastTouchY_single = event.getY(i)
                                Log.d("ProtractorOverlayView", "Transition from SCALING to single: new activePointerId=$activePointerId, x=$lastTouchX_single, y=$lastTouchY_single")
                                // If not spatially locked, redetermine mode for the remaining finger.
                                if (!canonicalState.isSpatiallyLocked) {
                                    determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                                }
                                break
                            }
                        }
                    } else if (event.pointerCount < 2) { // All pointers lifted or error
                        interactionMode = InteractionMode.NONE
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        if (gestureInProgress) { onGestureEnded?.invoke(); gestureInProgress = false }
                    }
                } else if (upPointerId == activePointerId) {
                    // The primary single-touch pointer was lifted, but it's not ACTION_UP (more pointers remain)
                    // This can happen if user briefly adds a second finger without initiating a scale, then lifts the first.
                    // Switch to the next available pointer for single touch.
                    val newPointerActionIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerActionIndex < event.pointerCount) { // Check if another pointer exists
                        activePointerId = event.getPointerId(newPointerActionIndex)
                        lastTouchX_single = event.getX(newPointerActionIndex)
                        lastTouchY_single = event.getY(newPointerActionIndex)
                        interactionMode = InteractionMode.NONE // Safest to reset
                        // If not spatially locked, redetermine mode for the new active finger.
                        if (!canonicalState.isSpatiallyLocked) {
                            determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                        }
                        Log.d("ProtractorOverlayView", "Primary single touch lifted, switched to other pointer: new activePointerId=$activePointerId. New Mode: $interactionMode")
                    } else { // Should ideally be caught by ACTION_UP
                        interactionMode = InteractionMode.NONE
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
            }
        }
        return true
    }

    private fun determineSingleTouchMode(touchX: Float, touchY: Float) {
        val touchPoint = PointF(touchX, touchY)
        var determinedMode = InteractionMode.NONE

        if (canonicalState.isBankingMode) {
            canonicalState.actualCueBall?.let {
                // Use screenCenter for screen-based interaction check
                val ballScreenPos = it.screenCenter
                val bankingBallSlop = draggableElementSlop * 0.75f
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < bankingBallSlop) {
                    determinedMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (determinedMode == InteractionMode.NONE) {
                determinedMode = InteractionMode.AIMING_BANK_SHOT
            }
        } else { // Protractor Mode
            canonicalState.actualCueBall?.let {
                // Use screenCenter for screen-based interaction check
                val ballScreenPos = it.screenCenter
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, canonicalState)
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < radiusInfo.radius + draggableElementSlop) {
                    determinedMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (determinedMode == InteractionMode.NONE) {
                // Use screenCenter for screen-based interaction check
                val unitScreenPos = canonicalState.protractorUnit.screenCenter
                val unitRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(canonicalState.protractorUnit, canonicalState)
                if (DrawingUtils.distance(touchPoint, unitScreenPos) < unitRadiusInfo.radius + draggableElementSlop) {
                    determinedMode = InteractionMode.MOVING_PROTRACTOR_UNIT
                }
            }
            if (determinedMode == InteractionMode.NONE) {
                determinedMode = InteractionMode.ROTATING_PROTRACTOR
            }
        }
        interactionMode = determinedMode
        Log.d("ProtractorOverlayView", "determineSingleTouchMode: x=$touchX, y=$touchY -> Mode=$interactionMode")
    }

    fun updateState(newState: OverlayState, systemIsDark: Boolean) {
        this.canonicalState = newState
        this.paints.updateColors(newState, systemIsDark)
        invalidate()
    }
}
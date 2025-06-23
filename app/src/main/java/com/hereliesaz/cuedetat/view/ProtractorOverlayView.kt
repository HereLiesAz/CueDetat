// app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlayView.kt
package com.hereliesaz.cuedetat.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Typeface
import android.util.Log // Added for debugging
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
    private val draggableElementSlop = touchSlop * 7.0f // Consider if this is too large

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
                // Regardless of current single-touch mode, if a scale gesture starts, prioritize it.
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
                    // Log.d("ProtractorOverlayView", "onScale: factor=${detector.scaleFactor}")
                    return true
                }
                return false
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                // This is called when the scale gesture itself ends (e.g., fingers lift sufficiently apart).
                // We will also handle finger lifting in ACTION_POINTER_UP and ACTION_UP.
                // Log.d("ProtractorOverlayView", "onScaleEnd")
                // No specific interactionMode change here, let ACTION_POINTER_UP / ACTION_UP handle final mode.
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
        // If it consumes the event (e.g., during a scale), it might return true.
        val scaleEventHandled = scaleGestureDetector.onTouchEvent(event)

        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        // If ScaleGestureDetector is in progress, it usually means interactionMode is SCALING.
        // Let it handle the event primarily.
        if (scaleGestureDetector.isInProgress || interactionMode == InteractionMode.SCALING && action == MotionEvent.ACTION_MOVE) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ||
                (action == MotionEvent.ACTION_POINTER_UP && event.pointerCount <= 2) ) { // If last two fingers lift or one of two lifts
                // Fall through to handle gesture end or transition from scaling.
            } else {
                return true // ScaleGestureDetector is handling it, or we are in active scaling move.
            }
        }


        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!gestureInProgress) {
                    onGestureStarted?.invoke(); gestureInProgress = true
                }
                // activePointerId is for single touch drag, scale detector handles multiple.
                event.findPointerIndex(activePointerId).takeIf { it != -1 }?.let {
                    // This pointer is already active, unusual for ACTION_DOWN unless it's a quick tap release-down
                } ?: run {
                    activePointerId = event.getPointerId(0)
                    lastTouchX_single = event.getX(0)
                    lastTouchY_single = event.getY(0)
                }

                // Only determine single touch mode if not already in scaling (e.g. if scale ended but one finger remained down then lifted and tapped again)
                if (interactionMode != InteractionMode.SCALING) {
                    determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                }
                Log.d("ProtractorOverlayView", "ACTION_DOWN: Mode=$interactionMode, activePointerId=$activePointerId")
            }
            MotionEvent.ACTION_MOVE -> {
                // If scaling was handled above, this won't be reached for scaling moves.
                // This handles single pointer moves.
                if (event.pointerCount == 1 && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val currentEventPointerIndex = event.findPointerIndex(activePointerId)
                    if (currentEventPointerIndex == -1) return true // Stale active pointer ID

                    val currentX = event.getX(currentEventPointerIndex)
                    val currentY = event.getY(currentEventPointerIndex)
                    val dx = currentX - lastTouchX_single
                    // val dy = currentY - lastTouchY_single // Not used for rotation

                    // If mode is NONE, attempt to determine mode based on movement surpassing slop
                    if (interactionMode == InteractionMode.NONE && (abs(dx) > touchSlop || abs(currentY - lastTouchY_single) > touchSlop)) {
                        determineSingleTouchMode(lastTouchX_single, lastTouchY_single) // Use original touch down point for determination
                        Log.d("ProtractorOverlayView", "ACTION_MOVE: Re-determined Mode=$interactionMode due to slop")
                    }

                    when (interactionMode) {
                        InteractionMode.ROTATING_PROTRACTOR -> {
                            if (abs(dx) > touchSlop/2) { // smaller slop for continuous rotation
                                onProtractorRotationChange?.invoke(canonicalState.protractorUnit.rotationDegrees - (dx * 0.3f))
                                lastTouchX_single = currentX // Update lastTouch for next delta
                                // lastTouchY_single = currentY; // Keep Y updated too
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
                        // SCALING is handled by ScaleGestureDetector, NONE means no dominant single gesture yet
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
                // A new pointer has come down. If not already scaling, ScaleGestureDetector's onScaleBegin
                // should handle setting interactionMode = SCALING.
                // We don't need to change activePointerId here for single touch,
                // as this is now a multi-touch gesture.
                // Log.d("ProtractorOverlayView", "ACTION_POINTER_DOWN: event.pointerCount=${event.pointerCount}")
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerId = event.getPointerId(pointerIndex)
                // Log.d("ProtractorOverlayView", "ACTION_POINTER_UP: upPointerId=$upPointerId, activePointerId=$activePointerId, event.pointerCount=${event.pointerCount}, oldMode=$interactionMode")

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
                                break
                            }
                        }
                    } else if (event.pointerCount < 2) { // All pointers lifted or error
                        interactionMode = InteractionMode.NONE
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        if (gestureInProgress) { onGestureEnded?.invoke(); gestureInProgress = false }
                    }
                    // If event.pointerCount > 2, scaling might continue with remaining pointers.
                    // ScaleGestureDetector handles this.
                } else if (upPointerId == activePointerId) {
                    // The primary single-touch pointer was lifted, but it's not ACTION_UP (more pointers remain)
                    // This is a complex case, often means transitioning from an unintended multi-touch that wasn't a scale.
                    // Switch to the next available pointer for single touch.
                    val newPointerActionIndex = if (pointerIndex == 0) 1 else 0 // Simplistic way to get other pointer
                    if (newPointerActionIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(newPointerActionIndex)
                        lastTouchX_single = event.getX(newPointerActionIndex)
                        lastTouchY_single = event.getY(newPointerActionIndex)
                        // Mode might need redetermination or reset
                        interactionMode = InteractionMode.NONE // Safest to reset and let next MOVE determine
                        Log.d("ProtractorOverlayView", "Primary single touch lifted, switched to other pointer: new activePointerId=$activePointerId")
                    } else {
                        // Should be caught by ACTION_UP
                        interactionMode = InteractionMode.NONE
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
                // If a non-active, non-scaling pointer went up, activePointerId and mode for single touch remain.
            }
        }
        return true // This view always handles touch events to manage its complex gestures.
    }

    private fun determineSingleTouchMode(touchX: Float, touchY: Float) {
        // if (interactionMode == InteractionMode.SCALING) return // Already checked before calling normally

        val touchPoint = PointF(touchX, touchY)
        var determinedMode = InteractionMode.NONE // Default to NONE

        if (canonicalState.isBankingMode) {
            canonicalState.actualCueBall?.let {
                val ballScreenPos = DrawingUtils.mapPoint(it.center, canonicalState.pitchMatrix)
                // Use a smaller slop for banking ball selection as it's the primary interactive element.
                val bankingBallSlop = draggableElementSlop * 0.75f
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < bankingBallSlop) {
                    determinedMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (determinedMode == InteractionMode.NONE) {
                determinedMode = InteractionMode.AIMING_BANK_SHOT
                // Don't invoke callback here, let ACTION_MOVE handle it based on confirmed mode
            }
        } else { // Protractor Mode
            canonicalState.actualCueBall?.let {
                val ballScreenPos = DrawingUtils.mapPoint(it.center, canonicalState.pitchMatrix)
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, canonicalState)
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < radiusInfo.radius + draggableElementSlop) {
                    determinedMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (determinedMode == InteractionMode.NONE) {
                val unitScreenPos = DrawingUtils.mapPoint(canonicalState.protractorUnit.center, canonicalState.pitchMatrix)
                val unitRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(canonicalState.protractorUnit, canonicalState)
                if (DrawingUtils.distance(touchPoint, unitScreenPos) < unitRadiusInfo.radius + draggableElementSlop) {
                    determinedMode = InteractionMode.MOVING_PROTRACTOR_UNIT
                }
            }
            if (determinedMode == InteractionMode.NONE) {
                determinedMode = InteractionMode.ROTATING_PROTRACTOR
            }
        }
        interactionMode = determinedMode // Set the determined mode
        // Log.d("ProtractorOverlayView", "determineSingleTouchMode: x=$touchX, y=$touchY -> Mode=$interactionMode")
    }

    fun updateState(newState: OverlayState, systemIsDark: Boolean) {
        this.canonicalState = newState
        this.paints.updateColors(newState, systemIsDark)
        invalidate()
    }
}
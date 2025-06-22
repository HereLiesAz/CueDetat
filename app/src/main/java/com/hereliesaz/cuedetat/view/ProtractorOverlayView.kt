package com.hereliesaz.cuedetat.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Typeface
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

    private val paints = PaintCache() // Keep paints instance here
    private val renderer = OverlayRenderer()
    private var canonicalState = OverlayState()
    private var barbaroTypeface: Typeface? = null

    // Callbacks to ViewModel (using SCREEN coordinates for positions)
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
            paints.setTypeface(barbaroTypeface) // Set typeface on internal paints instance
        }
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                if (interactionMode == InteractionMode.NONE || interactionMode == InteractionMode.SCALING) {
                    interactionMode = InteractionMode.SCALING
                    if (!gestureInProgress) {
                        onGestureStarted?.invoke(); gestureInProgress = true
                    }
                    return true
                }
                return false
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (interactionMode == InteractionMode.SCALING) {
                    onScale?.invoke(detector.scaleFactor); return true
                }
                return false
            }
            // onScaleEnd not strictly needed as ACTION_UP/CANCEL handles gesture end
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
        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!gestureInProgress) {
                    onGestureStarted?.invoke(); gestureInProgress = true
                }
                lastTouchX_single = event.getX(0)
                lastTouchY_single = event.getY(0)
                activePointerId = event.getPointerId(0)
                if (interactionMode != InteractionMode.SCALING) {
                    determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (interactionMode == InteractionMode.SCALING) return true
                if (event.pointerCount == 1 && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val currentEventPointerIndex = event.findPointerIndex(activePointerId)
                    if (currentEventPointerIndex == -1) return true

                    val currentX = event.getX(currentEventPointerIndex)
                    val currentY = event.getY(currentEventPointerIndex)
                    val dx = currentX - lastTouchX_single

                    when (interactionMode) {
                        InteractionMode.ROTATING_PROTRACTOR -> {
                            if (abs(dx) > touchSlop) {
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
                        InteractionMode.NONE -> {
                            if (abs(currentX - lastTouchX_single) > touchSlop || abs(currentY - lastTouchY_single) > touchSlop) {
                                determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
                            }
                        }
                        else -> {} // SCALING
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (gestureInProgress) {
                    onGestureEnded?.invoke(); gestureInProgress = false
                }
                interactionMode = InteractionMode.NONE
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerId = event.getPointerId(pointerIndex)
                if (upPointerId == activePointerId) {
                    val newPointerIdx = if (pointerIndex == 0) 1 else 0
                    if (newPointerIdx < event.pointerCount) {
                        lastTouchX_single = event.getX(newPointerIdx)
                        lastTouchY_single = event.getY(newPointerIdx)
                        activePointerId = event.getPointerId(newPointerIdx)
                    }
                }
                if (event.pointerCount <= 1 && interactionMode == InteractionMode.SCALING) {
                    interactionMode = InteractionMode.NONE
                }
            }
        }
        return true
    }

    private fun determineSingleTouchMode(touchX: Float, touchY: Float) {
        if (interactionMode == InteractionMode.SCALING) return
        val touchPoint = PointF(touchX, touchY)
        interactionMode = InteractionMode.NONE

        if (canonicalState.isBankingMode) {
            canonicalState.actualCueBall?.let {
                val ballScreenPos = DrawingUtils.mapPoint(it.center, canonicalState.pitchMatrix)
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < draggableElementSlop) {
                    interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL; return
                }
            }
            interactionMode = InteractionMode.AIMING_BANK_SHOT
            onBankingAimTargetScreenDrag?.invoke(touchPoint)
        } else {
            canonicalState.actualCueBall?.let {
                val ballScreenPos = DrawingUtils.mapPoint(it.center, canonicalState.pitchMatrix)
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, canonicalState)
                if (DrawingUtils.distance(
                        touchPoint,
                        ballScreenPos
                    ) < radiusInfo.radius + draggableElementSlop
                ) {
                    interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL; return
                }
            }
            val unitScreenPos = DrawingUtils.mapPoint(
                canonicalState.protractorUnit.center,
                canonicalState.pitchMatrix
            )
            val unitRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                canonicalState.protractorUnit,
                canonicalState
            )
            if (DrawingUtils.distance(
                    touchPoint,
                    unitScreenPos
                ) < unitRadiusInfo.radius + draggableElementSlop
            ) {
                interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT; return
            }
            interactionMode = InteractionMode.ROTATING_PROTRACTOR
        }
    }

    // Modified updateState to accept systemIsDark for PaintCache
    fun updateState(newState: OverlayState, systemIsDark: Boolean) {
        this.canonicalState = newState
        this.paints.updateColors(
            newState,
            systemIsDark
        ) // Update paints with full state and system theme
        invalidate()
    }
}
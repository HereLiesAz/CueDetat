package com.hereliesaz.cuedetatlite.view.gestures

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import java.lang.Math.toDegrees
import kotlin.math.atan2

class GestureHandler(
    context: Context,
    private val onEvent: (MainScreenEvent) -> Unit
) {

    private enum class InteractionMode {
        NONE, SCALING, ROTATING_AIM, MOVING_TARGET_BALL, MOVING_ACTUAL_CUE_BALL, AIMING_BANK_SHOT
    }

    private val scaleGestureDetector: ScaleGestureDetector
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val draggableElementSlop = touchSlop * 7.0f

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var interactionMode = InteractionMode.NONE
    private var gestureInProgress = false
    private var lastAngle = 0f // Track last angle for continuous rotation

    init {
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                interactionMode = InteractionMode.SCALING
                if (!gestureInProgress) { gestureInProgress = true }
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (interactionMode == InteractionMode.SCALING) {
                    onEvent(MainScreenEvent.ZoomChanged(detector.scaleFactor))
                    return true
                }
                return false
            }
        }
        scaleGestureDetector = ScaleGestureDetector(context, scaleListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun onTouchEvent(event: MotionEvent, state: OverlayState): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (state.isSpatiallyLocked && !scaleGestureDetector.isInProgress) return true
        if (scaleGestureDetector.isInProgress) return true

        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!gestureInProgress) { gestureInProgress = true }
                activePointerId = event.getPointerId(0)
                determineSingleTouchMode(event.getX(0), event.getY(0), state)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val currentEventPointerIndex = event.findPointerIndex(activePointerId)
                    if (currentEventPointerIndex == -1) return true

                    val currentX = event.getX(currentEventPointerIndex)
                    val currentY = event.getY(currentEventPointerIndex)
                    val currentPoint = PointF(currentX, currentY)

                    when (interactionMode) {
                        InteractionMode.ROTATING_AIM -> {
                            val targetBallScreenPos = DrawingUtils.mapPoint(state.screenState.protractorUnit.targetBall.logicalPosition, state.pitchMatrix)
                            val currentAngle = toDegrees(atan2(currentY - targetBallScreenPos.y, currentX - targetBallScreenPos.x).toDouble()).toFloat()
                            val deltaAngle = currentAngle - lastAngle
                            // Use the delta to adjust the current rotation
                            onEvent(MainScreenEvent.AimingAngleChanged(state.screenState.protractorUnit.aimingAngleDegrees + deltaAngle))
                            lastAngle = currentAngle
                        }
                        InteractionMode.MOVING_TARGET_BALL -> onEvent(MainScreenEvent.BallMoved(1, currentPoint))
                        InteractionMode.MOVING_ACTUAL_CUE_BALL -> onEvent(MainScreenEvent.BallMoved(2, currentPoint))
                        InteractionMode.AIMING_BANK_SHOT -> onEvent(MainScreenEvent.BankingAimTargetChanged(currentPoint))
                        else -> {}
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (gestureInProgress) { gestureInProgress = false }
                interactionMode = InteractionMode.NONE
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerId = event.getPointerId(pointerIndex)
                if (upPointerId == activePointerId) {
                    val newPointerActionIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerActionIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(newPointerActionIndex)
                    } else {
                        interactionMode = InteractionMode.NONE
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
            }
        }
        return true
    }

    private fun determineSingleTouchMode(touchX: Float, touchY: Float, state: OverlayState) {
        val touchPoint = PointF(touchX, touchY)
        interactionMode = InteractionMode.NONE

        if (state.isBankingMode) {
            state.screenState.actualCueBall?.let {
                val ballScreenPos = DrawingUtils.mapPoint(it.logicalPosition, state.pitchMatrix)
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < draggableElementSlop) {
                    interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL; return
                }
            }
            interactionMode = InteractionMode.AIMING_BANK_SHOT
            onEvent(MainScreenEvent.BankingAimTargetChanged(touchPoint))
        } else {
            state.screenState.actualCueBall?.let {
                val ballScreenPos = DrawingUtils.mapPoint(it.logicalPosition, state.pitchMatrix)
                if (DrawingUtils.distance(touchPoint, ballScreenPos) < draggableElementSlop) {
                    interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL; return
                }
            }

            val targetBallScreenPos = DrawingUtils.mapPoint(state.screenState.protractorUnit.targetBall.logicalPosition, state.pitchMatrix)
            if (DrawingUtils.distance(touchPoint, targetBallScreenPos) < draggableElementSlop) {
                interactionMode = InteractionMode.MOVING_TARGET_BALL; return
            }

            // For rotation, initialize the lastAngle to the starting touch angle
            interactionMode = InteractionMode.ROTATING_AIM
            lastAngle = toDegrees(atan2(touchY - targetBallScreenPos.y, touchX - targetBallScreenPos.x).toDouble()).toFloat()
        }
    }
}
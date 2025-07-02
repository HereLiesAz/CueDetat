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
        NONE, SCALING, ROTATING_AIM, MOVING_TARGET_BALL
    }

    private val scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX_single = 0f
    private var lastTouchY_single = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val draggableElementSlop = touchSlop * 7.0f

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var interactionMode = InteractionMode.NONE
    private var gestureInProgress = false

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
                lastTouchX_single = event.getX(0)
                lastTouchY_single = event.getY(0)
                determineSingleTouchMode(lastTouchX_single, lastTouchY_single, state)
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
                            val angleRad = atan2(currentY - targetBallScreenPos.y, currentX - targetBallScreenPos.x)
                            val angleDeg = toDegrees(angleRad.toDouble()).toFloat()
                            onEvent(MainScreenEvent.AimingAngleChanged(angleDeg + 180))
                        }
                        InteractionMode.MOVING_TARGET_BALL -> {
                            onEvent(MainScreenEvent.BallMoved(1, state.getLogicalPoint(currentPoint)))
                        }
                        else -> {}
                    }
                    lastTouchX_single = currentX
                    lastTouchY_single = currentY
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
                        lastTouchX_single = event.getX(newPointerActionIndex)
                        lastTouchY_single = event.getY(newPointerActionIndex)
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
        if (!state.isBankingMode) {
            val targetBallScreenPos = DrawingUtils.mapPoint(state.screenState.protractorUnit.targetBall.logicalPosition, state.pitchMatrix)
            interactionMode = if (DrawingUtils.distance(touchPoint, targetBallScreenPos) < draggableElementSlop) {
                InteractionMode.MOVING_TARGET_BALL
            } else {
                InteractionMode.ROTATING_AIM
            }
        } else {
            interactionMode = InteractionMode.NONE
        }
    }

    private fun OverlayState.getLogicalPoint(screenPoint: PointF): PointF {
        val logicalPoint = PointF(screenPoint.x, screenPoint.y)
        if (this.hasInverseMatrix) {
            val pointArray = floatArrayOf(logicalPoint.x, logicalPoint.y)
            this.inversePitchMatrix.mapPoints(pointArray)
            logicalPoint.x = pointArray[0]
            logicalPoint.y = pointArray[1]
        }
        return logicalPoint
    }
}
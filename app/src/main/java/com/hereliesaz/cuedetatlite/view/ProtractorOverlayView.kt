package com.hereliesaz.cuedetatlite.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetatlite.R
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.view.renderer.BallRenderer
import com.hereliesaz.cuedetatlite.view.renderer.LineRenderer
import com.hereliesaz.cuedetatlite.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetatlite.view.renderer.RailRenderer
import com.hereliesaz.cuedetatlite.view.renderer.TableRenderer
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ProtractorOverlayView(context: Context) : View(context) {

    private enum class InteractionMode {
        NONE, SCALING, ROTATING_PROTRACTOR, MOVING_CUE_BALL, AIMING_BANK_SHOT
    }

    // --- Renderer Setup ---
    private val paints = PaintCache()
    private val ballTextRenderer = BallTextRenderer()
    private val lineTextRenderer = LineTextRenderer()
    private val ballRenderer = BallRenderer(paints, ballTextRenderer)
    private val lineRenderer = LineRenderer(paints, lineTextRenderer)
    private val railRenderer = RailRenderer(paints)
    private val tableRenderer = TableRenderer(paints)
    private val renderer = OverlayRenderer(ballRenderer, lineRenderer, railRenderer, tableRenderer, paints)

    private var canonicalState = OverlayState()
    private var barbaroTypeface: Typeface? = null

    // --- Simplified Event Listener ---
    var onEvent: ((MainScreenEvent) -> Unit)? = null

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
                if (!gestureInProgress) { gestureInProgress = true }
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (interactionMode == InteractionMode.SCALING) {
                    onEvent?.invoke(MainScreenEvent.ZoomChanged(detector.scaleFactor))
                    return true
                }
                return false
            }
        }
        scaleGestureDetector = ScaleGestureDetector(context, scaleListener)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, canonicalState)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // onSizeChanged is no longer needed as the ViewModel can get this from the state
    }

    private fun getLogicalPoint(screenPoint: PointF): PointF {
        val logicalPoint = PointF(screenPoint.x, screenPoint.y)
        if (canonicalState.hasInverseMatrix) {
            val invertedMatrix = Matrix(canonicalState.inversePitchMatrix)
            val pointArray = floatArrayOf(logicalPoint.x, logicalPoint.y)
            invertedMatrix.mapPoints(pointArray)
            logicalPoint.x = pointArray[0]
            logicalPoint.y = pointArray[1]
        }
        return logicalPoint
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (canonicalState.isSpatiallyLocked && !scaleGestureDetector.isInProgress) return true

        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        if (scaleGestureDetector.isInProgress) return true

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!gestureInProgress) { gestureInProgress = true }
                activePointerId = event.getPointerId(0)
                lastTouchX_single = event.getX(0)
                lastTouchY_single = event.getY(0)
                determineSingleTouchMode(lastTouchX_single, lastTouchY_single)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val currentX = event.getX(event.findPointerIndex(activePointerId))
                    val currentY = event.getY(event.findPointerIndex(activePointerId))
                    val dx = currentX - lastTouchX_single
                    val currentPoint = PointF(currentX, currentY)

                    when (interactionMode) {
                        InteractionMode.ROTATING_PROTRACTOR -> {
                            onEvent?.invoke(MainScreenEvent.TableRotationChanged(canonicalState.tableRotationDegrees - (dx * 0.3f)))
                        }
                        InteractionMode.MOVING_CUE_BALL -> {
                            onEvent?.invoke(MainScreenEvent.BallMoved(0, getLogicalPoint(currentPoint))) // ID 0 for cue ball
                        }
                        InteractionMode.AIMING_BANK_SHOT -> {
                            onEvent?.invoke(MainScreenEvent.BankingAimTargetChanged(getLogicalPoint(currentPoint)))
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
                    activePointerId = event.getPointerId(newPointerActionIndex)
                    lastTouchX_single = event.getX(newPointerActionIndex)
                    lastTouchY_single = event.getY(newPointerActionIndex)
                }
            }
        }
        return true
    }

    private fun determineSingleTouchMode(touchX: Float, touchY: Float) {
        val touchPoint = PointF(touchX, touchY)
        interactionMode = if (canonicalState.isBankingMode) {
            InteractionMode.AIMING_BANK_SHOT
        } else {
            val cueBallScreenPos = DrawingUtils.mapPoint(canonicalState.screenState.protractorUnit.cueBall.logicalPosition, canonicalState.pitchMatrix)
            if (DrawingUtils.distance(touchPoint, cueBallScreenPos) < draggableElementSlop) {
                InteractionMode.MOVING_CUE_BALL
            } else {
                InteractionMode.ROTATING_PROTRACTOR
            }
        }
    }

    fun updateState(newState: OverlayState, systemIsDark: Boolean) {
        this.canonicalState = newState
        this.paints.updateColors(newState, systemIsDark)
        invalidate()
    }
}
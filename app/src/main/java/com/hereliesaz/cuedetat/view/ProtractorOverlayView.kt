package com.hereliesaz.cuedetat.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
class ProtractorOverlayView(context: Context) : View(context) {

    // MODIFIED: The renderer is no longer instantiated here. It is passed in.
    private val renderer = OverlayRenderer()
    private val paints = PaintCache() // The view now owns the paints
    private var state = OverlayState()

    var onSizeChanged: ((Int, Int) -> Unit)? = null
    var onRotationChange: ((Float) -> Unit)? = null
    var onUnitMove: ((PointF) -> Unit)? = null
    var onActualCueBallMoved: ((PointF) -> Unit)? = null

    private var lastTouchX = 0f
    private var pointerId = -1

    private enum class DragMode { NONE, ROTATE, MOVE_UNIT, MOVE_ACTUAL_CUE_BALL }

    private var dragMode = DragMode.NONE

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Delegate all drawing to the renderer, passing the current state and paints.
        renderer.draw(canvas, state, paints)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChanged?.invoke(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (state.viewWidth == 0 || !state.hasInverseMatrix) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = event.getPointerId(0)
                lastTouchX = event.x
                val touchPoint = PointF(event.x, event.y)

                val projectedTargetCenter =
                    renderer.mapPoint(state.protractorUnit.center, state.pitchMatrix)
                val projectedActualCueCenter = state.actualCueBall?.let {
                    renderer.mapPoint(it.center, state.pitchMatrix)
                }

                // MODIFIED: getPerspectiveRadiusAndLift call updated
                val touchRadius = renderer.getPerspectiveRadiusAndLift(
                    state.protractorUnit,
                    state,
                ).radius * 2.0f

                dragMode = when {
                    state.actualCueBall != null && projectedActualCueCenter != null && distance(
                        touchPoint,
                        projectedActualCueCenter
                    ) < touchRadius -> DragMode.MOVE_ACTUAL_CUE_BALL

                    distance(touchPoint, projectedTargetCenter) < touchRadius -> DragMode.MOVE_UNIT
                    else -> DragMode.ROTATE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId != -1) {
                    val pointerIndex = event.findPointerIndex(pointerId)
                    if (pointerIndex != -1) {
                        val newX = event.getX(pointerIndex)
                        val newY = event.getY(pointerIndex)

                        when (dragMode) {
                            DragMode.MOVE_UNIT -> onUnitMove?.invoke(PointF(newX, newY))
                            DragMode.MOVE_ACTUAL_CUE_BALL -> {
                                val logicalPos = FloatArray(2)
                                state.inversePitchMatrix.mapPoints(
                                    logicalPos,
                                    floatArrayOf(newX, newY)
                                )
                                onActualCueBallMoved?.invoke(
                                    PointF(
                                        logicalPos[0],
                                        logicalPos[1]
                                    )
                                )
                            }
                            DragMode.ROTATE -> {
                                val dx = newX - lastTouchX
                                lastTouchX = newX
                                val rotationDelta = -dx * 0.2f
                                onRotationChange?.invoke(state.protractorUnit.rotationDegrees + rotationDelta)
                            }

                            else -> {}
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pointerId = -1
                dragMode = DragMode.NONE
            }
        }
        return true
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx.pow(2) + dy.pow(2))
    }

    fun updateState(newState: OverlayState) {
        this.state = newState
        paints.updateColors(newState.dynamicColorScheme)
        invalidate()
    }
}

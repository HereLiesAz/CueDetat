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
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ProtractorOverlayView(context: Context) : View(context) {

    private enum class InteractionMode {
        NONE,
        SCALING,
        ROTATING,
        MOVING_UNIT,
        MOVING_CUE_BALL
    }

    private val renderer = OverlayRenderer()
    private val paints = PaintCache()

    // The canonical state from the ViewModel.
    private var canonicalState = OverlayState()

    // A mutable, local copy of the state for immediate drawing during gestures.
    private var drawableState = OverlayState()

    // This was the missing variable
    private var barbaroTypeface: Typeface? = null

    // Callbacks
    var onSizeChanged: ((Int, Int) -> Unit)? = null
    var onRotationChange: ((Float) -> Unit)? = null
    var onUnitMove: ((PointF) -> Unit)? = null
    var onActualCueBallMoved: ((PointF) -> Unit)? = null
    var onScale: ((Float) -> Unit)? = null

    // Gesture Handling
    private val scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var interactionMode = InteractionMode.NONE
    private var draggedObject = InteractionMode.NONE

    init {
        if (!isInEditMode) {
            barbaroTypeface = ResourcesCompat.getFont(context, R.font.barbaro)
            paints.setTypeface(barbaroTypeface)
        }
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                interactionMode = InteractionMode.SCALING
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onScale?.invoke(detector.scaleFactor)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                interactionMode = InteractionMode.NONE
            }
        }
        scaleGestureDetector = ScaleGestureDetector(context, listener)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Always draw using the local drawableState for immediate feedback.
        renderer.draw(canvas, drawableState, paints, barbaroTypeface)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChanged?.invoke(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (interactionMode == InteractionMode.SCALING) {
            return true
        }

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = touchX
                // Sync local state with canonical state at the beginning of any new gesture.
                drawableState = canonicalState.copy()

                val touchPoint = PointF(touchX, touchY)
                val unitScreenPos = DrawingUtils.mapPoint(
                    drawableState.protractorUnit.center,
                    drawableState.pitchMatrix
                )
                val unitRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                    drawableState.protractorUnit,
                    drawableState
                )

                if (DrawingUtils.distance(
                        touchPoint,
                        unitScreenPos
                    ) < unitRadiusInfo.radius + touchSlop
                ) {
                    interactionMode = InteractionMode.MOVING_UNIT
                    draggedObject = InteractionMode.MOVING_UNIT
                    return true
                }

                drawableState.actualCueBall?.let {
                    val cueBallScreenPos =
                        DrawingUtils.mapPoint(it.center, drawableState.pitchMatrix)
                    val cueBallRadiusInfo =
                        DrawingUtils.getPerspectiveRadiusAndLift(it, drawableState)
                    if (DrawingUtils.distance(
                            touchPoint,
                            cueBallScreenPos
                        ) < cueBallRadiusInfo.radius + touchSlop
                    ) {
                        interactionMode = InteractionMode.MOVING_CUE_BALL
                        draggedObject = InteractionMode.MOVING_CUE_BALL
                        return true
                    }
                }

                interactionMode = InteractionMode.ROTATING
                draggedObject = InteractionMode.ROTATING
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = touchX - lastTouchX

                when (interactionMode) {
                    InteractionMode.ROTATING -> {
                        if (abs(dx) > touchSlop) {
                            val newRotation =
                                drawableState.protractorUnit.rotationDegrees - (dx * 0.3f)
                            drawableState = drawableState.copy(
                                protractorUnit = drawableState.protractorUnit.copy(rotationDegrees = newRotation)
                            )
                            invalidate()
                            lastTouchX = touchX
                        }
                    }

                    InteractionMode.MOVING_UNIT -> {
                        val logicalPos = Perspective.screenToLogical(
                            PointF(touchX, touchY),
                            drawableState.inversePitchMatrix
                        )
                        drawableState = drawableState.copy(
                            protractorUnit = drawableState.protractorUnit.copy(center = logicalPos)
                        )
                        invalidate()
                    }

                    InteractionMode.MOVING_CUE_BALL -> {
                        if (drawableState.actualCueBall != null) {
                            val logicalPos = Perspective.screenToLogical(
                                PointF(touchX, touchY),
                                drawableState.inversePitchMatrix
                            )
                            drawableState = drawableState.copy(
                                actualCueBall = drawableState.actualCueBall?.copy(center = logicalPos)
                            )
                            invalidate()
                        }
                    }

                    else -> return true
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // When the gesture ends, send a single event to the ViewModel to update the canonical state.
                when (draggedObject) {
                    InteractionMode.ROTATING -> onRotationChange?.invoke(drawableState.protractorUnit.rotationDegrees)
                    InteractionMode.MOVING_UNIT -> onUnitMove?.invoke(PointF(touchX, touchY))
                    InteractionMode.MOVING_CUE_BALL -> onActualCueBallMoved?.invoke(
                        PointF(
                            touchX,
                            touchY
                        )
                    )

                    else -> { /* Do nothing */
                    }
                }
                interactionMode = InteractionMode.NONE
                draggedObject = InteractionMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun updateState(newState: OverlayState) {
        this.canonicalState = newState
        // Only update the drawable state if not in the middle of a gesture.
        if (interactionMode == InteractionMode.NONE) {
            this.drawableState = newState
            paints.updateColors(newState.dynamicColorScheme)
            invalidate()
        }
    }
}
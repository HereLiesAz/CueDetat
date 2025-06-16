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

    private var canonicalState = OverlayState()
    private var barbaroTypeface: Typeface? = null

    // Callbacks to the ViewModel
    var onSizeChanged: ((Int, Int) -> Unit)? = null
    var onRotationChange: ((Float) -> Unit)? = null
    var onUnitMove: ((PointF) -> Unit)? = null
    var onActualCueBallMoved: ((PointF) -> Unit)? = null
    var onScale: ((Float) -> Unit)? = null
    var onGestureStarted: (() -> Unit)? = null
    var onGestureEnded: (() -> Unit)? = null

    // Gesture Handling
    private val scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var interactionMode = InteractionMode.NONE

    init {
        if (!isInEditMode) {
            barbaroTypeface = ResourcesCompat.getFont(context, R.font.barbaro)
            paints.setTypeface(barbaroTypeface)
        }
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                onGestureStarted?.invoke()
                interactionMode = InteractionMode.SCALING
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onScale?.invoke(detector.scaleFactor)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                onGestureEnded?.invoke()
                interactionMode = InteractionMode.NONE
            }
        }
        scaleGestureDetector = ScaleGestureDetector(context, listener)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, canonicalState, paints, barbaroTypeface)
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
                onGestureStarted?.invoke()
                lastTouchX = touchX

                val touchPoint = PointF(touchX, touchY)
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
                    ) < unitRadiusInfo.radius + touchSlop
                ) {
                    interactionMode = InteractionMode.MOVING_UNIT
                    return true
                }

                canonicalState.actualCueBall?.let {
                    val cueBallScreenPos =
                        DrawingUtils.mapPoint(it.center, canonicalState.pitchMatrix)
                    val cueBallRadiusInfo =
                        DrawingUtils.getPerspectiveRadiusAndLift(it, canonicalState)
                    if (DrawingUtils.distance(
                            touchPoint,
                            cueBallScreenPos
                        ) < cueBallRadiusInfo.radius + touchSlop
                    ) {
                        interactionMode = InteractionMode.MOVING_CUE_BALL
                        return true
                    }
                }

                interactionMode = InteractionMode.ROTATING
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = touchX - lastTouchX

                when (interactionMode) {
                    InteractionMode.ROTATING -> {
                        if (abs(dx) > touchSlop) {
                            val newRotation =
                                canonicalState.protractorUnit.rotationDegrees - (dx * 0.3f)
                            onRotationChange?.invoke(newRotation)
                            lastTouchX = touchX
                        }
                    }
                    InteractionMode.MOVING_UNIT -> {
                        onUnitMove?.invoke(PointF(touchX, touchY))
                    }
                    InteractionMode.MOVING_CUE_BALL -> {
                        if (canonicalState.actualCueBall != null) {
                            onActualCueBallMoved?.invoke(PointF(touchX, touchY))
                        }
                    }
                    else -> return true
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onGestureEnded?.invoke()
                interactionMode = InteractionMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun updateState(newState: OverlayState) {
        this.canonicalState = newState
        paints.updateColors(newState.dynamicColorScheme)
        invalidate()
    }
}

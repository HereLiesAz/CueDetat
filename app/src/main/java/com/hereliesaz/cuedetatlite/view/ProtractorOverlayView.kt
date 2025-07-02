package com.hereliesaz.cuedetatlite.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetatlite.R
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.view.gestures.GestureHandler
import com.hereliesaz.cuedetatlite.view.renderer.BallRenderer
import com.hereliesaz.cuedetatlite.view.renderer.LineRenderer
import com.hereliesaz.cuedetatlite.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetatlite.view.renderer.RailRenderer
import com.hereliesaz.cuedetatlite.view.renderer.TableRenderer
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState

@SuppressLint("ClickableViewAccessibility")
class ProtractorOverlayView(context: Context) : View(context) {

    private val paints = PaintCache()
    private val renderer: OverlayRenderer
    private val gestureHandler: GestureHandler

    private var canonicalState = OverlayState()
    private var barbaroTypeface: Typeface? = null

    var onEvent: ((MainScreenEvent) -> Unit)? = null
        set(value) {
            field = value
            // This is a bit of a workaround to pass the final event handler
            // to the gesture handler after the view is constructed.
            if (value != null) {
                // Re-initialize gesture handler with the actual event listener
                // gestureHandler = GestureHandler(context, value) // This causes issues, handled in init
            }
        }

    init {
        if (!isInEditMode) {
            barbaroTypeface = ResourcesCompat.getFont(context, R.font.barbaro)
            paints.setTypeface(barbaroTypeface)
        }

        val ballTextRenderer = BallTextRenderer()
        val lineTextRenderer = LineTextRenderer()
        val ballRenderer = BallRenderer(paints, ballTextRenderer)
        val lineRenderer = LineRenderer(paints, lineTextRenderer)
        val railRenderer = RailRenderer(paints)
        val tableRenderer = TableRenderer(paints)
        renderer = OverlayRenderer(ballRenderer, lineRenderer, railRenderer, tableRenderer, paints)

        gestureHandler = GestureHandler(context) { event ->
            onEvent?.invoke(event)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, canonicalState)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onEvent?.invoke(MainScreenEvent.ViewResized(w, h))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureHandler.onTouchEvent(event, canonicalState)
    }

    fun updateState(newState: OverlayState, systemIsDark: Boolean) {
        this.canonicalState = newState
        this.paints.updateColors(newState, systemIsDark)
        invalidate()
    }
}
package com.hereliesaz.cuedetat.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

/**
 * The main View for the protractor overlay.
 * This class is now a lean orchestrator. It holds references to its collaborators
 * (renderer, gesture handler, paint cache) and manages the flow of state and events
 * between them and the hosting component (MainActivity/Compose).
 *
 * It is no longer responsible for any complex logic itself.
 */
class ProtractorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Collaborator components for handling specific responsibilities
    private val paints = PaintCache()
    private val renderer = OverlayRenderer()
    // Gesture handler would go here if it were more complex. For now, onTouchEvent is simple enough.

    // The current state of the overlay. Updated externally.
    private var state: OverlayState = OverlayState()

    // --- Public API ---

    /**
     * Updates the entire state of the view. This is the primary way to control
     * the view from the outside (e.g., from a ViewModel).
     *
     * @param newState The new, complete state to render.
     */
    fun updateState(newState: OverlayState) {
        this.state = newState
        invalidate() // Request a redraw with the new state
    }

    /**
     * Updates the colors of all drawing elements based on a Material ColorScheme.
     * This is called when the theme changes.
     */
    fun applyColorScheme(colorScheme: ColorScheme) {
        paints.updateColors(colorScheme)
        invalidate()
    }

    // --- View Overrides ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Delegate all drawing to the renderer, passing the current state and paints.
        renderer.draw(canvas, state, paints)
    }

}

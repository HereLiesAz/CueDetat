package com.hereliesaz.cuedetat.ar

import android.content.Context
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.ar.renderables.Ball
import com.hereliesaz.cuedetat.ar.renderables.TableNode
import com.hereliesaz.cuedetat.ui.state.UiState

class ARCoreRenderer(private val context: Context) {

    private lateinit var table: TableNode
    private lateinit var cueBall: Ball
    private lateinit var objectBall: Ball

    var uiState: UiState = UiState()

    fun onResume(session: Session) {
        // Session management will be handled here
    }

    fun onPause() {
        // Session pausing will be handled here
    }

    fun onSurfaceCreated() {
        table = TableNode(context)
        cueBall = Ball(context, color = floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f))
        objectBall = Ball(context, color = floatArrayOf(0.8f, 0.2f, 0.2f, 1.0f))
    }

    fun onDrawFrame() {
        // The rendering logic will be updated to use OpenXR
        // This will be driven by the uiState
    }
}
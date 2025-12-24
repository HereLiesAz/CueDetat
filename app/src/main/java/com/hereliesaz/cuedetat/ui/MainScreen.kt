package com.hereliesaz.cuedetat.ui

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MainScreen(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    activity: MainActivity?,
    glSurfaceView: GLSurfaceView?
) {
    MainLayout(uiState = uiState, onEvent = onEvent) {
        // This is where the rendering overlay goes.
        // For now, it's empty until we fix ProtractorOverlay and its dependencies.
    }
}

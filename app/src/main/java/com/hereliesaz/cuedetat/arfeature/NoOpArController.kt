package com.hereliesaz.cuedetat.arfeature

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Inert [ArController] used by [ArControllerFacade] before the Expert-AR module
 * is loaded (and permanently for un-entitled users). Reports no AR capability,
 * so the AR/scan UI in the base never composes and the per-frame hooks are
 * harmless no-ops.
 */
object NoOpArController : ArController {

    override fun probeCapability(): DepthCapability = DepthCapability.NONE

    override fun updateUiState(state: CueDetatState) {}

    override fun setTableZOffsetLogical(tableZOffsetLogical: Float) {}

    @Composable
    override fun ArBackground(modifier: Modifier, onEvent: (MainScreenEvent) -> Unit) {}

    override fun scanAnalyzer(): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { it.close() }

    @Composable
    override fun ScanOverlay(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit) {}

    override fun startManualHoleCapture() {}
}

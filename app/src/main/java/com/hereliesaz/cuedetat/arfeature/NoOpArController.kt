package com.hereliesaz.cuedetat.arfeature

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Inert [ArController] used while Expert AR is disabled. It reports no AR
 * capability and every interaction surface is a no-op.
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

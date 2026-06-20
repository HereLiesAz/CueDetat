// FILE: app/src/main/java/com/hereliesaz/cuedetat/arfeature/ArController.kt

package com.hereliesaz.cuedetat.arfeature

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Boundary for the Expert-only ARCore table-scan feature.
 *
 * Defined in the base module so the base never references ARCore or the AR
 * implementation classes (ArTableSession / ArFrameProcessor / ArCoreBackground /
 * TableScanViewModel) directly. That decoupling is what lets the implementation
 * eventually live in the on-demand `:feature_expert_ar` dynamic feature module,
 * delivered only to entitled (paid / trial) users.
 *
 * Step 1 of the extraction: the implementation (BaseArController) still lives in
 * the base and is Hilt-bound, so behaviour is unchanged. Step 2 moves the
 * implementation + ARCore into the dynamic feature module behind this same
 * interface, loaded via SplitInstall + reflection.
 */
interface ArController {

    /** Detect ARCore world-tracking capability (creates and closes a probe session). */
    fun probeCapability(): DepthCapability

    /** Push the latest derived UI state to the AR frame processor (ball detection input). */
    fun updateUiState(state: CueDetatState)

    /** Set the table-plane lift; input is the logical tableZOffset (converted to metres internally). */
    fun setTableZOffsetLogical(tableZOffsetLogical: Float)

    /** Full-screen ARCore camera background (corner capture during setup + 6DoF tracking). */
    @Composable
    fun ArBackground(modifier: Modifier, onEvent: (MainScreenEvent) -> Unit)

    /** CameraX analyzer that feeds the (non-AR) table-scan pocket detector. */
    fun scanAnalyzer(): ImageAnalysis.Analyzer

    /** The full-screen table-scan overlay UI. */
    @Composable
    fun ScanOverlay(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit)

    /** Begin a manual hole capture (driven from the nav menu). */
    fun startManualHoleCapture()
}

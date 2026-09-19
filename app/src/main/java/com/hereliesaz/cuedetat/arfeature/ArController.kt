// FILE: app/src/main/java/com/hereliesaz/cuedetat/arfeature/ArController.kt

package com.hereliesaz.cuedetat.arfeature

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Boundary reserved for the Expert AR/table-scan feature.
 *
 * Expert AR is currently disabled and excluded from the shipped application.
 * The base app binds this interface to [NoOpArController].
 */
interface ArController {

    /** Kept for API compatibility while AR is disabled. */
    suspend fun ensureLoaded(): Boolean = true

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

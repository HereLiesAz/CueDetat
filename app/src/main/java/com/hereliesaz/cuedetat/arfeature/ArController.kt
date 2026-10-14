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
 * delivered on demand the first time AR is requested.
 *
 * Step 1 of the extraction: the implementation (BaseArController) still lives in
 * the base and is Hilt-bound, so behaviour is unchanged. Step 2 moves the
 * implementation + ARCore into the dynamic feature module behind this same
 * interface, loaded via SplitInstall + reflection.
 */
interface ArController {

    /**
     * Ensure the Expert-AR implementation is available, requesting the on-demand
     * `:feature_expert_ar` split if necessary. Available to everyone.
     * Implemented by the
     * facade; the loaded implementation and the no-op both use the default.
     */
    suspend fun ensureLoaded(): Boolean = true

    /** Why the last [ensureLoaded] returned false, short enough to show the user; null if unknown. */
    val lastLoadError: String? get() = null

    /** Detect ARCore world-tracking capability (creates and closes a probe session). */
    fun probeCapability(): DepthCapability

    /** Push state to AR ball detection and the editable world-anchored table preview. */
    fun updateUiState(state: CueDetatState)

    /** Set the table-plane lift; input is the logical tableZOffset (converted to metres internally). */
    fun setTableZOffsetLogical(tableZOffsetLogical: Float)

    /**
     * Anchor the virtual table in the AR world. [screenCorners] are where its corners sit on
     * screen now and [logicalCorners] the same corners in logical space, both TL, TR, BR, BL.
     * The result comes back as [MainScreenEvent.ArTableLockResult].
     */
    fun lockTable(screenCorners: List<android.graphics.PointF>, logicalCorners: List<android.graphics.PointF>)

    /** Release the locked table. */
    fun unlockTable()

    /** Explicit, consent-confirmed cloud hosting; no automatic visual-map upload. */
    fun saveCloudPlacement() {}

    /** Explicit cloud restoration using the selected table's logical corners. */
    fun restoreCloudPlacement(logicalCorners: List<android.graphics.PointF>) {}

    /** Remove the saved local Cloud Anchor ID, not Google's already-hosted record. */
    fun forgetCloudPlacement()

    /** Full-screen ARCore camera background (corner capture during setup + 6DoF tracking). */
    @Composable
    fun ArBackground(modifier: Modifier, onEvent: (MainScreenEvent) -> Unit)

    /** CameraX analyzer that feeds the (non-AR) table-scan pocket detector. */
    fun scanAnalyzer(): ImageAnalysis.Analyzer

    /** The full-screen table-scan overlay UI. */
    @Composable
    fun ScanOverlay(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit)
}

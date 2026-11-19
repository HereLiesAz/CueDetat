// FILE: app/src/main/java/com/hereliesaz/cuedetat/arfeature/ArController.kt

package com.hereliesaz.cuedetat.arfeature

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.domain.ArCoreStatus
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Boundary for the Expert-only ARCore table-scan feature.
 *
 * Keeps the rest of the app from referencing the AR implementation classes
 * (ArTableSession / ArFrameProcessor / ArCoreBackground / TableScanViewModel) directly.
 * The implementation, `ArControllerImpl`, lives under `feature_expert_ar/`, which is a
 * source directory compiled into :app (app/build.gradle.kts sourceSets), not a Gradle or
 * dynamic-feature module. ArControllerFacade constructs it directly; there is
 * no SplitInstall, reflection or on-demand delivery.
 */
interface ArController {

    /**
     * Ensure the Expert-AR implementation is available (in the single bundled build
     * this only loads the compiled-in class). Available to everyone.
     * Implemented by the
     * facade; the loaded implementation and the no-op both use the default.
     */
    suspend fun ensureLoaded(): Boolean = true

    /** Why the last [ensureLoaded] returned false, short enough to show the user; null if unknown. */
    val lastLoadError: String? get() = null

    /** Detect ARCore world-tracking capability (creates and closes a probe session). */
    fun probeCapability(): DepthCapability

    /**
     * Whether Google Play Services for AR is installed and current. May wait a moment (up to about a
     * second) for Google Play to answer on the first call, so call it off the main thread. Never
     * throws; [ArCoreStatus.UNKNOWN] when it cannot tell.
     */
    fun arCoreStatus(): ArCoreStatus = ArCoreStatus.UNKNOWN

    /**
     * Ask Google Play to install or update Google Play Services for AR. Main thread, with the
     * foreground [activity]: Play takes over the screen, and the Activity resumes when it is done,
     * after which [arCoreStatus] has the answer. Never throws.
     */
    fun requestArCoreInstall(activity: android.app.Activity): ArCoreStatus = ArCoreStatus.UNKNOWN

    /** Push state to AR ball detection and the editable world-anchored table preview. */
    fun updateUiState(state: CueDetatState)

    /** Set the table-plane lift; input is the logical tableZOffset (converted to metres internally). */
    fun setTableZOffsetLogical(tableZOffsetLogical: Float)

    /**
     * Move the unlocked AR table toward ([factor] > 1) or away from the camera by that ratio of
     * its apparent size, along the camera's line. A relative nudge, not an absolute zoom: the
     * session clamps the resulting range itself, so a control driving this can never saturate.
     * Ignored while the table is locked.
     */
    fun nudgeTableRange(factor: Float) {}

    /**
     * Lay the unlocked AR table the other way, a quarter turn about its centre, so its sides meet
     * the real edges across rather than along. Ignored while the table is locked.
     */
    fun turnTableQuarter() {}

    /**
     * Anchor the virtual table in the AR world. [screenCorners] are where its corners sit on
     * screen now and [logicalCorners] the same corners in logical space, both TL, TR, BR, BL.
     * The result comes back as [MainScreenEvent.ArTableLockResult].
     */
    fun lockTable(screenCorners: List<android.graphics.PointF>, logicalCorners: List<android.graphics.PointF>)

    /** Release the locked table. */
    fun unlockTable()

    /**
     * Where memory says the table lies, for the first placement of the AR preview: given the phone's
     * heading and pitch (degrees), the remembered table rotation (clockwise from the way the camera
     * faces) with how far to trust it, or null. Set once; the AR side reads it when it places the
     * preview, so it need not be re-set when the phone turns.
     */
    fun setTableOrientationPrior(prior: ((yawDeg: Float, pitchDeg: Float) -> com.hereliesaz.cuedetat.domain.TableOrientationLearner.Prediction?)?) {}

    /**
     * The locked table's heading in clockwise degrees from the way the camera faces now, or null
     * when the table is not locked or the camera has not been seen yet. What a lock should remember:
     * the on-screen rotation the app keeps does not track the AR table.
     */
    fun lockedTableHeadingDegrees(): Float? = null

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

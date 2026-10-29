package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.reducers.reduceControlAction
import com.hereliesaz.cuedetat.domain.reducers.reduceCvAction
import com.hereliesaz.cuedetat.domain.reducers.reduceToggleAction
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArFlowReducerTest {

    private val base = CueDetatState()
    private val utils = ReducerUtils()

    @Test
    fun `CycleCameraMode from OFF transitions to AR_SETUP`() {
        val s = base.copy(cameraMode = CameraMode.OFF)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.AR_SETUP, result.cameraMode)
    }

    @Test
    fun `CycleCameraMode from CAMERA_ONLY turns off`() {
        val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.OFF, result.cameraMode)
    }

    @Test
    fun `CycleCameraMode from AR_SETUP turns off`() {
        val s = base.copy(cameraMode = CameraMode.AR_SETUP)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.OFF, result.cameraMode)
    }

    @Test
    fun `CycleCameraMode from AR_ACTIVE transitions to LITE_AR`() {
        // ToggleReducer:46 — AR_ACTIVE deliberately drops to LITE_AR rather than OFF
        // so users keep a usable camera view after AR session ends.
        val s = base.copy(cameraMode = CameraMode.AR_ACTIVE)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.LITE_AR, result.cameraMode)
    }

    @Test
    fun `CancelArSetup transitions to CAMERA_ONLY`() {
        val s = base.copy(cameraMode = CameraMode.AR_SETUP)
        val result = reduceToggleAction(s, MainScreenEvent.CancelArSetup, utils)
        assertEquals(CameraMode.CAMERA_ONLY, result.cameraMode)
    }

    @Test
    fun `TurnCameraOff transitions to OFF`() {
        val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
        val result = reduceToggleAction(s, MainScreenEvent.TurnCameraOff, utils)
        assertEquals(CameraMode.OFF, result.cameraMode)
    }

    @Test
    fun `CvDataUpdated auto-advances to AR_ACTIVE when all conditions met`() {
        val tps = TpsWarpData(
            srcPoints = listOf(PointF(0f, 0f)),
            dstPoints = listOf(PointF(1f, 1f))
        )
        val scan = TableScanModel(
            pockets = emptyList(),
            lensWarpTps = tps,
            tableSize = com.hereliesaz.cuedetat.view.state.TableSize.EIGHT_FT,
            feltColorHsv = listOf(0f, 0f, 0f),
            scanLatitude = null,
            scanLongitude = null
        )
        val s = base.copy(
            cameraMode = CameraMode.AR_SETUP,
            lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f),
            tableScanModel = scan
        )
        val visionData = VisionData(tableOverlayConfidence = 0.9f)
        val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(visionData))
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
    }

    @Test
    fun `CvDataUpdated does NOT auto-advance when confidence is low`() {
        val tps = TpsWarpData(
            srcPoints = listOf(PointF(0f, 0f)),
            dstPoints = listOf(PointF(1f, 1f))
        )
        val scan = TableScanModel(
            pockets = emptyList(),
            lensWarpTps = tps,
            tableSize = com.hereliesaz.cuedetat.view.state.TableSize.EIGHT_FT,
            feltColorHsv = listOf(0f, 0f, 0f),
            scanLatitude = null,
            scanLongitude = null
        )
        val s = base.copy(
            cameraMode = CameraMode.AR_SETUP,
            lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f),
            tableScanModel = scan
        )
        val visionData = VisionData(tableOverlayConfidence = 0.5f)
        val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(visionData))
        assertEquals(CameraMode.AR_SETUP, result.cameraMode)
    }

    // --- On-demand Expert-AR module load lifecycle (arModuleState) ---

    @Test
    fun `arModuleState defaults to IDLE`() {
        assertEquals(ArModuleState.IDLE, base.arModuleState)
    }

    @Test
    fun `ArModuleLoadStarted sets arModuleState to LOADING`() {
        val result = reduceControlAction(base, MainScreenEvent.ArModuleLoadStarted)
        assertEquals(ArModuleState.LOADING, result.arModuleState)
    }

    @Test
    fun `ArModuleLoadSucceeded sets arModuleState to READY`() {
        val s = base.copy(arModuleState = ArModuleState.LOADING)
        val result = reduceControlAction(s, MainScreenEvent.ArModuleLoadSucceeded)
        assertEquals(ArModuleState.READY, result.arModuleState)
    }

    @Test
    fun `ArModuleLoadFailed sets arModuleState to FAILED`() {
        val s = base.copy(arModuleState = ArModuleState.LOADING)
        val result = reduceControlAction(s, MainScreenEvent.ArModuleLoadFailed("boom"))
        assertEquals(ArModuleState.FAILED, result.arModuleState)
    }

    @Test
    fun `module load events do not disturb camera mode`() {
        // The load lifecycle runs while the user sits in AR_SETUP; the transition
        // must not clobber the camera mode the reducer already put them in.
        val s = base.copy(cameraMode = CameraMode.AR_SETUP)
        val started = reduceControlAction(s, MainScreenEvent.ArModuleLoadStarted)
        assertEquals(CameraMode.AR_SETUP, started.cameraMode)
        val ready = reduceControlAction(started, MainScreenEvent.ArModuleLoadSucceeded)
        assertEquals(CameraMode.AR_SETUP, ready.cameraMode)
    }

    @Test
    fun `retry path FAILED then LOADING is representable`() {
        // After a failed load, starting again returns to LOADING (the UI's Retry
        // re-runs ensureArModuleLoaded, which dispatches ArModuleLoadStarted).
        val failed = base.copy(arModuleState = ArModuleState.FAILED)
        val result = reduceControlAction(failed, MainScreenEvent.ArModuleLoadStarted)
        assertEquals(ArModuleState.LOADING, result.arModuleState)
    }
}

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
    fun `ArTrackingLost preserves AR session state (float on last known matrix)`() {
        // ControlReducer:116 — ArTrackingLost is a deliberate no-op; the app floats on
        // the last known matrix rather than tearing down the AR session whenever the
        // tracker hiccups. Test guards against accidentally restoring the old
        // "nuclear payload" behaviour.
        val tps = TpsWarpData(
            srcPoints = listOf(PointF(0f, 0f)),
            dstPoints = listOf(PointF(1f, 1f))
        )
        val scan = TableScanModel(
            pockets = emptyList(),
            lensWarpTps = tps,
            tableSize = TableSize.EIGHT_FT,
            feltColorHsv = listOf(0f, 0f, 0f),
            scanLatitude = null,
            scanLongitude = null
        )
        val s = base.copy(
            cameraMode = CameraMode.AR_ACTIVE,
            tableScanModel = scan,
            lensWarpTps = scan.lensWarpTps
        )
        val result = reduceControlAction(s, MainScreenEvent.ArTrackingLost)
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
        assertEquals(scan, result.tableScanModel)
        assertEquals(scan.lensWarpTps, result.lensWarpTps)
    }

    @Test
    fun `ArTrackingLost when not AR_ACTIVE does not change cameraMode`() {
        val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
        val result = reduceControlAction(s, MainScreenEvent.ArTrackingLost)
        assertEquals(CameraMode.CAMERA_ONLY, result.cameraMode)
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
}

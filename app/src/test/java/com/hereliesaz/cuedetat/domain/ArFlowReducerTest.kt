package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.reducers.reduceControlAction
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
    fun `CycleCameraMode from CAMERA_ONLY transitions to AR_SETUP`() {
        val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.AR_SETUP, result.cameraMode)
    }

    @Test
    fun `CycleCameraMode from AR_SETUP is a no-op`() {
        val s = base.copy(cameraMode = CameraMode.AR_SETUP)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.AR_SETUP, result.cameraMode)
    }

    @Test
    fun `CycleCameraMode from AR_ACTIVE is a no-op`() {
        val s = base.copy(cameraMode = CameraMode.AR_ACTIVE)
        val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
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
    fun `ArTrackingLost clears tableScanModel and lensWarpTps and returns to AR_SETUP from AR_ACTIVE`() {
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
        assertEquals(CameraMode.AR_SETUP, result.cameraMode)
        assertNull(result.tableScanModel)
        assertNull(result.lensWarpTps)
    }

    @Test
    fun `ArTrackingLost when not AR_ACTIVE does not change cameraMode`() {
        val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
        val result = reduceControlAction(s, MainScreenEvent.ArTrackingLost)
        assertEquals(CameraMode.CAMERA_ONLY, result.cameraMode)
    }
}

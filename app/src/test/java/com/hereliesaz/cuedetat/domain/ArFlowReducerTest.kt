package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.reducers.reduceToggleAction
import org.junit.Assert.assertEquals
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
}

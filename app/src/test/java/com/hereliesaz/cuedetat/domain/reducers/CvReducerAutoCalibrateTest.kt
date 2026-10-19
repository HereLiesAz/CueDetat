package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.Rect
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The auto-tune (low-light) loop behind the Advanced dialog's button: it needs no locked felt
 * colour, lowers the Canny thresholds one step per empty frame, and must stop when the
 * dialog closes or the camera goes off.
 */
class CvReducerAutoCalibrateTest {

    private val base = CueDetatState()
    private val utils = ReducerUtils()
    private val empty = MainScreenEvent.CvDataUpdated(VisionData())

    @Test
    fun `AutoCalibrateCv starts without a locked colour`() {
        assertNull(base.lockedHsvColor)
        assertTrue(reduceCvAction(base, MainScreenEvent.AutoCalibrateCv).isAutoCalibrating)
    }

    @Test
    fun `an empty frame lowers both thresholds by their steps`() {
        val s = reduceCvAction(base, MainScreenEvent.AutoCalibrateCv)
        val out = reduceCvAction(s, empty)
        // Defaults 40 / 120; steps 5 / 10 are literals here, not read from the reducer.
        assertEquals(35f, out.cannyThreshold1, 0f)
        assertEquals(110f, out.cannyThreshold2, 0f)
        assertTrue(out.isAutoCalibrating)
    }

    @Test
    fun `a frame with a detection ends the loop and keeps the thresholds`() {
        val s = reduceCvAction(base, MainScreenEvent.AutoCalibrateCv)
        val seen = MainScreenEvent.CvDataUpdated(VisionData(detectedBoundingBoxes = listOf(Rect())))
        val out = reduceCvAction(s, seen)
        assertFalse(out.isAutoCalibrating)
        assertEquals(40f, out.cannyThreshold1, 0f)
        assertEquals(120f, out.cannyThreshold2, 0f)
    }

    @Test
    fun `ten empty frames hit the floor and fail with a warning`() {
        // T2 falls 120 -> 20 in ten steps of 10; T1 reaches its floor of 10 after six.
        var s = reduceCvAction(base, MainScreenEvent.AutoCalibrateCv)
        repeat(9) { s = reduceCvAction(s, empty) }
        assertTrue(s.isAutoCalibrating)
        s = reduceCvAction(s, empty)
        assertFalse(s.isAutoCalibrating)
        assertEquals(10f, s.cannyThreshold1, 0f)
        assertEquals(20f, s.cannyThreshold2, 0f)
        assertNotNull(s.warningText)
    }

    @Test
    fun `closing the Advanced dialog stops a running auto-tune`() {
        val running = base.copy(showAdvancedOptionsDialog = true, isAutoCalibrating = true)
        val out = reduceAdvancedOptionsAction(running, MainScreenEvent.ToggleAdvancedOptionsDialog)
        assertFalse(out.showAdvancedOptionsDialog)
        assertFalse(out.isAutoCalibrating)
    }

    @Test
    fun `opening the Advanced dialog does not start or stop anything`() {
        val out = reduceAdvancedOptionsAction(base, MainScreenEvent.ToggleAdvancedOptionsDialog)
        assertTrue(out.showAdvancedOptionsDialog)
        assertFalse(out.isAutoCalibrating)
    }

    @Test
    fun `turning the camera off stops a running auto-tune`() {
        val running = base.copy(cameraMode = CameraMode.AR_ACTIVE, isAutoCalibrating = true)
        assertFalse(reduceToggleAction(running, MainScreenEvent.TurnCameraOff, utils).isAutoCalibrating)
    }

    @Test
    fun `cycling the camera off stops a running auto-tune`() {
        val running = base.copy(cameraMode = CameraMode.CAMERA_ONLY, isAutoCalibrating = true)
        val out = reduceToggleAction(running, MainScreenEvent.CycleCameraMode, utils)
        assertEquals(CameraMode.OFF, out.cameraMode)
        assertFalse(out.isAutoCalibrating)
    }

    @Test
    fun `rescan from AR_ACTIVE resets to a blank AR_SETUP scan`() {
        val s = base.copy(
            cameraMode = CameraMode.AR_ACTIVE,
            lockedHsvColor = floatArrayOf(60f, 100f, 100f),
            isArTableLocked = true,
        )
        val out = reduceToggleAction(s, MainScreenEvent.ToggleTableScanScreen, utils)
        assertEquals(CameraMode.AR_SETUP, out.cameraMode)
        assertTrue(out.showTableScanScreen)
        assertNull(out.lockedHsvColor)
        assertNull(out.tableScanModel)
        assertFalse(out.isArTableLocked)
    }

    @Test
    fun `toggling the scan overlay while it is showing only hides it`() {
        val s = base.copy(cameraMode = CameraMode.AR_SETUP, showTableScanScreen = true)
        val out = reduceToggleAction(s, MainScreenEvent.ToggleTableScanScreen, utils)
        assertEquals(CameraMode.AR_SETUP, out.cameraMode)
        assertFalse(out.showTableScanScreen)
    }
}

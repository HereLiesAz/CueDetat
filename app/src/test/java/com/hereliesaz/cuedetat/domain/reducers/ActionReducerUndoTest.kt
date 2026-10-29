package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/** Undo/redo must restore game/aim fields only, never live device/view state. */
class ActionReducerUndoTest {

    private val utils = ReducerUtils()

    @Test
    fun `undo keeps live view size and camera mode, restores zoom`() {
        val before = CueDetatState().copy(
            viewWidth = 1080, viewHeight = 2400, cameraMode = CameraMode.OFF,
            zoomSliderPosition = 25f
        )
        val afterReset = reduceAction(before, MainScreenEvent.Reset, utils)
        // Device rotates and the camera turns on after the reset.
        val live = afterReset.copy(viewWidth = 2400, viewHeight = 1080, cameraMode = CameraMode.CAMERA_ONLY)

        val undone = reduceAction(live, MainScreenEvent.Reset, utils)

        assertEquals(2400, undone.viewWidth)
        assertEquals(1080, undone.viewHeight)
        assertEquals(CameraMode.CAMERA_ONLY, undone.cameraMode)
        assertEquals(25f, undone.zoomSliderPosition, 0f)

        val redone = reduceAction(undone.copy(viewWidth = 720), MainScreenEvent.Reset, utils)
        assertEquals(720, redone.viewWidth)
        assertEquals(afterReset.zoomSliderPosition, redone.zoomSliderPosition, 0f)
    }

    @Test
    fun `reset after rearranging takes a fresh snapshot instead of toggling the first two layouts`() {
        val start = CueDetatState().copy(zoomSliderPosition = 25f)
        val reset1 = reduceAction(start, MainScreenEvent.Reset, utils)
        val undone = reduceAction(reset1, MainScreenEvent.Reset, utils)
        assertEquals(25f, undone.zoomSliderPosition, 0f)

        // The user rearranges, then resets: this must reset (and remember the new layout),
        // not redo the first reset.
        val rearranged = undone.copy(zoomSliderPosition = -30f, valuesChangedSinceReset = true)
        val reset2 = reduceAction(rearranged, MainScreenEvent.Reset, utils)
        assertEquals(reset1.zoomSliderPosition, reset2.zoomSliderPosition, 0f)
        assertEquals(-30f, reset2.preResetState!!.zoomSliderPosition, 0f)

        // And undoing it brings the rearranged layout back, not the very first one.
        val undone2 = reduceAction(reset2, MainScreenEvent.Reset, utils)
        assertEquals(-30f, undone2.zoomSliderPosition, 0f)
    }

    @Test
    fun `undo and redo leave the restored layout as the unchanged baseline`() {
        val reset = reduceAction(CueDetatState().copy(valuesChangedSinceReset = true), MainScreenEvent.Reset, utils)
        val undone = reduceAction(reset, MainScreenEvent.Reset, utils)
        assertEquals(false, undone.valuesChangedSinceReset)
        assertEquals(false, reduceAction(undone, MainScreenEvent.Reset, utils).valuesChangedSinceReset)
    }

    @Test
    fun `the table snapping itself back into place does not make undo unreachable`() {
        // Reset, then the felt-fit pull (ApplyTablePose) and the CV pose blend (UpdateArPose) move
        // the table on their own: the user changed nothing, so Reset must still undo.
        val start = CueDetatState().copy(zoomSliderPosition = 25f)
        val reset = reduceAction(start, MainScreenEvent.Reset, utils)
        val pulled = applyTablePose(reset, 10f, 10f, 5f, 1f)
        assertEquals(false, pulled.valuesChangedSinceReset)
        val blended = reduceControlAction(
            pulled,
            MainScreenEvent.UpdateArPose(androidx.compose.ui.geometry.Offset(1f, 1f), 2f, 1f),
        )
        assertEquals(false, blended.valuesChangedSinceReset)
        val undone = reduceAction(blended, MainScreenEvent.Reset, utils)
        assertEquals(25f, undone.zoomSliderPosition, 0f)
    }
}

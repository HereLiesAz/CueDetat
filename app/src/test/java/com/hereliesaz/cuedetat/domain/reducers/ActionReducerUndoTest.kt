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
}

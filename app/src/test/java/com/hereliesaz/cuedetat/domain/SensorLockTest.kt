package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.domain.reducers.reduceControlAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorLockTest {

    @Test
    fun `sensor-driven events are blocked`() {
        assertTrue(SensorLock.blocks(MainScreenEvent.FullOrientationChanged(FullOrientation(10f, 0f, 0f))))
        assertTrue(SensorLock.blocks(MainScreenEvent.ApplyTablePose(0f, 0f, 30f, 1f)))
        assertTrue(SensorLock.blocks(MainScreenEvent.TableFitUpdated(null)))
    }

    @Test
    fun `manual controls are not blocked`() {
        assertFalse(SensorLock.blocks(MainScreenEvent.TableRotationApplied(5f)))
        assertFalse(SensorLock.blocks(MainScreenEvent.ZoomSliderChanged(3f)))
        assertFalse(SensorLock.blocks(MainScreenEvent.MoveTableZ(1f)))
        assertFalse(SensorLock.blocks(MainScreenEvent.PanView(PointF(1f, 1f))))
        assertFalse(SensorLock.blocks(MainScreenEvent.ToggleViewLock))
    }

    @Test
    fun `toggling the lock drops the compass reference`() {
        val locked = reduceControlAction(CueDetatState(compassRefYaw = 40f), MainScreenEvent.ToggleViewLock)
        assertTrue(locked.isViewLocked)
        assertNull(locked.compassRefYaw)
        assertFalse(reduceControlAction(locked, MainScreenEvent.ToggleViewLock).isViewLocked)
    }
}

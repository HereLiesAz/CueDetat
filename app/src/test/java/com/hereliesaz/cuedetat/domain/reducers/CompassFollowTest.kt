package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.domain.CueDetatState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompassFollowTest {

    private fun at(state: CueDetatState, yaw: Float) =
        followCompass(state.copy(currentOrientation = FullOrientation(yaw, 0f, 0f)))

    @Test
    fun `first heading only sets the reference`() {
        val s = at(CueDetatState(worldRotationDegrees = 10f), 50f)
        assertEquals(10f, s.worldRotationDegrees, 1e-3f)
        assertEquals(50f, s.compassRefYaw!!, 1e-3f)
    }

    @Test
    fun `turning the phone right turns the table left`() {
        val s = at(at(CueDetatState(worldRotationDegrees = 10f), 0f), 30f)
        assertEquals(-20f, s.worldRotationDegrees, 1e-3f)
    }

    @Test
    fun `crossing south takes the short way round`() {
        // 170 -> -170 is a 20 degree turn right, not 340 left.
        val s = at(at(CueDetatState(worldRotationDegrees = 0f), 170f), -170f)
        assertEquals(-20f, s.worldRotationDegrees, 1e-3f)
    }

    @Test
    fun `jitter under the deadband accumulates instead of vanishing`() {
        var s = at(CueDetatState(worldRotationDegrees = 0f), 0f)
        s = at(s, 0.3f)
        assertEquals(0f, s.worldRotationDegrees, 1e-3f)
        s = at(s, 0.6f)
        assertEquals(-0.6f, s.worldRotationDegrees, 1e-3f)
    }

    @Test
    fun `locked AR table ignores the compass and drops the reference`() {
        val s = at(at(CueDetatState(worldRotationDegrees = 10f), 0f).copy(isArTableLocked = true), 40f)
        assertEquals(10f, s.worldRotationDegrees, 1e-3f)
        assertNull(s.compassRefYaw)
    }

    @Test
    fun `compass turns the table but not the user's rotation`() {
        val s = at(at(CueDetatState(worldRotationDegrees = 10f, userRotationDegrees = 10f), 0f), 30f)
        assertEquals(-20f, s.worldRotationDegrees, 1e-3f)
        assertEquals(10f, s.userRotationDegrees, 1e-3f)
    }

    @Test
    fun `hand rotation moves both`() {
        val s = reduceControlAction(
            CueDetatState(worldRotationDegrees = -20f, userRotationDegrees = 10f),
            com.hereliesaz.cuedetat.domain.MainScreenEvent.TableRotationApplied(5f),
        )
        assertEquals(-15f, s.worldRotationDegrees, 1e-3f)
        assertEquals(15f, s.userRotationDegrees, 1e-3f)
    }
}

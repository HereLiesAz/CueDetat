package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CvReducerRelocaliserTest {

    @Test
    fun forceArActiveBypassesConfidenceRamp() {
        val state = CueDetatState(
            cameraMode = CameraMode.AR_SETUP,
            arConfidenceHistory = listOf(0.1f, 0.2f)
        )
        val result = reduceCvAction(state, MainScreenEvent.ForceArActive)
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
        assertEquals(0, result.arConfidenceHistory.size)
        assertEquals(0, result.arLowConfidenceFrameCount)
        assertEquals(0, result.relocaliserAttemptFrames)
        assertNull(result.relocaliserDeltaQ)
    }

    @Test
    fun forceArActiveIsNoOpWhenAlreadyActive() {
        val state = CueDetatState(cameraMode = CameraMode.AR_ACTIVE)
        val result = reduceCvAction(state, MainScreenEvent.ForceArActive)
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
    }

    @Test
    fun seedRelocaliserStoresDelta() {
        val delta = floatArrayOf(0.1f, 0.2f, 0.3f, 0.9f)
        val state = CueDetatState()
        val result = reduceCvAction(state, MainScreenEvent.SeedRelocaliser(delta))
        assertArrayEquals(delta, result.relocaliserDeltaQ, 0.001f)
        assertEquals(0, result.relocaliserAttemptFrames)
    }

    @Test
    fun seedRelocaliserWithNullClearsDelta() {
        val state = CueDetatState(relocaliserDeltaQ = floatArrayOf(0.1f, 0f, 0f, 1f))
        val result = reduceCvAction(state, MainScreenEvent.SeedRelocaliser(null))
        assertNull(result.relocaliserDeltaQ)
        assertEquals(0, result.relocaliserAttemptFrames)
    }
}

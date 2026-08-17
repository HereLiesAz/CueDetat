package com.hereliesaz.cuedetat.data

import com.hereliesaz.cuedetat.domain.StrokeSessionTracker
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Tests for the wire-format parsing and stroke/baseline bookkeeping used by
 * [WristWearableRepository]. These exercise [WristWearableRepository.parseImuPayload]
 * and the top-level [processImuSample] directly rather than constructing
 * [WristWearableRepository] itself, since that class depends on an Android
 * [android.content.Context] and Play Services' [com.google.android.gms.wearable.Wearable]
 * which aren't available/meaningful in a plain JVM unit test.
 */
class WristWearableRepositoryTest {

    private fun buildImuPayload(
        time: Long,
        ax: Float, ay: Float, az: Float,
        gx: Float, gy: Float, gz: Float
    ): ByteArray {
        val buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(time)
        buffer.putFloat(ax)
        buffer.putFloat(ay)
        buffer.putFloat(az)
        buffer.putFloat(gx)
        buffer.putFloat(gy)
        buffer.putFloat(gz)
        return buffer.array()
    }

    // --- parseImuPayload ---

    @Test
    fun `parses a known 32-byte payload into timestamp and 6 floats`() {
        val payload = buildImuPayload(
            time = 123_456_789L,
            ax = 1.5f, ay = -2.25f, az = 0.75f,
            gx = 3.0f, gy = -1.0f, gz = 0.125f
        )

        val profile = WristWearableRepository.parseImuPayload(payload)

        assertNotNull(profile)
        assertEquals(123_456_789L, profile!!.time)
        assertArrayEquals(floatArrayOf(1.5f, -2.25f, 0.75f), profile.acceleration, 0.0001f)
        assertArrayEquals(floatArrayOf(3.0f, -1.0f, 0.125f), profile.gyroscope, 0.0001f)
    }

    @Test
    fun `returns null for a payload smaller than 32 bytes`() {
        val shortPayload = ByteArray(16)
        assertNull(WristWearableRepository.parseImuPayload(shortPayload))
    }

    @Test
    fun `ignores trailing bytes beyond the first 32`() {
        val payload = buildImuPayload(
            time = 42L,
            ax = 1f, ay = 2f, az = 3f,
            gx = 4f, gy = 5f, gz = 6f
        ) + ByteArray(8)

        val profile = WristWearableRepository.parseImuPayload(payload)

        assertNotNull(profile)
        assertEquals(42L, profile!!.time)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), profile.acceleration, 0.0001f)
    }

    // --- processImuSample / baseline population from a sequence of stroke messages ---

    @Test
    fun `first stroke worth of messages populates the baseline`() {
        val tracker = StrokeSessionTracker(windowSize = 5)
        var state = WearableState()

        repeat(5) { i ->
            val payload = buildImuPayload(
                time = i.toLong(), ax = 0f, ay = 1f + i * 0.1f, az = 0f, gx = 0f, gy = 0f, gz = 0f
            )
            state = processImuSample(payload, tracker, state)
        }

        assertTrue(tracker.hasBaseline)
        assertEquals(5, tracker.baselineProfiles.size)
        // The calibration stroke itself is reported as a perfect match.
        assertEquals(1.0f, state.consistencyScore, 0.001f)
        assertFalse(state.isShaking)
    }

    @Test
    fun `a divergent second stroke is scored against the captured baseline`() {
        val tracker = StrokeSessionTracker(windowSize = 5)
        var state = WearableState()

        // Baseline (smooth) stroke.
        repeat(5) { i ->
            val payload = buildImuPayload(
                time = i.toLong(), ax = 0f, ay = 1f + i * 0.1f, az = 0f, gx = 0f, gy = 0f, gz = 0f
            )
            state = processImuSample(payload, tracker, state)
        }
        val baselineAfterFirstStroke = tracker.baselineProfiles

        // Second, erratic stroke with heavy wrist twist. The amplitude is large
        // enough that its DTW distance from the baseline clears the "erratic"
        // threshold even at the default (resting) heart rate.
        repeat(5) { i ->
            val jitter = if (i % 2 == 0) 200f else -200f
            val payload = buildImuPayload(
                time = i.toLong(), ax = 0f, ay = jitter, az = 0f, gx = 5f, gy = 0f, gz = 5f
            )
            state = processImuSample(payload, tracker, state)
        }

        // Baseline must not have been overwritten by the second stroke.
        assertEquals(baselineAfterFirstStroke, tracker.baselineProfiles)
        assertTrue(
            "Expected consistencyScore < 1.0 for an erratic stroke but was ${state.consistencyScore}",
            state.consistencyScore < 1.0f
        )
        assertTrue(state.isShaking)
    }

    @Test
    fun `malformed payloads are ignored and leave state and tracker unchanged`() {
        val tracker = StrokeSessionTracker(windowSize = 5)
        val initialState = WearableState(heartRate = 72f)

        val resultState = processImuSample(ByteArray(10), tracker, initialState)

        assertEquals(initialState, resultState)
        assertFalse(tracker.hasBaseline)
    }

    @Test
    fun `an in-progress window returns state unchanged until the stroke completes`() {
        val tracker = StrokeSessionTracker(windowSize = 5)
        var state = WearableState()

        repeat(4) { i ->
            val payload = buildImuPayload(
                time = i.toLong(), ax = 0f, ay = 1f, az = 0f, gx = 0f, gy = 0f, gz = 0f
            )
            val before = state
            state = processImuSample(payload, tracker, state)
            assertEquals(before, state)
        }
        assertFalse(tracker.hasBaseline)
    }
}

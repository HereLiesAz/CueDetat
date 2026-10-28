package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArTrackingMessagesTest {

    @Test
    fun `each tracking failure gets its own advice and an unknown one a generic line`() {
        val reasons = listOf("INSUFFICIENT_LIGHT", "EXCESSIVE_MOTION", "INSUFFICIENT_FEATURES", "CAMERA_UNAVAILABLE")
        val lines = reasons.map { ArTrackingMessages.trackingLost(it) }
        assertEquals(lines.size, lines.toSet().size)
        assertEquals(ArTrackingMessages.trackingLost(null), ArTrackingMessages.trackingLost("SOMETHING_NEW"))
    }

    @Test
    fun `only a no-match or a flaky service is worth retrying`() {
        assertTrue("ERROR_RESOLVING_LOCALIZATION_NO_MATCH" in ArTrackingMessages.retryableResolveStates)
        assertTrue("ERROR_CLOUD_ID_NOT_FOUND" !in ArTrackingMessages.retryableResolveStates)
        assertTrue("ERROR_NOT_AUTHORIZED" !in ArTrackingMessages.retryableResolveStates)
    }

    @Test
    fun `a state nobody wrote a line for still names itself`() {
        assertTrue("ERROR_NEW_THING" in ArTrackingMessages.resolveFailed("ERROR_NEW_THING"))
    }
}

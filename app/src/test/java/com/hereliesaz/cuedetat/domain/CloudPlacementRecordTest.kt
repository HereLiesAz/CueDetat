package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudPlacementRecordTest {
    private val record = CloudPlacementRecord("cloud-1", 86_400_100L, 100f, 200f, -1f)

    @Test fun `one day key-based placement is usable only until expiry`() {
        assertTrue(record.isUsableAt(100L, 100f, 200f))
        assertFalse(record.isUsableAt(86_400_100L, 100f, 200f))
        assertFalse(record.isUsableAt(86_400_101L, 100f, 200f))
    }

    @Test fun `a different table size never silently reuses the saved map`() {
        assertFalse(record.isUsableAt(100L, 90f, 200f))
        assertFalse(record.isUsableAt(100L, 100f, 180f))
        assertFalse(record.isUsableAt(100L, Float.NaN, 200f))
    }

    @Test fun `bad identifiers and invalid handedness cannot be restored`() {
        assertFalse(record.copy(id = " ").isUsableAt(100L, 100f, 200f))
        assertFalse(record.copy(awaySign = 0f).isUsableAt(100L, 100f, 200f))
        assertFalse(record.copy(logicalWidth = Float.NaN).isUsableAt(100L, 100f, 200f))
    }
}

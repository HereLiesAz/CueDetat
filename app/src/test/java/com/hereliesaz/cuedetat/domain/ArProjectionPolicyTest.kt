package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArProjectionPolicyTest {
    @Test fun `world tracking depends on ARCore capability not just UI mode`() {
        assertTrue(ArProjectionPolicy.isWorldTracked(CameraMode.AR_ACTIVE, DepthCapability.WORLD_TRACKING))
        assertTrue(ArProjectionPolicy.isWorldTracked(CameraMode.AR_ACTIVE, DepthCapability.DEPTH_API))
        assertFalse(ArProjectionPolicy.isWorldTracked(CameraMode.AR_ACTIVE, DepthCapability.NONE))
        assertFalse(ArProjectionPolicy.isWorldTracked(CameraMode.CAMERA, DepthCapability.WORLD_TRACKING))
        assertFalse(ArProjectionPolicy.isWorldTracked(CameraMode.AR_SETUP, DepthCapability.DEPTH_API))
    }
}

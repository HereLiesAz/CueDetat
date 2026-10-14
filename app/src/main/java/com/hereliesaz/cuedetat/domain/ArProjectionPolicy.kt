package com.hereliesaz.cuedetat.domain

/**
 * The overlay has TWO camera implementations even when its UI is AR_ACTIVE.
 *
 * On supported phones, ARCore provides the sole camera/table projection.
 * On unsupported phones, CameraX and CV deliberately use the legacy sensor
 * projection. Always gate sensor suppression on capability, NOT camera mode
 * alone or the transient availability of an ARCore matrix.
 */
object ArProjectionPolicy {
    fun isWorldTracked(mode: CameraMode, capability: DepthCapability): Boolean =
        mode == CameraMode.AR_ACTIVE && capability != DepthCapability.NONE
}

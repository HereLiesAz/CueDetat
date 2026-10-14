package com.hereliesaz.cuedetat.domain

enum class DepthCapability {
    NONE,           // ARCore unavailable
    DEPTH_API,      // ARCore tracking with its metric Depth API
    WORLD_TRACKING, // ARCore tracking without hardware/software depth
}

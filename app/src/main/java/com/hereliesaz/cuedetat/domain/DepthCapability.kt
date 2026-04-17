package com.hereliesaz.cuedetat.domain

enum class DepthCapability {
    NONE,       // ARCore not available on this device
    DEPTH_API,  // ARCore Depth API available (ToF / stereo / SfM — abstracted by ARCore)
}

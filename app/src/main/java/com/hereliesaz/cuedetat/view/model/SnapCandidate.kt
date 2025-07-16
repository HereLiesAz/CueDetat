// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/SnapCandidate.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

data class SnapCandidate(
    val detectedPoint: PointF,
    val firstSeenTimestamp: Long,
    val isConfirmed: Boolean = false
)
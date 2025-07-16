// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/BankShotResult.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

data class BankShotResult(
    val path: List<PointF> = emptyList(),
    val pocketedIndex: Int? = null
)
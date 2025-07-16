// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/TableGeometry.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

data class TableGeometry(
    val width: Float = 0f,
    val height: Float = 0f,
    val unrotatedCorners: List<PointF> = emptyList(),
    val rotatedCorners: List<PointF> = emptyList()
) {
    val isValid: Boolean get() = rotatedCorners.isNotEmpty()
}
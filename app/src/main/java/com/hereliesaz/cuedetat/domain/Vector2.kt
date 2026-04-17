package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.hypot

data class Vector2(val x: Float, val y: Float) {
    fun toPointF(): PointF = PointF(x, y)

    fun distanceTo(other: Vector2): Float {
        return hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()
    }
}

fun PointF.toVector2(): Vector2 = Vector2(x, y)

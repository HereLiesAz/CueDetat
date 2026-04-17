package com.hereliesaz.cuedetat.domain

import kotlin.math.hypot

data class Vector2(val x: Float, val y: Float) {

    fun distanceTo(other: Vector2): Float {
        return hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()
    }
}

package com.hereliesaz.cuedetat.ar

import dev.romainguy.kotlin.math.Float3
import kotlin.math.acos
import kotlin.math.sqrt

object MathUtils {

    fun angleBetween(v1: Float3, v2: Float3): Float {
        val dotProduct = dot(normalize(v1), normalize(v2))
        return Math.toDegrees(acos(dotProduct).toDouble()).toFloat()
    }
}

fun FloatArray.toF3(): Float3 {
    return Float3(this[0], this[1], this[2])
}

fun normalize(v: Float3): Float3 {
    val length = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
    return if (length != 0f) v / length else v
}

fun dot(v1: Float3, v2: Float3): Float {
    return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
}

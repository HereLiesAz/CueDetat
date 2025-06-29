package com.hereliesaz.cuedetat.ar
import androidx.xr.core.Pose
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.dot
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

object MathUtils {
    fun Pose.translationToF3() = Float3(this.translation[0], this.translation[1], this.translation[2])
    fun length(vec: Float3): Float = sqrt(vec.x.pow(2) + vec.y.pow(2) + vec.z.pow(2))
    fun normalize(vec: Float3): Float3 = if (length(vec) > 0) vec / length(vec) else vec
    fun angleBetween(vec1: Float3, vec2: Float3): Float {
        val dotProduct = dot(normalize(vec1), normalize(vec2)).coerceIn(-1.0f, 1.0f)
        return Math.toDegrees(acos(dotProduct).toDouble()).toFloat()
    }
}
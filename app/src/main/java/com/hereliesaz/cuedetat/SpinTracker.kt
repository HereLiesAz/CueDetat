package com.hereliesaz.cuedetat

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.dot
import com.google.ar.sceneform.math.Vector3.subtract
import kotlin.math.acos
import com.hereliesaz.cuedetat.SpinType


class SpinTracker {
    private var previousPos: Vector3? = null
    private var currentPos: Vector3? = null

    fun updatePosition(pos: Vector3) {
        previousPos = currentPos
        currentPos = pos
    }

    fun estimateSpin(expectedDirection: Vector3): SpinType {
        val velocity = currentVelocity() ?: return SpinType.NONE
        val angleDiff = angleBetweenVectors(velocity, expectedDirection)

        return when {
            angleDiff > 20 -> SpinType.MASSÉ
            velocity.z > 0.1 -> SpinType.TOPSPIN
            velocity.z < -0.1 -> SpinType.BACKSPIN
            velocity.x > 0.1 -> SpinType.RIGHT_SPIN
            velocity.x < -0.1 -> SpinType.LEFT_SPIN
            else -> SpinType.NONE
        }
    }

    private fun currentVelocity(): Vector3? {
        if (previousPos == null || currentPos == null) return null
        return subtract(currentPos!!, previousPos!!).normalized()
    }

    private fun angleBetweenVectors(v1: Vector3, v2: Vector3): Float {
        val dot = dot(v1, v2)
        val mag = v1.length() * v2.length()
        return if (mag == 0f) 0f else Math.toDegrees(acos((dot / mag).coerceIn(-1.0f, 1.0f).toDouble())).toFloat()
    }
}
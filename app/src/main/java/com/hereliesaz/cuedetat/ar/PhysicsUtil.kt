package com.hereliesaz.cuedetat.ar

import androidx.xr.runtime.math.Pose.Companion.distance
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.distance
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object PhysicsUtil {

    fun calculateSquirtedVector(
        shotDirection: Float3,
        spinOffset: Float3,
        maxSquirtDegrees: Float
    ): Float3 {
        val squirtFactor = spinOffset.x / 0.5f // -1 to 1
        val squirtAngle = Math.toRadians(squirtFactor * maxSquirtDegrees.toDouble()).toFloat()

        val newX = shotDirection.x * cos(squirtAngle) - shotDirection.z * sin(squirtAngle)
        val newZ = shotDirection.x * sin(squirtAngle) + shotDirection.z * cos(squirtAngle)
        return normalize(Float3(newX, 0f, newZ))
    }

    fun calculateThrownVector(
        shotDirection: Float3,
        hitNormal: Float3,
        spinOffset: Float3,
        maxThrowDegrees: Float
    ): Float3 {
        val throwFactor = spinOffset.x / 0.5f // -1 to 1
        val cutAngleFactor = 1 - abs(dot(shotDirection, hitNormal))
        val throwAngle = Math.toRadians(throwFactor * maxThrowDegrees * cutAngleFactor.toDouble()).toFloat()

        val newX = shotDirection.x * cos(throwAngle) - shotDirection.z * sin(throwAngle)
        val newZ = shotDirection.x * sin(throwAngle) + shotDirection.z * cos(throwAngle)
        return normalize(Float3(newX, 0f, newZ))
    }

    fun calculateBankShot(
        startPos: Float3,
        direction: Float3,
        selectedRail: Rail
    ): Pair<Float3, Float3>? {
        val timeToRail = when (selectedRail) {
            Rail.TOP -> (ArConstants.TABLE_DEPTH / 2 - startPos.z) / direction.z
            Rail.BOTTOM -> (-ArConstants.TABLE_DEPTH / 2 - startPos.z) / direction.z
            Rail.LEFT_TOP, Rail.LEFT_BOTTOM -> (-ArConstants.TABLE_WIDTH / 2 - startPos.x) / direction.x
            Rail.RIGHT_TOP, Rail.RIGHT_BOTTOM -> (ArConstants.TABLE_WIDTH / 2 - startPos.x) / direction.x
        }
        if (timeToRail < 0) return null

        val impactPoint = startPos + direction * timeToRail

        val reflectedDirection = when (selectedRail) {
            Rail.TOP, Rail.BOTTOM -> Float3(direction.x, 0f, -direction.z)
            Rail.LEFT_TOP, Rail.LEFT_BOTTOM, Rail.RIGHT_TOP, Rail.RIGHT_BOTTOM -> Float3(-direction.x, 0f, direction.z)
        }
        return Pair(impactPoint, reflectedDirection)
    }

    fun calculateJumpShotArc(start: Float3, end: Float3, cueElevation: Float): List<Float3> {
        val points = mutableListOf<Float3>()
        val distance = distance(start, end)
        val steps = 20
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = start.x + (end.x - start.x) * t
            val z = start.z + (end.z - start.z) * t
            val y = (4 * cueElevation * t * (1 - t)) * distance * 0.5f // Parabolic arc, scaled
            points.add(Float3(x, y, z))
        }
        return points
    }
}

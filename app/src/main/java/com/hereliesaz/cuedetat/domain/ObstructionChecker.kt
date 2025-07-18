// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ObstructionChecker.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class ObstructionChecker @Inject constructor() {

    data class ObstructionStatus(
        val isObstructed: Boolean = false,
        val obstructingBall: OnPlaneBall? = null
    )

    operator fun invoke(
        shotLineAnchor: PointF,
        cueBall: OnPlaneBall?,
        obstacleBalls: List<OnPlaneBall>,
        isBankingMode: Boolean
    ): ObstructionStatus {
        if (obstacleBalls.isEmpty() || cueBall == null) {
            return ObstructionStatus()
        }

        // In banking mode, we don't check for obstructions as the path is complex.
        if (isBankingMode) {
            return ObstructionStatus()
        }

        val shotVector = PointF(shotLineAnchor.x - cueBall.center.x, shotLineAnchor.y - cueBall.center.y)

        for (obstacle in obstacleBalls) {
            // Check if the obstacle is between the cue ball and the ghost ball
            val vecToObstacle = PointF(obstacle.center.x - cueBall.center.x, obstacle.center.y - cueBall.center.y)
            val dotProduct = vecToObstacle.x * shotVector.x + vecToObstacle.y * shotVector.y
            if (dotProduct > 0 && dotProduct < shotVector.x.pow(2) + shotVector.y.pow(2)) {
                val distanceToShotLine = pointToLineDistance(cueBall.center, shotLineAnchor, obstacle.center)
                if (distanceToShotLine < (cueBall.radius + obstacle.radius)) {
                    return ObstructionStatus(isObstructed = true, obstructingBall = obstacle)
                }
            }
        }

        return ObstructionStatus()
    }

    private fun pointToLineDistance(p1: PointF, p2: PointF, p: PointF): Float {
        val num = abs((p2.x - p1.x) * (p1.y - p.y) - (p1.x - p.x) * (p2.y - p1.y))
        val den = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        return if (den == 0f) 0f else num / den
    }
}
// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ObstacleReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.random.Random

@Singleton
class ObstacleReducer @Inject constructor(private val reducerUtils: ReducerUtils) {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.AddObstacleBall -> handleAddObstacleBall(currentState)
            else -> currentState
        }
    }

    private fun handleAddObstacleBall(currentState: OverlayState): OverlayState {
        val newBallRadius = currentState.onPlaneBall?.radius ?: currentState.protractorUnit.radius
        val placement = findNextAvailablePlacement(currentState)
        val newBall = OnPlaneBall(center = placement, radius = newBallRadius)
        val updatedList = currentState.obstacleBalls + newBall
        return currentState.copy(obstacleBalls = updatedList, valuesChangedSinceReset = true)
    }

    private fun findNextAvailablePlacement(currentState: OverlayState): PointF {
        val visionData =
            currentState.visionData ?: return reducerUtils.getDefaultTargetBallPosition()
        val detectedBalls = visionData.genericBalls + visionData.customBalls
        val allLogicalBalls = (listOfNotNull(
            currentState.onPlaneBall,
            currentState.protractorUnit
        ) + currentState.obstacleBalls).map { it.center }

        val nextAvailableBall = detectedBalls.firstOrNull { detected ->
            allLogicalBalls.none { logical ->
                hypot((logical.x - detected.x).toDouble(), (logical.y - detected.y).toDouble()) < (currentState.protractorUnit.radius * 2)
            }
        }

        if (nextAvailableBall != null) {
            return nextAvailableBall
        }

        // Fallback placement logic
        if (currentState.table.isVisible) {
            val corners = currentState.table.corners
            if (corners.isEmpty()) return reducerUtils.getDefaultTargetBallPosition()

            val minX = corners.minOf { it.x }
            val maxX = corners.maxOf { it.x }
            val minY = corners.minOf { it.y }
            val maxY = corners.maxOf { it.y }

            val attempts = 10
            for (i in 0 until attempts) {
                val randomX = Random.nextFloat() * (maxX - minX) + minX
                val randomY = Random.nextFloat() * (maxY - minY) + minY
                val candidatePoint = PointF(randomX, randomY)

                if (currentState.table.isPointInside(candidatePoint)) {
                    val isOverlapping = allLogicalBalls.any {
                        hypot((it.x - candidatePoint.x).toDouble(), (it.y - candidatePoint.y).toDouble()) < (currentState.protractorUnit.radius * 2.5)
                    }
                    if (!isOverlapping) return candidatePoint
                }
            }
            return reducerUtils.getDefaultTargetBallPosition()
        } else {
            // Original logical-based placement if table is not visible
            val numObstacles = currentState.obstacleBalls.size
            val quadrantIndex = numObstacles % 4
            val radius = currentState.protractorUnit.radius
            val xOffset = if (quadrantIndex % 2 == 0) -radius * 3 else radius * 3
            val yOffset = if (quadrantIndex < 2) -radius * 6 else radius * 6
            return PointF(xOffset, yOffset)
        }
    }
}
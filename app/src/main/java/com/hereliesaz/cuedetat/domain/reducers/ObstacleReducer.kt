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
        val detectedBalls = currentState.visionData.genericBalls + currentState.visionData.customBalls
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
        if (currentState.showTable) {
            // If table is visible, place randomly within its bounds.
            val bounds = reducerUtils.getTableBoundaries(currentState)
            val attempts = 10
            for (i in 0 until attempts) {
                val randomX = Random.nextFloat() * (bounds.width() - currentState.protractorUnit.radius * 2) + bounds.left + currentState.protractorUnit.radius
                val randomY = Random.nextFloat() * (bounds.height() - currentState.protractorUnit.radius * 2) + bounds.top + currentState.protractorUnit.radius
                val candidatePoint = PointF(randomX, randomY)

                // Ensure it doesn't overlap with existing balls
                val isOverlapping = allLogicalBalls.any {
                    hypot((it.x - candidatePoint.x).toDouble(), (it.y - candidatePoint.y).toDouble()) < (currentState.protractorUnit.radius * 2.5)
                }
                if (!isOverlapping) return candidatePoint
            }
            // If all random attempts fail, fall back to center as a last resort
            return PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f)
        } else {
            // Original screen-based placement if table is not visible
            val numObstacles = currentState.obstacleBalls.size
            val quadrantIndex = numObstacles % 4
            val xFactor = if (quadrantIndex % 2 == 0) 0.3f else 0.7f
            val yFactor = if (quadrantIndex < 2) 0.35f else 0.65f
            return PointF(currentState.viewWidth * xFactor, currentState.viewHeight * yFactor)
        }
    }
}
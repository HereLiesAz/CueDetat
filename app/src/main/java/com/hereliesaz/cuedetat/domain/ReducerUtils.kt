// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ReducerUtils.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

const val LOGICAL_BALL_RADIUS = 25f

@Singleton
class ReducerUtils @Inject constructor() {

    fun getDefaultTargetBallPosition(): PointF = PointF(0f, 0f)

    fun getDefaultCueBallPosition(state: OverlayState): PointF {
        val headSpotY = state.table.logicalHeight / 4f // Relative to table center
        return PointF(0f, headSpotY)
    }

    fun snapViolatingBalls(state: OverlayState): OverlayState {
        if (!state.table.isVisible) return state

        var updatedState = state

        if (!state.table.isPointInside(state.protractorUnit.center)) {
            updatedState = updatedState.copy(
                protractorUnit = state.protractorUnit.copy(center = getDefaultTargetBallPosition())
            )
        }

        updatedState.onPlaneBall?.let { ball ->
            if (!state.table.isPointInside(ball.center)) {
                updatedState = updatedState.copy(
                    onPlaneBall = ball.copy(center = getDefaultCueBallPosition(updatedState))
                )
            }
        }

        val confinedObstacles = updatedState.obstacleBalls.filter {
            state.table.isPointInside(it.center)
        }
        if (confinedObstacles.size != updatedState.obstacleBalls.size) {
            updatedState = updatedState.copy(obstacleBalls = confinedObstacles)
        }

        return updatedState
    }
}
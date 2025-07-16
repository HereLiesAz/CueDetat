package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.LogicalBall
import javax.inject.Inject

class ObstacleReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent.AddObstacle, state: OverlayState): OverlayState {
        val newBallCenter = PointF(0f, -state.table.geometry.height * 0.25f)
        val newBall = LogicalBall(center = newBallCenter, radius = state.protractorUnit.radius)
        val newObstacles = state.obstacleBalls + newBall
        return state.copy(obstacleBalls = newObstacles)
    }
}
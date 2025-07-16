// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ReducerUtils.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ReducerUtils @Inject constructor() {

    fun getDefaultTargetBallPosition(): PointF = PointF(0f, 0f)

    fun getDefaultCueBallPosition(state: OverlayState): PointF {
        val logicalRadius = getCurrentLogicalRadius(state.viewWidth, state.viewHeight, 0f)
        val scale = (logicalRadius * 2) / 2.25f
        val tablePlayingSurfaceHeight = state.table.size.shortSideInches * scale
        val headSpotY = tablePlayingSurfaceHeight / 4f
        return PointF(0f, headSpotY)
    }

    fun getCurrentLogicalRadius(stateWidth: Int, stateHeight: Int, zoomSliderPos: Float): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    fun snapViolatingBalls(state: OverlayState): OverlayState {
        if (!state.table.isVisible || !state.table.geometry.isValid) return state

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

    fun reflect(v: PointF, n: PointF): PointF {
        val dot = v.x * n.x + v.y * n.y
        return PointF(v.x - 2 * dot * n.x, v.y - 2 * dot * n.y)
    }
}
// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/domain/StateReducer.kt
package com.hereliesaz.cuedetatlite.domain

import android.graphics.PointF
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.state.ScreenState

class StateReducer(private val warningManager: WarningManager) {

    fun reduce(state: ScreenState, event: MainScreenEvent): ScreenState {
        val newState = when (event) {
            is MainScreenEvent.BallMoved -> reduceBallMoved(state, event.ballId, event.position)
            is MainScreenEvent.BallRadiusChanged -> reduceBallRadiusChanged(state, event.ballId, event.radius)
            MainScreenEvent.Reset -> reduceReset(state)
            else -> state
        }
        return newState.copy(warningText = warningManager.getWarning(newState))
    }

    private fun reduceReset(state: ScreenState): ScreenState {
        return state.copy(
            protractorUnit = ProtractorUnit(),
            warningText = null
        )
    }

    private fun reduceBallMoved(state: ScreenState, ballId: Int, position: PointF): ScreenState {
        val newProtractorUnit = when (ballId) {
            0 -> state.protractorUnit.copy(cueBall = ProtractorUnit.LogicalBall(position, state.protractorUnit.cueBall.radius))
            1 -> state.protractorUnit.copy(targetBall = ProtractorUnit.LogicalBall(position, state.protractorUnit.targetBall.radius))
            else -> state.protractorUnit
        }
        return state.copy(protractorUnit = newProtractorUnit)
    }

    private fun reduceBallRadiusChanged(state: ScreenState, ballId: Int, radius: Float): ScreenState {
        val newProtractorUnit = when (ballId) {
            0 -> state.protractorUnit.copy(cueBall = ProtractorUnit.LogicalBall(state.protractorUnit.cueBall.logicalPosition, radius))
            1 -> state.protractorUnit.copy(targetBall = ProtractorUnit.LogicalBall(state.protractorUnit.targetBall.logicalPosition, radius))
            else -> state.protractorUnit
        }
        return state.copy(protractorUnit = newProtractorUnit)
    }
}

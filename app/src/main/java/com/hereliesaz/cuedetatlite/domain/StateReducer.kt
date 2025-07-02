package com.hereliesaz.cuedetatlite.domain

import android.graphics.PointF
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.ui.ZoomMapping
import com.hereliesaz.cuedetatlite.view.model.ActualCueBall
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import javax.inject.Inject

class StateReducer @Inject constructor(private val warningManager: WarningManager) {

    private fun getInitialState(width: Int, height: Int): ScreenState {
        val initialRadius = (width.coerceAtMost(height) * 0.30f / 2f) * ZoomMapping.DEFAULT_ZOOM
        return ScreenState(
            protractorUnit = ProtractorUnit(
                targetBall = ProtractorUnit.LogicalBall(PointF(width / 2f, height / 2f), initialRadius),
                aimingAngleDegrees = 0f
            ),
        )
    }

    fun reduce(state: ScreenState, event: MainScreenEvent, viewWidth: Int, viewHeight: Int): ScreenState {
        val newState = when (event) {
            is MainScreenEvent.ViewResized -> getInitialState(event.width, event.height)

            is MainScreenEvent.BallMoved -> {
                when (event.ballId) {
                    1 -> {
                        val newTargetBall = ProtractorUnit.LogicalBall(event.position, state.protractorUnit.targetBall.radius)
                        state.copy(protractorUnit = state.protractorUnit.copy(targetBall = newTargetBall))
                    }
                    2 -> {
                        val newActualCueBall = state.actualCueBall?.let { ActualCueBall(event.position, it.radius) }
                        state.copy(actualCueBall = newActualCueBall)
                    }
                    else -> state
                }
            }

            is MainScreenEvent.BallRadiusChanged -> {
                when (event.ballId) {
                    1 -> {
                        val newTargetBall = ProtractorUnit.LogicalBall(state.protractorUnit.targetBall.logicalPosition, event.radius)
                        state.copy(protractorUnit = state.protractorUnit.copy(targetBall = newTargetBall))
                    }
                    2 -> {
                        val newActualCueBall = state.actualCueBall?.let { ActualCueBall(it.logicalPosition, event.radius) }
                        state.copy(actualCueBall = newActualCueBall)
                    }
                    else -> state
                }
            }

            is MainScreenEvent.ToggleActualCueBall -> {
                if (state.isBankingMode) {
                    return state // Correct way to return early from the function
                }
                if (state.actualCueBall == null) {
                    state.copy(actualCueBall = ActualCueBall(PointF(viewWidth / 2f, viewHeight * 0.75f), state.protractorUnit.targetBall.radius))
                } else {
                    state.copy(actualCueBall = null)
                }
            }

            is MainScreenEvent.ToggleBankingMode -> {
                if (!state.isBankingMode) {
                    state.copy(
                        isBankingMode = true,
                        isProtractorMode = false,
                        tableModel = TableModel.create(viewWidth.toFloat(), viewHeight.toFloat()),
                        actualCueBall = ActualCueBall(PointF(viewWidth / 2f, viewHeight / 2f), state.protractorUnit.targetBall.radius)
                    )
                } else {
                    state.copy(
                        isBankingMode = false,
                        isProtractorMode = true,
                        tableModel = null,
                        bankingPath = emptyList(),
                        actualCueBall = null // Remove the banking ball when leaving mode
                    )
                }
            }

            is MainScreenEvent.BankingAimTargetChanged -> {
                if (state.isBankingMode && state.tableModel != null && state.actualCueBall != null) {
                    val path = state.tableModel.calculateBankingPath(state.actualCueBall.logicalPosition, event.position)
                    state.copy(bankingPath = path)
                } else state
            }

            MainScreenEvent.Reset -> getInitialState(viewWidth, viewHeight)
            else -> state
        }
        return newState.copy(warningText = warningManager.getWarning(newState))
    }
}
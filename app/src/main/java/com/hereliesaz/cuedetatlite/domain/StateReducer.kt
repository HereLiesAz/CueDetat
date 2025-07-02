package com.hereliesaz.cuedetatlite.domain

import android.graphics.PointF
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.ui.ZoomMapping
import com.hereliesaz.cuedetatlite.view.model.ActualCueBall
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import javax.inject.Inject
import kotlin.math.min

class StateReducer @Inject constructor(private val warningManager: WarningManager) {

    private fun getInitialState(width: Int, height: Int): ScreenState {
        val initialRadius = (min(width, height) * 0.30f / 2f) * ZoomMapping.sliderToZoom(50f)
        return ScreenState(
            protractorUnit = ProtractorUnit(
                targetBall = ProtractorUnit.LogicalBall(PointF(width / 2f, height / 2f), initialRadius)
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

            is MainScreenEvent.BallRadiusChanged -> state // Handled by UpdateStateUseCase

            is MainScreenEvent.ToggleActualCueBall -> {
                if (state.isBankingMode) {
                    return state
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
                        actualCueBall = null
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
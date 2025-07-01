// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/domain/StateReducer.kt
package com.hereliesaz.cuedetatlite.domain

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.view.model.IlogicalBall
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.hereliesaz.cuedetatlite.view.state.WarningManager

@Singleton
class StateReducer @Inject constructor(
    private val sensorRepository: SensorRepository
) {
    private val _pitchMatrix = MutableStateFlow(Matrix())
    val pitchMatrix = _pitchMatrix.asStateFlow()

    private val _luminance = MutableStateFlow(0f)
    val luminance = _luminance.asStateFlow()

    private val _isForceLightMode = MutableStateFlow<Boolean?>(null)
    val isForceLightMode = _isForceLightMode.asStateFlow()

    private val stateHistory = mutableListOf<ScreenState>()
    private var historyIndex = -1

    private fun updateState(newState: ScreenState): ScreenState {
        if (historyIndex < stateHistory.lastIndex) {
            stateHistory.subList(historyIndex + 1, stateHistory.size).clear()
        }
        stateHistory.add(newState)
        historyIndex++
        return newState
    }

    fun onTouch(x: Float, y: Float, currentState: ScreenState): ScreenState {
        val newProtractorUnit = currentState.protractorUnit.copy()
        val bankingPath = if(currentState.isBankingMode) {
            currentState.tableModel?.calculateBankingPath(newProtractorUnit.cueBall.center, PointF(x,y)) ?: emptyList()
        } else {
            emptyList()
        }

        val newState = currentState.copy(
            protractorUnit = newProtractorUnit,
            bankingPath = bankingPath,
            actualCueBall = IlogicalBall(center = PointF(x, y), radius = 28f)
        )
        return updateState(newState)
    }

    fun onScale(factor: Float, currentState: ScreenState): ScreenState {
        val newRadius = (currentState.protractorUnit.cueBall.radius * factor).coerceIn(10f, 100f)
        val newProtractorUnit = currentState.protractorUnit.copy(
            cueBall = currentState.protractorUnit.cueBall.copy(radius = newRadius),
            targetBall = currentState.protractorUnit.targetBall.copy(radius = newRadius)
        )
        val newState = currentState.copy(protractorUnit = newProtractorUnit)
        return updateState(newState)
    }

    fun onTableResize(width: Int, height: Int, currentState: ScreenState): ScreenState {
        val table = TableModel.create(width.toFloat(), height.toFloat())
        val newState = currentState.copy(tableModel = table)
        return updateState(newState)
    }

    fun onForceLightMode(isLightMode: Boolean) {
        _isForceLightMode.value = isLightMode
    }

    fun onLuminanceChange(value: Float) {
        _luminance.value = value
    }

    fun onUndo(currentState: ScreenState): ScreenState {
        if (historyIndex > 0) {
            historyIndex--
            return stateHistory[historyIndex]
        }
        return currentState
    }

    fun onRedo(currentState: ScreenState): ScreenState {
        if (historyIndex < stateHistory.lastIndex) {
            historyIndex++
            return stateHistory[historyIndex]
        }
        return currentState
    }

    fun onJumpShot(currentState: ScreenState): ScreenState {
        val newState = currentState.copy(warningText = WarningText("Jump Shot Mode Toggled (Not Implemented)"))
        return updateState(newState)
    }
}

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.LogicalBall
import com.hereliesaz.cuedetat.view.model.TableSize
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.ToastMessage
import javax.inject.Inject

class SettingsReducer @Inject constructor(
    private val reducerUtils: ReducerUtils,
) {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        val isChanged = state.copy(valuesChangedSinceReset = true)
        return when (event) {
            is MainScreenEvent.Reset -> OverlayState()
            is MainScreenEvent.ToggleOnPlaneBall -> isChanged.copy(onPlaneBall = if (state.onPlaneBall == null) LogicalBall(center = reducerUtils.getDefaultCueBallPosition(state), radius = state.protractorUnit.radius) else null)
            is MainScreenEvent.ToggleBankingMode -> {
                val newBankingMode = !state.isBankingMode
                if(newBankingMode && state.onPlaneBall == null){
                    return state.copy(
                        toastMessage = "Can't bank without a cue ball",
                    )
                }
                isChanged.copy(isBankingMode = newBankingMode)
            }
            is MainScreenEvent.LoadUserSettings -> state.copy(
                isForceLightMode = event.prefs.forceLightMode,
                glowStickValue = event.prefs.glowAmount,
                luminanceAdjustment = event.prefs.luminance,
                table = state.table.copy(size = TableSize.valueOf(event.prefs.tableSize))
            )
            else -> state
        }
    }
}
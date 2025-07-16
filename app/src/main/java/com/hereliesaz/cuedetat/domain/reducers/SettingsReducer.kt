// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/SettingsReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.R
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
            is MainScreenEvent.Reset -> OverlayState(appControlColorScheme = state.appControlColorScheme, isTutorialVisible = false)
            is MainScreenEvent.UpdateZoom -> isChanged.copy(zoomSliderPosition = event.zoom)
            is MainScreenEvent.ToggleTable -> isChanged.copy(table = state.table.withVisibility(!state.table.isVisible))
            is MainScreenEvent.UpdateTableRotation -> isChanged.copy(table = state.table.withRotation(event.rotation))
            is MainScreenEvent.CycleTableSize -> isChanged.copy(table = state.table.withSize(TableSize.entries.let { val nextIndex = (it.indexOf(state.table.size) + 1) % it.size; it[nextIndex] }))
            is MainScreenEvent.SetTableSize -> isChanged.copy(table = state.table.withSize(event.size), isTableSizeDialogVisible = false)
            is MainScreenEvent.ToggleOnPlaneBall -> isChanged.copy(onPlaneBall = if (state.onPlaneBall == null) LogicalBall(center = reducerUtils.getDefaultCueBallPosition(state), radius = state.protractorUnit.radius) else null)
            is MainScreenEvent.ToggleBankingMode -> {
                val newBankingMode = !state.isBankingMode
                if(newBankingMode && state.onPlaneBall == null){
                    return state.copy(
                        toastMessage = ToastMessage.StringResource(R.string.toast_no_cue_ball_for_banking),
                    )
                }
                isChanged.copy(isBankingMode = newBankingMode)
            }
            is MainScreenEvent.UpdateBankingAim -> isChanged.copy(bankingAimTarget = event.position)
            is MainScreenEvent.AddObstacle -> {
                val newBallCenter = PointF(0f, -state.table.geometry.height * 0.25f)
                val newBall = LogicalBall(center = newBallCenter, radius = state.protractorUnit.radius)
                val newObstacles = state.obstacleBalls + newBall
                isChanged.copy(obstacleBalls = newObstacles)
            }
            is MainScreenEvent.ToggleLuminanceDialog -> state.copy(isLuminanceDialogVisible = !state.isLuminanceDialogVisible)
            is MainScreenEvent.ToggleGlowStickDialog -> state.copy(isGlowStickDialogVisible = !state.isGlowStickDialogVisible)
            is MainScreenEvent.ToggleTableSizeDialog -> state.copy(isTableSizeDialogVisible = !state.isTableSizeDialogVisible)
            is MainScreenEvent.ToggleAdvancedOptionsDialog -> state.copy(isAdvancedOptionsDialogVisible = !state.isAdvancedOptionsDialogVisible)
            is MainScreenEvent.ToggleSpinControl -> state.copy(isSpinControlVisible = !state.isSpinControlVisible, spinControlCenter = if (!state.isSpinControlVisible) state.onPlaneBall?.center else null)
            is MainScreenEvent.ClearSpinState -> state.copy(lingeringSpinOffset = null, selectedSpinOffset = null)
            is MainScreenEvent.ToggleForceLightMode -> state.copy(isForceLightMode = !(state.isForceLightMode ?: false))
            is MainScreenEvent.ToggleHelpers -> state.copy(areHelpersVisible = !state.areHelpersVisible)
            is MainScreenEvent.LoadUserSettings -> state.copy(
                isForceLightMode = event.prefs.forceLightMode,
                glowStickValue = event.prefs.glowAmount,
                luminanceAdjustment = event.prefs.luminance,
                table = state.table.withSize(TableSize.valueOf(event.prefs.tableSize))
            )
            else -> state
        }
    }
}
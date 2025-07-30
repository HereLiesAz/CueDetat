package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.TableSize

internal fun reduceSystemAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.SizeChanged -> handleSizeChanged(state, action)
        is MainScreenEvent.FullOrientationChanged -> state.copy(currentOrientation = action.orientation)
        is MainScreenEvent.ThemeChanged -> state.copy(appControlColorScheme = action.scheme)
        is MainScreenEvent.SetWarning -> state.copy(warningText = action.warning)
        else -> state
    }
}

private fun handleSizeChanged(
    state: CueDetatState,
    action: MainScreenEvent.SizeChanged
): CueDetatState {
    if (state.viewWidth == 0 && state.viewHeight == 0) {
        return createInitialState(
            action.width,
            action.height,
            state.appControlColorScheme ?: darkColorScheme()
        )
    }
    return state.copy(
        viewWidth = action.width,
        viewHeight = action.height,
    )
}

private fun createInitialState(
    viewWidth: Int,
    viewHeight: Int,
    appColorScheme: ColorScheme
): CueDetatState {
    val initialSliderPos = 0f
    val initialProtractorCenter = PointF(0f, 0f)
    val initialSpinControlCenter = PointF(viewWidth / 2f, viewHeight * 0.75f)

    return CueDetatState(
        viewWidth = viewWidth,
        viewHeight = viewHeight,
        protractorUnit = ProtractorUnit(
            center = initialProtractorCenter,
            radius = LOGICAL_BALL_RADIUS,
            rotationDegrees = 0f
        ),
        table = Table(
            size = TableSize.EIGHT_FT,
            isVisible = false,
        ),
        onPlaneBall = null,
        zoomSliderPosition = initialSliderPos,
        isBankingMode = false,
        bankingAimTarget = null,
        valuesChangedSinceReset = false,
        areHelpersVisible = LabelConfig.showLabelsByDefault,
        isMoreHelpVisible = false,
        isForceLightMode = null,
        luminanceAdjustment = 0f,
        showLuminanceDialog = false,
        showTutorialOverlay = false,
        currentTutorialStep = 0,
        appControlColorScheme = appColorScheme,
        interactionMode = InteractionMode.NONE,
        spinControlCenter = initialSpinControlCenter,
        experienceMode = null // Force selection on first load
    )
}
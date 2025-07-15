// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SystemReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemReducer @Inject constructor(private val reducerUtils: ReducerUtils) {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.SizeChanged -> handleSizeChanged(currentState, event)
            is MainScreenEvent.FullOrientationChanged -> currentState.copy(currentOrientation = event.orientation)
            is MainScreenEvent.ThemeChanged -> currentState.copy(appControlColorScheme = event.scheme)
            else -> currentState
        }
    }

    private fun handleSizeChanged(currentState: OverlayState, event: MainScreenEvent.SizeChanged): OverlayState {
        // Upon genesis, create the initial state.
        if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
            return createInitialState(event.width, event.height, currentState.appControlColorScheme)
        }

        // For all subsequent resizes, only update view dimensions and dependent logical radii.
        // Logical coordinates of objects remain absolute and are not modified here.
        val newLogicalRadius = reducerUtils.getCurrentLogicalRadius(event.width, event.height, currentState.zoomSliderPosition)

        return currentState.copy(
            viewWidth = event.width,
            viewHeight = event.height,
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius),
            obstacleBalls = currentState.obstacleBalls.map { it.copy(radius = newLogicalRadius) }
        )
    }

    private fun createInitialState(viewWidth: Int, viewHeight: Int, appColorScheme: ColorScheme): OverlayState {
        val initialSliderPos = 0f
        val initialLogicalRadius = reducerUtils.getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialProtractorCenter = PointF(0f, 0f)
        val initialTableRotation = 90f // Default to portrait orientation

        // UI elements like the spin control remain screen-relative.
        val initialSpinControlCenter = PointF(viewWidth / 2f, viewHeight * 0.75f)

        return OverlayState(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = initialProtractorCenter,
                radius = initialLogicalRadius,
                rotationDegrees = -90f // Default to a straight-down shot
            ),
            onPlaneBall = null,
            zoomSliderPosition = initialSliderPos,
            isBankingMode = false,
            showTable = false,
            tableRotationDegrees = initialTableRotation,
            bankingAimTarget = null,
            valuesChangedSinceReset = false,
            areHelpersVisible = false,
            isMoreHelpVisible = false,
            isForceLightMode = null,
            luminanceAdjustment = 0f,
            showLuminanceDialog = false,
            showTutorialOverlay = false,
            currentTutorialStep = 0,
            appControlColorScheme = appColorScheme,
            interactionMode = InteractionMode.NONE,
            spinControlCenter = initialSpinControlCenter
        )
    }
}
package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class SystemReducer @Inject constructor() {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.SizeChanged -> handleSizeChanged(currentState, event)
            is MainScreenEvent.FullOrientationChanged -> currentState.copy(currentOrientation = event.orientation)
            is MainScreenEvent.ThemeChanged -> currentState.copy(appControlColorScheme = event.scheme)
            else -> currentState
        }
    }

    private fun handleSizeChanged(currentState: OverlayState, event: MainScreenEvent.SizeChanged): OverlayState {
        if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
            return createInitialState(event.width, event.height, currentState.appControlColorScheme)
        } else {
            val newLogicalRadius = ReducerUtils.getCurrentLogicalRadius(event.width, event.height, currentState.zoomSliderPosition)
            var updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)
            var protractorNewCenter = currentState.protractorUnit.center

            if (currentState.protractorUnit.center.x.roughlyEquals(currentState.viewWidth / 2f) &&
                currentState.protractorUnit.center.y.roughlyEquals(currentState.viewHeight / 2f)) {
                protractorNewCenter = PointF(event.width / 2f, event.height / 2f)
            }
            if (currentState.isBankingMode && updatedOnPlaneBall != null) {
                if (updatedOnPlaneBall.center.x.roughlyEquals(currentState.viewWidth/2f) && updatedOnPlaneBall.center.y.roughlyEquals(currentState.viewHeight/2f)) {
                    updatedOnPlaneBall = updatedOnPlaneBall.copy(center = PointF(
                        event.width / 2f,
                        event.height / 2f
                    )
                    )
                }
            }

            return currentState.copy(
                viewWidth = event.width, viewHeight = event.height,
                protractorUnit = currentState.protractorUnit.copy(
                    radius = newLogicalRadius,
                    center = protractorNewCenter
                ),
                onPlaneBall = updatedOnPlaneBall
            )
        }
    }

    private fun createInitialState(viewWidth: Int, viewHeight: Int, appColorScheme: ColorScheme): OverlayState {
        val initialSliderPos = 0f // Centered
        val initialLogicalRadius = ReducerUtils.getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialProtractorCenter = PointF(viewWidth / 2f, viewHeight / 2f)
        val initialTableRotation = if (viewWidth > viewHeight) 0f else 90f // Landscape vs Portrait

        return OverlayState(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = initialProtractorCenter,
                radius = initialLogicalRadius,
                rotationDegrees = 0f
            ),
            onPlaneBall = null, // Default to null
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
            interactionMode = InteractionMode.NONE
        )
    }

    private fun Float.roughlyEquals(other: Float, tolerance: Float = 0.00001f): Boolean {
        return abs(this - other) < tolerance
    }
}
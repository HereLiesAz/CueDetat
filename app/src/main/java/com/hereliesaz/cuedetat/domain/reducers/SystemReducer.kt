// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SystemReducer.kt

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
        if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
            return createInitialState(event.width, event.height, currentState.appControlColorScheme)
        } else {
            val newLogicalRadius = reducerUtils.getCurrentLogicalRadius(event.width, event.height, currentState.zoomSliderPosition)
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

            // Also update the spin control's position if it's at its default location
            var newSpinControlCenter = currentState.spinControlCenter
            currentState.spinControlCenter?.let {
                val oldDefaultY = currentState.viewHeight * 0.75f
                if(it.x.roughlyEquals(currentState.viewWidth / 2f) && it.y.roughlyEquals(oldDefaultY)) {
                    newSpinControlCenter = PointF(event.width / 2f, event.height * 0.75f)
                }
            }

            return currentState.copy(
                viewWidth = event.width, viewHeight = event.height,
                protractorUnit = currentState.protractorUnit.copy(
                    radius = newLogicalRadius,
                    center = protractorNewCenter
                ),
                onPlaneBall = updatedOnPlaneBall,
                spinControlCenter = newSpinControlCenter
            )
        }
    }

    private fun createInitialState(viewWidth: Int, viewHeight: Int, appColorScheme: ColorScheme): OverlayState {
        val initialSliderPos = 0f // Centered
        val initialLogicalRadius = reducerUtils.getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialProtractorCenter = PointF(viewWidth / 2f, viewHeight / 2f)
        val initialTableRotation = 90f // Default to Portrait orientation

        // --- THE RIGHTEOUS FIX ---
        // Calculate the center of the bottom half of the screen.
        val initialSpinControlCenter = PointF(
            viewWidth / 2f,
            viewHeight * 0.75f
        )
        // --- END FIX ---

        return OverlayState(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = initialProtractorCenter,
                radius = initialLogicalRadius,
                rotationDegrees = 0f
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
            spinControlCenter = initialSpinControlCenter // Set the initial position
        )
    }

    private fun Float.roughlyEquals(other: Float, tolerance: Float = 0.00001f): Boolean {
        return abs(this - other) < tolerance
    }
}
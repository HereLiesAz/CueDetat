// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SystemReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.TableSize
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

        // For all subsequent resizes, only update view dimensions.
        // Logical sizes are now constant and do not depend on view size.
        return currentState.copy(
            viewWidth = event.width,
            viewHeight = event.height,
        )
    }

    private fun createInitialState(viewWidth: Int, viewHeight: Int, appColorScheme: ColorScheme): OverlayState {
        val initialSliderPos = 0f
        val initialProtractorCenter = PointF(0f, 0f)

        // UI elements like the spin control remain screen-relative.
        val initialSpinControlCenter = PointF(viewWidth / 2f, viewHeight * 0.75f)

        return OverlayState(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = initialProtractorCenter,
                radius = LOGICAL_BALL_RADIUS,
                rotationDegrees = -90f // Default to a straight-down shot
            ),
            table = Table(
                size = TableSize.EIGHT_FT,
                rotationDegrees = 0f, // Portrait is default
                isVisible = false,
            ),
            onPlaneBall = null,
            zoomSliderPosition = initialSliderPos,
            isBankingMode = false,
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
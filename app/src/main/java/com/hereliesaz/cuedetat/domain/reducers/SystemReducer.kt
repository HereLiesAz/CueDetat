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

/**
 * Reducer function responsible for handling System-level events.
 *
 * This includes events related to:
 * - Screen size changes (layout updates).
 * - Device orientation changes.
 * - Theme/Color scheme updates.
 * - System warnings.
 *
 * @param state The current state.
 * @param action The system event.
 * @return The updated state.
 */
internal fun reduceSystemAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    // Process the specific system event.
    return when (action) {
        // Case: The size of the main view container has changed (e.g., layout pass, resize).
        is MainScreenEvent.SizeChanged -> handleSizeChanged(state, action)

        // Case: The device's physical orientation has changed (Portrait/Landscape).
        is MainScreenEvent.FullOrientationChanged -> state.copy(currentOrientation = action.orientation)

        // Case: The application's theme/color scheme has been updated (e.g., dynamic colors).
        is MainScreenEvent.ThemeChanged -> state.copy(appControlColorScheme = action.scheme)

        // Case: A warning message needs to be displayed or cleared.
        is MainScreenEvent.SetWarning -> state.copy(warningText = action.warning)

        // Fallback: Return state unchanged for unknown actions.
        else -> state
    }
}

/**
 * Handles changes to the view's dimensions.
 *
 * If this is the FIRST time dimensions are being set (initialization),
 * it triggers the creation of the initial state with default values centered in the view.
 * Otherwise, it just updates the dimensions in the existing state.
 *
 * @param state The current state.
 * @param action The size change event containing new width and height.
 * @return The updated state.
 */
private fun handleSizeChanged(
    state: CueDetatState,
    action: MainScreenEvent.SizeChanged
): CueDetatState {
    val newSpinCenter = if (state.spinControlCenter == null && action.width > 0 && action.height > 0) {
        PointF(action.width / 2f, 116f * action.density)
    } else {
        state.spinControlCenter
    }

    return state.copy(
        viewWidth = action.width,
        viewHeight = action.height,
        screenDensity = action.density,
        spinControlCenter = newSpinCenter
    )
}

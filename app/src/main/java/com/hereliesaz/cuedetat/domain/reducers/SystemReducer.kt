package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.TableOrientationLearner
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
        is MainScreenEvent.FullOrientationChanged -> followCompass(state.copy(currentOrientation = action.orientation))

        // Case: The application's theme/color scheme has been updated (e.g., dynamic colors).
        is MainScreenEvent.ThemeChanged -> state.copy(appControlColorScheme = action.scheme)

        // Case: A warning message needs to be displayed or cleared.
        is MainScreenEvent.SetWarning -> state.copy(warningText = action.warning)

        // Case: Wear OS state updated
        is MainScreenEvent.WearableStateUpdated -> state.copy(wearableState = action.state)

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

/** Heading change (degrees) below which the table isn't turned: keeps sensor jitter from re-rendering. */
internal const val COMPASS_FOLLOW_DEADBAND_DEG = 0.5f

/**
 * Turns the virtual table against the phone's compass heading, AR or not: a pool table doesn't
 * move, so turning the phone right turns the table left on screen (the same rule, sign -1, as
 * TableOrientationLearner). User rotation adds on top; the compass only contributes its change.
 *
 * The change is measured from [CueDetatState.compassRefYaw], not the previous tick, so sub-deadband
 * drift accumulates instead of being lost. Suspended — and the reference dropped — while the view
 * is locked (beginner) or ARCore anchors the table, so resuming picks up from the current heading.
 */
internal fun followCompass(state: CueDetatState): CueDetatState {
    val yaw = state.currentOrientation.yaw
    if (state.isBeginnerViewLocked || state.isArTableLocked) {
        return if (state.compassRefYaw == null) state else state.copy(compassRefYaw = null)
    }
    val ref = state.compassRefYaw ?: return state.copy(compassRefYaw = yaw)
    val delta = TableOrientationLearner.normalize360(yaw - ref)
    if (kotlin.math.abs(delta) < COMPASS_FOLLOW_DEADBAND_DEG) return state
    return state.copy(
        worldRotationDegrees = TableOrientationLearner.normalize360(state.worldRotationDegrees - delta),
        compassRefYaw = yaw,
    )
}

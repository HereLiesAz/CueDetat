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
    // Check if the state has been initialized with valid dimensions yet.
    if (state.viewWidth == 0 && state.viewHeight == 0) {
        // If not, this is the first layout pass. Create the full initial state.
        return createInitialState(
            action.width,
            action.height,
            state.appControlColorScheme ?: darkColorScheme() // Use existing scheme or default dark.
        )
    }
    // If state already exists, just update the dimensions to match the new layout.
    return state.copy(
        viewWidth = action.width,
        viewHeight = action.height,
    )
}

/**
 * Creates the initial application state with default values.
 *
 * This is called once when the view dimensions are first known.
 *
 * @param viewWidth The width of the view in pixels.
 * @param viewHeight The height of the view in pixels.
 * @param appColorScheme The color scheme to use.
 * @return A fresh [CueDetatState] instance.
 */
private fun createInitialState(
    viewWidth: Int,
    viewHeight: Int,
    appColorScheme: ColorScheme
): CueDetatState {
    // Default zoom slider position (0.0 means unzoomed/default).
    val initialSliderPos = 0f

    // Default center for the target ball (ProtractorUnit) is logical (0,0).
    val initialProtractorCenter = PointF(0f, 0f)

    // Default position for the spin control UI element (bottom-right quadrant).
    val initialSpinControlCenter = PointF(viewWidth / 2f, viewHeight * 0.75f)

    // Construct and return the state object.
    return CueDetatState(
        viewWidth = viewWidth,
        viewHeight = viewHeight,
        protractorUnit = ProtractorUnit(
            center = initialProtractorCenter,
            radius = LOGICAL_BALL_RADIUS,
            rotationDegrees = 0f
        ),
        table = Table(
            size = TableSize.EIGHT_FT, // Default table size.
            isVisible = false, // Table is hidden by default.
        ),
        onPlaneBall = null, // Cue ball is not placed initially.
        zoomSliderPosition = initialSliderPos,
        isBankingMode = false, // Banking mode disabled by default.
        bankingAimTarget = null,
        valuesChangedSinceReset = false,
        areHelpersVisible = LabelConfig.showLabelsByDefault, // Respect global config.
        isMoreHelpVisible = false,
        isForceLightMode = null, // Follow system theme by default.
        luminanceAdjustment = 0f, // No brightness adjustment.
        showLuminanceDialog = false,
        showTutorialOverlay = false,
        currentTutorialStep = 0,
        appControlColorScheme = appColorScheme,
        interactionMode = InteractionMode.NONE, // No interaction active initially.
        spinControlCenter = initialSpinControlCenter,
        experienceMode = null // Force user to select mode on first load (if null).
    )
}

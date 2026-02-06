// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit

/**
 * Reducer function responsible for handling "Toggle" actions.
 *
 * This includes all boolean state flips (showing/hiding UI elements),
 * cycling through modes (table sizes, units), and managing experience modes (Expert/Beginner).
 *
 * @param state The current state.
 * @param action The toggle event.
 * @param reducerUtils Utility for calculating defaults during mode switches.
 * @return The updated state.
 */
internal fun reduceToggleAction(
    state: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils
): CueDetatState {
    // Switch on the specific event type.
    return when (action) {
        // Toggle the visibility of the main side menu.
        is MainScreenEvent.ToggleMenu -> {
            // When toggling the nav rail/menu, we simply flip the visibility flag.
            // Note: Implementation detail regarding interaction with 'isNavigationRailExpanded' handles elsewhere.
            state.copy(
                isMenuVisible = !state.isMenuVisible
            )
        }

        // Toggle the expansion state of the navigation rail.
        is MainScreenEvent.ToggleNavigationRail -> {
            state.copy(
                isNavigationRailExpanded = !state.isNavigationRailExpanded,
                // If expanding the rail, ensure the standard menu is closed to avoid clutter.
                isMenuVisible = false
            )
        }

        // Toggle the visibility of the Spin (English) control widget.
        is MainScreenEvent.ToggleSpinControl -> state.copy(isSpinControlVisible = !state.isSpinControlVisible)

        // Toggle the "Banking Mode" (calculating bank shots).
        is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(state, reducerUtils)

        // Cycle to the next available table size.
        is MainScreenEvent.CycleTableSize -> {
            // Create a new state with the next table size enum value.
            val newState = state.copy(
                table = state.table.copy(size = state.table.size.next()),
                valuesChangedSinceReset = true
            )
            // Ensure that balls are still within valid bounds on the new table size.
            reducerUtils.snapViolatingBalls(newState)
        }

        // Set the table size to a specific value (e.g., from a dialog).
        is MainScreenEvent.SetTableSize -> {
            val newState = state.copy(
                table = state.table.copy(size = action.size),
                valuesChangedSinceReset = true
            )
            // Ensure ball positions are valid.
            reducerUtils.snapViolatingBalls(newState)
        }

        // Toggle visibility of the Table Size selection dialog.
        is MainScreenEvent.ToggleTableSizeDialog -> state.copy(showTableSizeDialog = !state.showTableSizeDialog)

        // Toggle the forced UI theme (Light/Dark/System).
        is MainScreenEvent.ToggleForceTheme -> {
            // Cycle: Null (System) -> True (Light) -> False (Dark) -> Null.
            val newMode = when (state.isForceLightMode) {
                null -> true; true -> false; false -> null
            }
            state.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
        }

        // Toggle the visibility of the camera feed (for AR vs Pure Virtual mode).
        is MainScreenEvent.ToggleCamera -> state.copy(isCameraVisible = !state.isCameraVisible)

        // Toggle between Metric and Imperial distance units.
        is MainScreenEvent.ToggleDistanceUnit -> state.copy(
            distanceUnit = if (state.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
            valuesChangedSinceReset = true
        )

        // Toggle visibility of the luminance adjustment dialog.
        is MainScreenEvent.ToggleLuminanceDialog -> state.copy(showLuminanceDialog = !state.showLuminanceDialog)

        // Toggle visibility of the "Glow Stick" feature dialog.
        is MainScreenEvent.ToggleGlowStickDialog -> state.copy(showGlowStickDialog = !state.showGlowStickDialog)

        // Toggle visibility of basic helper labels.
        is MainScreenEvent.ToggleHelp -> state.copy(areHelpersVisible = !state.areHelpersVisible)

        // Toggle visibility of the extended help/tutorial overlay.
        is MainScreenEvent.ToggleMoreHelp -> state.copy(isMoreHelpVisible = !state.isMoreHelpVisible)

        // Toggle the snap-to-ball functionality.
        is MainScreenEvent.ToggleSnapping -> state.copy(isSnappingEnabled = !state.isSnappingEnabled)

        // Toggle between standard and custom CV models.
        is MainScreenEvent.ToggleCvModel -> state.copy(useCustomModel = !state.useCustomModel)

        // Cycle through orientation lock modes (Portrait/Landscape/Auto) - Pending application.
        is MainScreenEvent.ToggleOrientationLock -> {
            // Get current pending or active lock state.
            val current = state.pendingOrientationLock ?: state.orientationLock
            // Cycle to next state.
            state.copy(pendingOrientationLock = current.next())
        }

        // Apply the pending orientation lock choice.
        is MainScreenEvent.ApplyPendingOrientationLock -> {
            if (state.pendingOrientationLock == null) return state
            return state.copy(
                orientationLock = state.pendingOrientationLock,
                pendingOrientationLock = null
            )
        }

        // Handle a change in orientation lock (direct set).
        is MainScreenEvent.OrientationChanged -> state.copy(orientationLock = action.orientationLock)

        // Set the user's Experience Mode (Expert/Beginner/Hater).
        is MainScreenEvent.SetExperienceMode -> handleSetExperienceMode(
            state,
            action.mode,
            reducerUtils
        )

        // Apply a pending experience mode change.
        is MainScreenEvent.ApplyPendingExperienceMode -> {
            if (state.pendingExperienceMode == null) return state
            return handleSetExperienceMode(state, state.pendingExperienceMode, reducerUtils)
                .copy(pendingExperienceMode = null)
        }

        // Unlock the "Beginner View" (allow free camera movement).
        is MainScreenEvent.UnlockBeginnerView -> state.copy(isBeginnerViewLocked = false)

        // Lock the "Beginner View" (restrict camera movement to simplify UI).
        is MainScreenEvent.LockBeginnerView -> {
            state.copy(
                isBeginnerViewLocked = true,
                // Reset protractor to default.
                protractorUnit = ProtractorUnit(
                    reducerUtils.getDefaultTargetBallPosition(),
                    LOGICAL_BALL_RADIUS,
                    0f
                ),
                // Set a fixed zoom level for beginner mode.
                zoomSliderPosition = 50f
            )
        }

        // Toggle the calibration screen visibility.
        is MainScreenEvent.ToggleCalibrationScreen -> state.copy(showCalibrationScreen = !state.showCalibrationScreen)

        // Toggle the quick align screen visibility.
        is MainScreenEvent.ToggleQuickAlignScreen -> state.copy(showQuickAlignScreen = !state.showQuickAlignScreen)

        // Fallback for unhandled actions.
        else -> state
    }
}

/**
 * Handles the logic for switching between Experience Modes.
 *
 * Resets the UI to a state appropriate for the selected mode (e.g., hiding/showing table, resetting balls).
 */
private fun handleSetExperienceMode(
    state: CueDetatState,
    mode: ExperienceMode,
    reducerUtils: ReducerUtils
): CueDetatState {
    // Create a base new state with common resets.
    var newState = state.copy(
        experienceMode = mode,
        // Reset target ball.
        protractorUnit = ProtractorUnit(
            reducerUtils.getDefaultTargetBallPosition(),
            LOGICAL_BALL_RADIUS,
            0f
        ),
        // Clear obstacles.
        obstacleBalls = emptyList(),
        // Reset zoom.
        zoomSliderPosition = 0f,
        // Reset world rotation.
        worldRotationDegrees = 0f,
        // Clear banking.
        bankingAimTarget = null,
        // Reset change tracker.
        valuesChangedSinceReset = false,
        // Unlock world.
        isWorldLocked = false,
        // Reset view pan.
        viewOffset = PointF(0f, 0f)
    )

    // Apply mode-specific configurations.
    return when (mode) {
        ExperienceMode.EXPERT -> {
            newState.copy(
                // Expert mode: Table visible, cue ball placed, helpers off by default.
                table = newState.table.copy(isVisible = true),
                onPlaneBall = OnPlaneBall(
                    center = reducerUtils.getDefaultCueBallPosition(newState),
                    radius = LOGICAL_BALL_RADIUS
                ),
                areHelpersVisible = false
            )
        }
        ExperienceMode.BEGINNER -> {
            newState.copy(
                // Beginner mode: Table hidden (simple view), no cue ball (simple aim), helpers on.
                table = newState.table.copy(isVisible = false),
                onPlaneBall = null,
                isBankingMode = false,
                areHelpersVisible = true,
                isBeginnerViewLocked = true,
                zoomSliderPosition = 50f
            )
        }
        ExperienceMode.HATER -> {
            // Hater mode: Minimalist/No-nonsense? currently just returns base reset state.
            newState
        }
    }
}

/**
 * Toggles the Banking Mode on or off.
 *
 * When enabling: sets up the banking environment (table visible, cue ball placed, default rotation).
 * When disabling: resets to standard view (expert or beginner depending on mode).
 */
private fun handleToggleBankingMode(
    state: CueDetatState,
    reducerUtils: ReducerUtils
): CueDetatState {
    // Determine the new state (toggle).
    val bankingEnabled = !state.isBankingMode

    val newState = if (bankingEnabled) {
        // ENABLE BANKING:
        // Create a new banking cue ball at center (0,0).
        val newBankingBall = OnPlaneBall(center = PointF(0f, 0f), radius = LOGICAL_BALL_RADIUS)
        // Default rotation for banking view.
        val defaultTableRotation = 90f
        // Calculate where the aim target should be initially.
        val initialAimTarget =
            calculateInitialBankingAimTarget(newBankingBall, defaultTableRotation)

        state.copy(
            isBankingMode = true,
            onPlaneBall = newBankingBall,
            zoomSliderPosition = 0f,
            // Ensure table is visible for banking context.
            table = state.table.copy(isVisible = true),
            worldRotationDegrees = defaultTableRotation,
            bankingAimTarget = initialAimTarget,
            // Reset protractor to center.
            protractorUnit = state.protractorUnit.copy(
                radius = LOGICAL_BALL_RADIUS,
                center = PointF(0f, 0f)
            ),
            warningText = null
        )
    } else {
        // DISABLE BANKING:
        state.copy(
            isBankingMode = false,
            bankingAimTarget = null,
            zoomSliderPosition = 0f,
            // Table visibility depends on experience mode (Expert=Visible, Beginner=Hidden).
            table = state.table.copy(isVisible = state.experienceMode == ExperienceMode.EXPERT),
            worldRotationDegrees = 0f,
            // Reset cue ball to default position.
            onPlaneBall = OnPlaneBall(
                reducerUtils.getDefaultCueBallPosition(state),
                LOGICAL_BALL_RADIUS
            ),
            // Reset protractor to center.
            protractorUnit = state.protractorUnit.copy(
                radius = LOGICAL_BALL_RADIUS,
                center = PointF(0f, 0f)
            ),
            warningText = null
        )
    }

    // Apply a final check to ensure all balls are within valid bounds.
    return reducerUtils.snapViolatingBalls(
        newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false,
            showTutorialOverlay = false,
            viewOffset = PointF(0f, 0f)
        )
    )
}

/**
 * Calculates the initial position for the banking aim target.
 *
 * Places it at a fixed distance from the cue ball in the direction of the table's rotation.
 */
private fun calculateInitialBankingAimTarget(
    cueBall: OnPlaneBall,
    tableRotationDegrees: Float
): PointF {
    // Distance factor (15 radii away).
    val defaultBankingAimDistanceFactor = 15f
    val aimDistance = LOGICAL_BALL_RADIUS * defaultBankingAimDistanceFactor
    // Convert rotation to radians (adjusting by -90 for coordinate system).
    val angleRad = Math.toRadians((tableRotationDegrees - 90.0))

    // Calculate position.
    return PointF(
        cueBall.center.x + (kotlin.math.cos(angleRad)).toFloat() * aimDistance,
        cueBall.center.y + (kotlin.math.sin(angleRad)).toFloat() * aimDistance
    )
}

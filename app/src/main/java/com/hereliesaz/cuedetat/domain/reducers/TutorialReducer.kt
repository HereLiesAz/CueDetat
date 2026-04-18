package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

private const val TUTORIAL_LAST_STEP = 6

/**
 * Reducer responsible for guiding the user through the interface.
 *
 * Handles two categories of event:
 *  1. Tutorial-specific events (StartTutorial, NextTutorialStep, etc.) — always processed.
 *  2. Any other event — checked against the current step's completing action when tutorial is active.
 *
 * Strictly gates Expert-level tasks (SCAN_TABLE) from non-Expert modes.
 */
internal fun reduceTutorialAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.StartTutorial -> state.copy(
            showTutorialOverlay = true,
            currentTutorialStep = 0,
            tutorialHighlight = TutorialHighlightElement.NONE
        )

        is MainScreenEvent.NextTutorialStep -> advanceTutorialStep(state)

        is MainScreenEvent.TutorialBack -> {
            if (state.currentTutorialStep == 0) state
            else {
                val prevStep = state.currentTutorialStep - 1
                state.copy(
                    currentTutorialStep = prevStep,
                    tutorialHighlight = highlightForStep(prevStep, state)
                )
            }
        }

        is MainScreenEvent.EndTutorial -> state.copy(
            showTutorialOverlay = false,
            tutorialHighlight = TutorialHighlightElement.NONE
        )

        is MainScreenEvent.UpdateHighlightAlpha -> state.copy(highlightAlpha = action.alpha)

        else -> {
            if (state.showTutorialOverlay && isTutorialStepCompleted(state, action)) {
                advanceTutorialStep(state)
            } else {
                state
            }
        }
    }
}

private fun advanceTutorialStep(state: CueDetatState): CueDetatState {
    val nextStep = state.currentTutorialStep + 1
    return if (nextStep > TUTORIAL_LAST_STEP) {
        state.copy(showTutorialOverlay = false, tutorialHighlight = TutorialHighlightElement.NONE)
    } else {
        state.copy(
            currentTutorialStep = nextStep,
            tutorialHighlight = highlightForStep(nextStep, state)
        )
    }
}

private fun highlightForStep(step: Int, state: CueDetatState): TutorialHighlightElement = when (step) {
    0 -> TutorialHighlightElement.NONE
    1 -> TutorialHighlightElement.TARGET_BALL
    2 -> TutorialHighlightElement.GHOST_BALL
    3 -> if (state.experienceMode == ExperienceMode.EXPERT && !state.table.isVisible)
        TutorialHighlightElement.SCAN_TABLE
    else
        TutorialHighlightElement.CUE_BALL
    4 -> TutorialHighlightElement.ZOOM_SLIDER
    else -> TutorialHighlightElement.NONE
}

private fun isTutorialStepCompleted(state: CueDetatState, action: MainScreenEvent): Boolean =
    when (state.currentTutorialStep) {
        0 -> action is MainScreenEvent.LogicalDragApplied
        1 -> action is MainScreenEvent.LogicalGestureStarted &&
                isNearTargetBall(action.logicalPoint, state)
        2 -> action is MainScreenEvent.TableRotationApplied
        3 -> if (state.experienceMode == ExperienceMode.EXPERT && !state.table.isVisible)
            action is MainScreenEvent.ToggleTableScanScreen
        else
            action is MainScreenEvent.LogicalDragApplied
        4 -> action is MainScreenEvent.ZoomSliderChanged
        5 -> action is MainScreenEvent.ToggleBankingMode
        else -> false
    }

private fun isNearTargetBall(point: PointF, state: CueDetatState): Boolean {
    val dx = point.x - state.protractorUnit.center.x
    val dy = point.y - state.protractorUnit.center.y
    return kotlin.math.hypot(dx.toDouble(), dy.toDouble()) <= LOGICAL_BALL_RADIUS * 2.0
}

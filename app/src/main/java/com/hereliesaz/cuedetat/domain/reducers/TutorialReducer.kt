package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

internal fun reduceTutorialAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.StartTutorial -> {
            val firstStep = 0
            state.copy(
                showTutorialOverlay = true,
                currentTutorialStep = firstStep,
                tutorialHighlight = getHighlightForStep(firstStep),
                flashingTutorialElement = getHighlightForStep(firstStep),
                valuesChangedSinceReset = true,
                areHelpersVisible = false,
                showLuminanceDialog = false,
                isMoreHelpVisible = false
            )
        }

        is MainScreenEvent.NextTutorialStep -> {
            val nextStep = state.currentTutorialStep + 1
            state.copy(
                currentTutorialStep = nextStep,
                tutorialHighlight = getHighlightForStep(nextStep),
                flashingTutorialElement = getHighlightForStep(nextStep),
                valuesChangedSinceReset = true
            )
        }

        is MainScreenEvent.EndTutorial -> state.copy(
            showTutorialOverlay = false,
            currentTutorialStep = 0,
            tutorialHighlight = TutorialHighlightElement.NONE,
            flashingTutorialElement = null
        )

        is MainScreenEvent.UpdateHighlightAlpha -> state.copy(highlightAlpha = action.alpha)

        else -> state
    }
}

private fun getHighlightForStep(step: Int): TutorialHighlightElement {
    return when (step) {
        1 -> TutorialHighlightElement.TARGET_BALL
        2 -> TutorialHighlightElement.ZOOM_SLIDER
        3 -> TutorialHighlightElement.GHOST_BALL
        4 -> TutorialHighlightElement.GHOST_BALL
        else -> TutorialHighlightElement.NONE
    }
}
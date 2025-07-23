package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorialReducer @Inject constructor() {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.StartTutorial -> {
                val firstStep = 0
                currentState.copy(
                    showTutorialOverlay = true,
                    currentTutorialStep = firstStep,
                    tutorialHighlight = getHighlightForStep(firstStep),
                    valuesChangedSinceReset = true,
                    areHelpersVisible = false,
                    showLuminanceDialog = false,
                    isMoreHelpVisible = false
                )
            }

            is MainScreenEvent.NextTutorialStep -> {
                val nextStep = currentState.currentTutorialStep + 1
                currentState.copy(
                    currentTutorialStep = nextStep,
                    tutorialHighlight = getHighlightForStep(nextStep),
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.EndTutorial -> currentState.copy(
                showTutorialOverlay = false,
                currentTutorialStep = 0,
                tutorialHighlight = TutorialHighlightElement.NONE
            )
            else -> currentState
        }
    }

    private fun getHighlightForStep(step: Int): TutorialHighlightElement {
        return when (step) {
            1 -> TutorialHighlightElement.TARGET_BALL
            2 -> TutorialHighlightElement.GHOST_BALL
            3 -> TutorialHighlightElement.CUE_BALL
            4 -> TutorialHighlightElement.ZOOM_SLIDER
            5 -> TutorialHighlightElement.BANK_BUTTON
            else -> TutorialHighlightElement.NONE
        }
    }
}
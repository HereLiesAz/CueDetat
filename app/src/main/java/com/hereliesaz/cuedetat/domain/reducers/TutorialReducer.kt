package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

/**
 * Reducer responsible for guiding the user through the interface.
 * Strictly gates Expert-level tasks (SCAN_TABLE) from non-Expert modes.
 */
internal fun reduceTutorialAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.StartTutorial -> {
            state.copy(
                showTutorialOverlay = true,
                tutorialType = action.type,
                currentTutorialStep = 0,
                tutorialHighlight = if (action.type == TutorialType.GENERAL) TutorialHighlightElement.CUE_BALL else TutorialHighlightElement.NONE
            )
        }

        is MainScreenEvent.NextTutorialStep -> {
            val nextStep = state.currentTutorialStep + 1

            val highlight = when (state.tutorialType) {
                TutorialType.GENERAL -> {
                    when (nextStep) {
                        1 -> TutorialHighlightElement.GHOST_BALL
                        2 -> TutorialHighlightElement.ZOOM_SLIDER
                        3 -> {
                            if (state.experienceMode == ExperienceMode.EXPERT && !state.table.isVisible) {
                                TutorialHighlightElement.SCAN_TABLE
                            } else {
                                TutorialHighlightElement.TARGET_BALL
                            }
                        }
                        else -> TutorialHighlightElement.NONE
                    }
                }
                else -> TutorialHighlightElement.NONE
            }

            state.copy(
                currentTutorialStep = nextStep,
                tutorialHighlight = highlight
            )
        }

        is MainScreenEvent.EndTutorial -> {
            state.copy(
                showTutorialOverlay = false,
                tutorialHighlight = TutorialHighlightElement.NONE
            )
        }

        is MainScreenEvent.UpdateHighlightAlpha -> {
            state.copy(highlightAlpha = action.alpha)
        }

        else -> state
    }
}
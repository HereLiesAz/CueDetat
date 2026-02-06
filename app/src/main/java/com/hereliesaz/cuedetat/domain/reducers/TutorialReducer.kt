package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

/**
 * Reducer function responsible for handling Tutorial-related events.
 *
 * This manages the state of the onboarding tutorial overlay, including:
 * - Starting/Stopping the tutorial.
 * - Advancing through steps.
 * - Highlighting specific UI elements for each step.
 *
 * @param state The current state.
 * @param action The tutorial event.
 * @return The updated state.
 */
internal fun reduceTutorialAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    // Handle the specific tutorial event.
    return when (action) {
        // Case: User starts the tutorial.
        is MainScreenEvent.StartTutorial -> {
            val firstStep = 0
            state.copy(
                showTutorialOverlay = true, // Show the overlay.
                currentTutorialStep = firstStep, // Reset step index.
                tutorialHighlight = getHighlightForStep(firstStep), // Set initial highlight.
                flashingTutorialElement = getHighlightForStep(firstStep), // Flash the initial element.
                valuesChangedSinceReset = true,
                areHelpersVisible = false, // clear distraction.
                showLuminanceDialog = false, // clear distraction.
                isMoreHelpVisible = false // clear distraction.
            )
        }

        // Case: User taps "Next" or advances the tutorial.
        is MainScreenEvent.NextTutorialStep -> {
            val nextStep = state.currentTutorialStep + 1
            state.copy(
                currentTutorialStep = nextStep,
                tutorialHighlight = getHighlightForStep(nextStep), // Update highlight for new step.
                flashingTutorialElement = getHighlightForStep(nextStep), // Flash the new element.
                valuesChangedSinceReset = true
            )
        }

        // Case: User finishes or dismisses the tutorial.
        is MainScreenEvent.EndTutorial -> state.copy(
            showTutorialOverlay = false, // Hide overlay.
            currentTutorialStep = 0, // Reset step.
            tutorialHighlight = TutorialHighlightElement.NONE, // Clear highlights.
            flashingTutorialElement = null // Stop flashing.
        )

        // Case: Animation update for the highlight transparency/alpha.
        is MainScreenEvent.UpdateHighlightAlpha -> state.copy(highlightAlpha = action.alpha)

        // Fallback: Return state unchanged.
        else -> state
    }
}

/**
 * Maps a tutorial step index to the UI element that should be highlighted.
 *
 * @param step The zero-based step index.
 * @return The [TutorialHighlightElement] enum indicating what to highlight.
 */
private fun getHighlightForStep(step: Int): TutorialHighlightElement {
    return when (step) {
        1 -> TutorialHighlightElement.TARGET_BALL // Step 1: Explain the target ball.
        2 -> TutorialHighlightElement.ZOOM_SLIDER // Step 2: Explain zoom.
        3 -> TutorialHighlightElement.GHOST_BALL // Step 3: Explain ghost ball.
        4 -> TutorialHighlightElement.GHOST_BALL // Step 4: Continue explaining ghost ball (aiming).
        else -> TutorialHighlightElement.NONE // Default: No highlight.
    }
}

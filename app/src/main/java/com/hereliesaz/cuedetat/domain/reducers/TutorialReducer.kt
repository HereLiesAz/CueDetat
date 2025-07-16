// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/TutorialReducer.kt
package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class TutorialReducer @Inject constructor() {
    private val tutorialStepCount = 7

    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.StartTutorial -> state.copy(isTutorialVisible = true, tutorialStep = 0)
            is MainScreenEvent.NextTutorialStep -> {
                val nextStep = state.tutorialStep + 1
                if (nextStep < tutorialStepCount) {
                    state.copy(tutorialStep = nextStep)
                } else {
                    state.copy(isTutorialVisible = false)
                }
            }
            is MainScreenEvent.FinishTutorial -> state.copy(isTutorialVisible = false)
            else -> state
        }
    }
}
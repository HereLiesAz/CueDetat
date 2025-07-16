package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.reducers.*
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateReducer @Inject constructor(
    private val gestureReducer: GestureReducer,
    private val toggleReducer: ToggleReducer,
    private val controlReducer: ControlReducer,
    private val systemReducer: SystemReducer,
    private val actionReducer: ActionReducer,
    private val tutorialReducer: TutorialReducer,
    private val spinReducer: SpinReducer,
    private val cvReducer: CvReducer,
    private val advancedOptionsReducer: AdvancedOptionsReducer
) {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return state
            .let { gestureReducer.reduce(it, event) }
            .let { toggleReducer.reduce(it, event) }
            .let { controlReducer.reduce(it, event) }
            .let { systemReducer.reduce(it, event) }
            .let { actionReducer.reduce(it, event) }
            .let { tutorialReducer.reduce(it, event) }
            .let { spinReducer.reduce(it, event) }
            .let { cvReducer.reduce(it, event) }
            .let { advancedOptionsReducer.reduce(it, event) }
    }
}

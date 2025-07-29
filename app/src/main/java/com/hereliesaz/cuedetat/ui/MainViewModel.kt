package com.hereliesaz.cuedetat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.OverlayState
import com.hereliesaz.cuedetat.domain.stateReducer
import com.hereliesaz.cuedetat.ui.hatemode.HateModeViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// The overall state of the application UI
data class CueDetatState(
    val experienceMode: ExperienceMode = ExperienceMode.NORMAL,
    val overlay: OverlayState = OverlayState.None,
    val haterState: HateModeViewModel.HaterState = HateModeViewModel.HaterState()
)

class MainViewModel : ViewModel() {

    // A channel for receiving actions from the UI.
    // Using a channel ensures that all actions are processed sequentially.
    private val actionChannel = Channel<CueDetatAction>(Channel.UNLIMITED)

    // The single source of truth for the UI state.
    private val _state = MutableStateFlow(CueDetatState())
    val state = _state.asStateFlow()

    // Sub-ViewModel for Hater Mode logic
    private val haterViewModel = HateModeViewModel(viewModelScope, ::dispatch)

    init {
        // Launch a coroutine to process actions from the channel.
        viewModelScope.launch {
            actionChannel.consumeAsFlow().collect { action ->
                val newState = stateReducer(_state.value, action)
                _state.value = newState
            }
        }

        // Observe changes in Hater Mode state and dispatch updates
        viewModelScope.launch {
            haterViewModel.state.distinctUntilChanged().collect { haterState ->
                // This check prevents an infinite loop of state updates.
                // We only update the main state if the hater sub-state has actually changed.
                if (_state.value.haterState != haterState) {
                    _state.value = _state.value.copy(haterState = haterState)
                }
            }
        }
    }

    /**
     * Dispatches an action to be processed. This is the only way to trigger a
     * state change from the UI.
     */
    fun dispatch(action: CueDetatAction) {
        viewModelScope.launch {
            // Special handling for HaterMode actions to delegate to the sub-viewmodel
            if (action is CueDetatAction.HaterAction) {
                haterViewModel.dispatch(action.action)
            } else {
                actionChannel.send(action)
            }
        }
    }
}
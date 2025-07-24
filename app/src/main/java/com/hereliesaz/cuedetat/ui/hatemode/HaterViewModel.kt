package com.hereliesaz.cuedetat.ui.hatemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HaterViewModel @Inject constructor(
    private val shakeDetector: ShakeDetector
) : ViewModel() {

    private val reducer = HaterReducer()
    private val responses = HaterResponses

    private val _state = MutableStateFlow(HaterState())
    val state: StateFlow<HaterState> = _state.asStateFlow()

    init {
        // Observe shake events to trigger Hater Mode
        shakeDetector.shakeFlow
            .onEach {
                onEvent(HaterEvent.ShowHater)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HaterEvent) {
        val currentState = _state.value
        val newState = reducer.reduce(currentState, event)
        _state.value = newState

        // Handle side-effects after state reduction
        if (event is HaterEvent.ShowHater) {
            selectAnswer(currentState.isFirstReveal)
        }
    }

    private fun selectAnswer(isFirstReveal: Boolean) {
        viewModelScope.launch {
            if (isFirstReveal) {
                // On the first reveal, the answer is always group0.
                _state.value = _state.value.copy(
                    currentAnswer = R.drawable.group0,
                    isFirstReveal = false,
                    recentlyUsedAnswers = listOf(R.drawable.group0) // Add it to recently used
                )
            } else {
                // On subsequent reveals, select a new random answer.
                val recentlyUsed = _state.value.recentlyUsedAnswers
                val availableResponses = responses.allResponses.filter { it !in recentlyUsed }

                val newAnswer = if (availableResponses.isNotEmpty()) {
                    availableResponses.random()
                } else {
                    // All answers used, reset the list but still exclude the last answer.
                    val lastAnswer = recentlyUsed.lastOrNull()
                    _state.value =
                        _state.value.copy(recentlyUsedAnswers = listOfNotNull(lastAnswer))
                    responses.allResponses.filter { it != lastAnswer }.random()
                }

                val updatedRecentlyUsed =
                    (_state.value.recentlyUsedAnswers + newAnswer).takeLast(15)
                _state.value = _state.value.copy(
                    currentAnswer = newAnswer,
                    recentlyUsedAnswers = updatedRecentlyUsed
                )
            }
        }
    }
}
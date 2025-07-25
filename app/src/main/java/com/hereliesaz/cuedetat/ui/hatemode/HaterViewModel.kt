package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HaterViewModel @Inject constructor(
    private val shakeDetector: ShakeDetector,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val reducer = HaterReducer()
    private val responses = HaterResponses

    private val _state = MutableStateFlow(HaterState())
    val state: StateFlow<HaterState> = _state.asStateFlow()

    init {
        // Listen for shakes to trigger subsequent responses.
        shakeDetector.shakeFlow
            .onEach {
                if (!_state.value.isCooldownActive) {
                    triggerHaterSequence()
                }
            }
            .launchIn(viewModelScope)

        // Listen for sensor changes for the floating effect.
        sensorRepository.fullOrientationFlow
            .onEach { orientation ->
                onEvent(HaterEvent.UpdateSensorOffset(orientation.roll, orientation.pitch))
            }
            .launchIn(viewModelScope)

        // Automatically trigger the first reveal animation on ViewModel creation.
        viewModelScope.launch {
            if (_state.value.isFirstReveal) {
                // Prime the state with the initial answer.
                selectAnswer()
                // Make the UI visible to start the animation.
                onEvent(HaterEvent.ShowHater)

                // Start a cooldown to prevent an immediate shake from interrupting.
                _state.value = _state.value.copy(isCooldownActive = true)
                delay(2500)
                _state.value = _state.value.copy(isCooldownActive = false)
            }
        }
    }

    fun onEvent(event: HaterEvent) {
        val currentState = _state.value
        val newState = reducer.reduce(currentState, event)
        _state.value = newState
    }

    private fun triggerHaterSequence() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCooldownActive = true)

            if (_state.value.currentAnswer != null) {
                onEvent(HaterEvent.HideHater)
                delay(1000) // Wait for submerge animation
            }

            selectAnswer()

            onEvent(HaterEvent.ShowHater)

            launch {
                delay(2500) // Cooldown just longer than the full animation
                _state.value = _state.value.copy(isCooldownActive = false)
            }
        }
    }

    private fun selectAnswer() {
        val newAnswer: Int
        if (_state.value.isFirstReveal) {
            newAnswer = R.drawable.group0
            _state.value = _state.value.copy(
                isFirstReveal = false,
                recentlyUsedAnswers = listOf(R.drawable.group0)
            )
        } else {
            val recentlyUsed = _state.value.recentlyUsedAnswers
            val availableResponses = responses.allResponses.filter { it !in recentlyUsed }

            newAnswer = if (availableResponses.isNotEmpty()) {
                availableResponses.random()
            } else {
                val lastAnswer = recentlyUsed.lastOrNull()
                _state.value = _state.value.copy(recentlyUsedAnswers = listOfNotNull(lastAnswer))
                responses.allResponses.filter { it != lastAnswer }.randomOrNull()
                    ?: R.drawable.group0
            }
        }

        val updatedRecentlyUsed = (_state.value.recentlyUsedAnswers + newAnswer).takeLast(15)

        _state.value = _state.value.copy(
            currentAnswer = newAnswer,
            recentlyUsedAnswers = updatedRecentlyUsed,
            randomRotation = Random.nextFloat() * 90f - 45f, // Subtler rotation
            randomOffset = Offset(
                x = Random.nextFloat() * 0.4f - 0.2f,
                y = Random.nextFloat() * 0.4f - 0.2f
            ),
            gradientStart = Offset(Random.nextFloat(), Random.nextFloat()),
            gradientEnd = Offset(Random.nextFloat(), Random.nextFloat())
        )
    }
}
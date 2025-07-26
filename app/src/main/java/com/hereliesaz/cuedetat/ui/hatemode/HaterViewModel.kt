// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterViewModel.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.hypot
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

    private var physicsJob: Job? = null

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
        startPhysicsLoop()
    }

    fun onEvent(event: HaterEvent) {
        val currentState = _state.value
        val newState = reducer.reduce(currentState, event)
        _state.value = newState
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (true) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f // Delta time in seconds
                lastTime = currentTime

                updatePhysics(deltaTime)

                delay(16) // Aim for ~60 FPS
            }
        }
    }

    private fun updatePhysics(deltaTime: Float) {
        _state.value = _state.value.let { s ->
            var (ax, ay) = s.gravity

            if (s.isUserDragging) {
                // User's touch applies a direct force
                ax += s.touchForce.x * 2f // Multiply force to make it feel responsive
                ay += s.touchForce.y * 2f
            }

            var vx = s.velocity.x + ax
            var vy = s.velocity.y + ay

            // Apply viscous damping to simulate thick liquid
            vx *= 0.85f
            vy *= 0.85f

            var newAngle = s.angle
            var newAngularVelocity = s.angularVelocity

            if (s.isUserDragging && hypot(s.touchForce.x, s.touchForce.y) > 0.1) {
                // Pushing off-center creates torque
                newAngularVelocity += s.touchForce.x * 0.1f
            }

            newAngularVelocity *= 0.90f // Angular damping
            newAngle += newAngularVelocity

            s.copy(
                position = s.position + Offset(vx, vy),
                velocity = Offset(vx, vy),
                angle = newAngle,
                angularVelocity = newAngularVelocity,
                touchForce = Offset.Zero // Consume the force after applying it
            )
        }
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
            // Reset physics state for the new answer
            position = Offset.Zero,
            velocity = Offset.Zero,
            angle = 0f,
            angularVelocity = 0f,
            gradientStart = Offset(Random.nextFloat(), Random.nextFloat()),
            gradientEnd = Offset(Random.nextFloat(), Random.nextFloat())
        )
    }
}
package com.hereliesaz.cuedetat.ui.hatemode

import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HaterViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val shakeDetector: ShakeDetector
) : ViewModel() {

    private val _haterState = MutableStateFlow(HaterState())
    val haterState = _haterState.asStateFlow()

    private val physicsManager = HaterPhysicsManager()
    private var physicsJob: Job? = null

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.White.toArgb()
    }

    private val masterAnswerList = listOf(
        "Ask again, but with less hope.",
        "Outlook hazy. Try to not suck.",
        "System busy judging you. Try again later.",
        "That's not your fingers. Don't count on it.",
        "Go home. You're drunk.",
        "42",
        "You're asking the wrong questions.",
        "Bless your heart.",
        "Ask again. More feeling this time.",
        "What a terrible idea.",
        "Without a doubt. You will be disappointed.",
        "It is as certain as the heat-death of the universe but will take longer.",
        "Outlook doubtful. Try looking inward.",
        "The odds are forever not in your favor.",
        "Yes. And by that, I mean absolutely not.",
        "Your guaranteed success is also a non-linear failure.",
        "It is written in nuclear fallout.",
        "Yes, but in the wrong universe.",
        "Three known things can survive an atomic holocaust: roaches, twinkies, and stupid fucking questions like that.",
        "Nah.",
        "The end is nigh and you ask me this?",
        "Reply hazy, the bar is on fire.",
        "Thank you for asking! And the laugh.",
        "The giraffe screams yes from a velvet mailbox.",
        "Green is not the color of maybe.",
        "Invert the spoon and dance.",
        "Your question ate itself and said, \"Delicious.\"",
        "Your future is shaped like a falling piano.",
        "Advice is a sandwich you forgot to invent.",
        "All questions like this start with the letter Q.",
        "The cat was Schrödinger's therapist. All but certain.",
        "Yes, if gravity permits.",
        "Onions are opinions, sans Pi.",
        "Paint your doubt with the bones of clocks.",
        "Nonsense is the only honest answer.",
        "Reality declined the invitation.",
        "Flip the idea inside out and wear it like it fits.",
        "The oracle sneezed. That's your sign.",
        "Fork your expectations.",
        "The crows know, but they won't tell.",
        "Ask again after the dream ends.",
        "Everything is true until spoken aloud.",
        "That question is the butterfly effect that ends humanity.",
        "I’d agree, but then we’d both be wrong.",
        "Ask again but try crying softer. The void is sensitive.",
        "Even your apathy is underwhelming.",
        "Wow. Edge of my non-existent seat.",
        "Life’s short. So is your attention span.",
        "Try giving a damn and maybe someone else will.",
        "No. It’s not you. It’s reality."
    )
    private var remainingAnswers = mutableListOf<String>()

    init {
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collect { orientation ->
                onEvent(HaterEvent.SensorChanged(orientation.roll, orientation.pitch))
            }
        }
        viewModelScope.launch {
            shakeDetector.shakeFlow.collect { onEvent(HaterEvent.ShakeDetected) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        physicsJob?.cancel()
    }

    fun onEvent(event: HaterEvent) {
        when (event) {
            is HaterEvent.EnterHaterMode -> enterHaterMode()
            is HaterEvent.ShakeDetected -> onShake()
            is HaterEvent.Dragging -> physicsManager.pushDie(event.delta)
            is HaterEvent.SensorChanged -> physicsManager.applyGravity(event.roll, event.pitch)
            is HaterEvent.DragEnd -> { /* no-op */
            }
        }
    }

    private fun enterHaterMode() {
        reshuffleAnswers()
        viewModelScope.launch {
            physicsManager.updateDieAndText(_haterState.value.answer, textPaint)
            physicsManager.createParticles()
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
            startPhysics()
            delay(1000)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
        }
    }

    private fun startPhysics() {
        if (physicsJob?.isActive == true) return
        physicsJob = viewModelScope.launch {
            while (true) {
                physicsManager.step()
                _haterState.value = _haterState.value.copy(
                    particles = physicsManager.particlePositions,
                    diePosition = physicsManager.diePosition,
                    dieAngle = physicsManager.dieAngle
                )
                delay(16L) // ~60 FPS
            }
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        physicsManager.setupBoundaries(width, height)
    }

    private fun onShake() {
        val currentState = _haterState.value.triangleState
        if (currentState == TriangleState.IDLE || currentState == TriangleState.SETTLING) {
            viewModelScope.launch {
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SUBMERGING)

                physicsManager.agitateParticles(viewModelScope) {
                    // This callback runs after the initial agitation force is removed.
                    viewModelScope.launch {
                        // Restore gravity to the current sensor reading.
                        val currentOrientation = sensorRepository.fullOrientationFlow.first()
                        physicsManager.applyGravity(
                            currentOrientation.roll,
                            currentOrientation.pitch
                        )
                    }
                }

                delay(1500) // Give time for particles to churn

                if (remainingAnswers.isEmpty()) reshuffleAnswers()
                val newAnswer = remainingAnswers.removeAt(0)
                _haterState.value = _haterState.value.copy(answer = newAnswer)
                physicsManager.updateDieAndText(newAnswer, textPaint)

                _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
                delay(1000)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
            }
        }
    }

    private fun reshuffleAnswers() {
        remainingAnswers = masterAnswerList.shuffled().toMutableList()
    }
}
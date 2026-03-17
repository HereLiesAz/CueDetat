package com.hereliesaz.cuedetat.ui.hatemode

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

    private var remainingResIds = mutableListOf<Int>()

    private fun reshuffleAnswers() {
        remainingResIds = HaterResponses.allResponses.shuffled().toMutableList()
    }

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
        physicsManager.destroy()
    }

    fun onEvent(event: HaterEvent) {
        when (event) {
            is HaterEvent.EnterHaterMode -> enterHaterMode()
            is HaterEvent.ShakeDetected -> onShake()
            is HaterEvent.Dragging -> physicsManager.pushDie(event.delta)
            is HaterEvent.SensorChanged -> physicsManager.applyGravity(event.roll, event.pitch)
            is HaterEvent.DragEnd -> { /* no-op */ }
        }
    }

    private fun enterHaterMode() {
        reshuffleAnswers()
        viewModelScope.launch {
            physicsManager.setPhase(TriangleState.EMERGING)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
            startPhysics()
            delay(1000)
            physicsManager.setPhase(TriangleState.SETTLING)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
        }
    }

    private fun startPhysics() {
        if (physicsJob?.isActive == true) return
        physicsJob = viewModelScope.launch {
            while (true) {
                physicsManager.step()
                _haterState.value = _haterState.value.copy(
                    diePosition = physicsManager.diePosition,
                    dieAngle    = physicsManager.dieAngle,
                    dieScale    = physicsManager.currentDieScale
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
                physicsManager.setPhase(TriangleState.SUBMERGING)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SUBMERGING)

                physicsManager.agitate(viewModelScope) {
                    viewModelScope.launch {
                        val currentOrientation = sensorRepository.fullOrientationFlow.first()
                        physicsManager.applyGravity(
                            currentOrientation.roll,
                            currentOrientation.pitch
                        )
                    }
                }

                delay(1500)

                if (remainingResIds.isEmpty()) reshuffleAnswers()
                val newResId = remainingResIds.removeAt(0)
                _haterState.value = _haterState.value.copy(answerResId = newResId)

                physicsManager.setPhase(TriangleState.EMERGING)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
                delay(1000)
                physicsManager.setPhase(TriangleState.SETTLING)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
            }
        }
    }
}

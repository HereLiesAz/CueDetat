package com.hereliesaz.cuedetat.ui.hatemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class HaterViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val shakeDetector: ShakeDetector,
) : ViewModel() {

    private val _haterState = MutableStateFlow(HaterState())
    val haterState = _haterState.asStateFlow()

    private val physicsManager = HaterPhysicsManager()
    private var physicsJob: Job? = null

    private var remainingIndices = mutableListOf<Int>()

    private fun reshuffleAnswers() {
        remainingIndices = HaterResponses.allAnswers.indices.shuffled().toMutableList()
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

    // Last orientation we reacted to, so a near-still phone (tiny EMA jitter) doesn't
    // wake the paused physics loop on every sensor sample.
    private var lastWakeRoll = 0f
    private var lastWakePitch = 0f

    fun onEvent(event: HaterEvent) {
        when (event) {
            is HaterEvent.EnterHaterMode -> enterHaterMode()
            is HaterEvent.ShakeDetected -> onShake()
            is HaterEvent.Dragging -> {
                physicsManager.pushDie(event.delta, event.position)
                ensurePhysicsRunning()
            }
            is HaterEvent.SensorChanged -> {
                physicsManager.applyGravity(event.roll, event.pitch)
                // Only wake the loop when the phone was actually tilted, not on jitter.
                if (abs(event.roll - lastWakeRoll) > WAKE_TILT_DEGREES ||
                    abs(event.pitch - lastWakePitch) > WAKE_TILT_DEGREES
                ) {
                    lastWakeRoll = event.roll
                    lastWakePitch = event.pitch
                    ensurePhysicsRunning()
                }
            }
            is HaterEvent.DragEnd -> {
                physicsManager.onDragEnd()
                ensurePhysicsRunning()
            }
        }
    }

    private fun enterHaterMode() {
        reshuffleAnswers()
        viewModelScope.launch {
            physicsManager.setPhase(TriangleState.EMERGING)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
            startPhysics()
            delay(1000L)
            physicsManager.setPhase(TriangleState.SETTLING)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
        }
    }

    /** Restart the physics loop if it has paused (settled). No-op while already running. */
    private fun ensurePhysicsRunning() = startPhysics()

    private fun startPhysics() {
        if (physicsJob?.isActive == true) return
        physicsJob = viewModelScope.launch(Dispatchers.Default) {
            // ~30fps. We previously drove this from withFrameNanos on
            // AndroidUiDispatcher.Main to align with the choreographer, but in
            // release builds the dispatched context did not carry a
            // MonotonicFrameClock and the call crashed on entry to Hater Mode
            // (IllegalStateException: A MonotonicFrameClock is not available).
            // A simple delay loop on Dispatchers.Default is sufficient for this
            // physics sim and has no frame clock requirements. 30fps is plenty for
            // a floating die and halves the per-frame CPU vs the old 60fps.
            while (isActive) {
                physicsManager.step()
                _haterState.value = _haterState.value.copy(
                    diePosition = physicsManager.diePosition,
                    dieAngle    = physicsManager.dieAngle,
                    dieScale    = physicsManager.currentDieScale,
                    rockAngleX  = physicsManager.currentRockX,
                    rockAngleY  = physicsManager.currentRockY,
                )
                // Battery: stop stepping once everything has settled. A perturbing input
                // (drag, shake, tilt, phase change) relaunches the loop via ensurePhysicsRunning().
                if (physicsManager.isAtRest) break
                delay(FRAME_DELAY_MS)
            }
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        physicsManager.setupBoundaries(width, height)
    }

    private fun onShake() {
        val currentState = _haterState.value.triangleState
        if ((currentState == TriangleState.IDLE) || (currentState == TriangleState.SETTLING)) {
            viewModelScope.launch {
                ensurePhysicsRunning() // loop may have paused while settled
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

                delay(1500L)

                if (remainingIndices.isEmpty()) reshuffleAnswers()
                val newIndex = remainingIndices.removeAt(0)
                _haterState.value = _haterState.value.copy(answerIndex = newIndex)

                physicsManager.setPhase(TriangleState.EMERGING)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
                delay(1000L)
                physicsManager.setPhase(TriangleState.SETTLING)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
            }
        }
    }

    private companion object {
        const val FRAME_DELAY_MS = 33L      // ~30fps physics step
        const val WAKE_TILT_DEGREES = 0.5f  // orientation change that resumes a paused loop
    }
}

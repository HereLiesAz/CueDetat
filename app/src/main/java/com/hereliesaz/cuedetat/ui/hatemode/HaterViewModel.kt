// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterViewModel.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import com.kphysics.Body
import com.kphysics.BodyDef
import com.kphysics.BodyType
import com.kphysics.CircleShape
import com.kphysics.FixtureDef
import com.kphysics.World
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
    private var physicsWorld: World? = null
    private var triangleBody: Body? = null

    init {
        initializePhysicsWorld()

        shakeDetector.shakeFlow
            .onEach {
                if (!_state.value.isCooldownActive) {
                    triggerHaterSequence()
                }
            }
            .launchIn(viewModelScope)

        sensorRepository.fullOrientationFlow
            .onEach { orientation ->
                // Directly update the physics world's gravity
                val gravityX = -orientation.roll * 0.1f
                val gravityY = orientation.pitch * 0.1f
                physicsWorld?.setGravity(Offset(gravityX, gravityY))
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            if (_state.value.isFirstReveal) {
                selectAnswer()
                onEvent(HaterEvent.ShowHater)
                _state.value = _state.value.copy(isCooldownActive = true)
                delay(2500)
                _state.value = _state.value.copy(isCooldownActive = false)
            }
        }
        startPhysicsLoop()
    }

    private fun initializePhysicsWorld() {
        physicsWorld = World(gravity = Offset.Zero).apply {
            // Define world boundaries if the library supports them,
            // otherwise, we rely on damping.
        }
    }

    fun onEvent(event: HaterEvent) {
        val currentState = _state.value
        val newState = reducer.reduce(currentState, event)
        _state.value = newState

        if (event is HaterEvent.DragTriangle) {
            // Apply drag force directly in the ViewModel
            val forceMagnitude = 5000f // A value to make the drag feel responsive
            val force = event.delta * forceMagnitude
            triangleBody?.applyForceToCenter(force)
        }
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (true) {
                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastTime) / 1_000_000_000.0f
                lastTime = currentTime

                // Step the physics world
                physicsWorld?.step(deltaTime, 8, 3)

                // Update UI state from physics body
                triangleBody?.let { body ->
                    _state.value = _state.value.copy(
                        position = body.position,
                        angle = Math.toDegrees(body.angle.toDouble()).toFloat()
                    )
                }

                delay(16) // Aim for ~60 FPS
            }
        }
    }

    private fun triggerHaterSequence() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCooldownActive = true)

            if (_state.value.currentAnswer != null) {
                onEvent(HaterEvent.HideHater)
                delay(1000)
            }

            selectAnswer()

            onEvent(HaterEvent.ShowHater)

            launch {
                delay(2500)
                _state.value = _state.value.copy(isCooldownActive = false)
            }
        }
    }

    private fun selectAnswer() {
        // ... (existing answer selection logic remains the same)
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
            randomRotation = Random.nextFloat() * 90f - 45f,
            gradientStart = Offset(Random.nextFloat(), Random.nextFloat()),
            gradientEnd = Offset(Random.nextFloat(), Random.nextFloat())
        )

        // Create a new physics body for the new answer
        createTriangleBody()
    }

    private fun createTriangleBody() {
        physicsWorld?.let { world ->
            // Destroy the old body if it exists
            triangleBody?.let { world.destroyBody(it) }

            // Define the new body
            val bodyDef = BodyDef(
                type = BodyType.Dynamic,
                position = Offset.Zero,
                angle = 0f,
                linearDamping = 5.0f,  // High damping for a floaty feel
                angularDamping = 5.0f
            )
            val body = world.createBody(bodyDef)

            // Define its shape and physical properties
            val shape = CircleShape(radius = 150f) // Using a circle for simpler physics
            val fixtureDef = FixtureDef(
                shape = shape,
                density = 0.5f,
                friction = 0.3f,
                restitution = 0.5f // Bounciness
            )
            body.createFixture(fixtureDef)
            triangleBody = body
        }
    }
}
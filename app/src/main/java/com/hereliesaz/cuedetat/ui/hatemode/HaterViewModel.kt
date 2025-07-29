package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.minigdx.kphysics.body.BodyDefinition
import com.github.minigdx.kphysics.body.BodyType
import com.github.minigdx.kphysics.fixture.FixtureDefinition
import com.github.minigdx.kphysics.shape.CircleShape
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.math.Vec2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class HaterViewModel @Inject constructor(
) : ViewModel() {

    private val reducer = HaterReducer()

    private val _state = MutableStateFlow(HaterState())
    val state = _state.asStateFlow()

    private var haterCooldownJob: Job? = null
    private var physicsJob: Job? = null
    private val world = World(Vec2(0.0, 9.8))
    private var triangleBody: Body? = null


    fun onEvent(event: HaterEvent) {
        val newState = reducer.reduce(_state.value, event)
        _state.value = newState

        when (event) {
            is HaterEvent.EnterHaterMode -> {
                resetForNewSession()
                startPhysicsLoop()
            }

            is HaterEvent.ShowHater -> {
                if (!_state.value.isCooldownActive) {
                    showNewHaterResponse()
                }
            }

            is HaterEvent.UpdateSensorOffset -> {
                // Apply gravity based on sensor data
                val gravityX = event.roll * 0.2f
                val gravityY = event.pitch * 0.2f
                world.gravity = Vec2(gravityX.toDouble(), gravityY.toDouble())
            }

            is HaterEvent.DragTriangle -> {
                triangleBody?.apply {
                    val force = Vec2(event.delta.x.toDouble() * 200, event.delta.y.toDouble() * 200)
                    applyForceToCenter(force)
                }
            }

            else -> {}
        }
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (true) {
                world.step(1 / 60f, 8, 3)
                triangleBody?.let { body ->
                    _state.value = _state.value.copy(
                        position = Offset(body.position.x, body.position.y),
                        angle = Math.toDegrees(body.angle.toDouble()).toFloat()
                    )
                }
                delay(16) // ~60fps
            }
        }
    }

    private fun resetForNewSession() {
        world.clear()
        triangleBody = null
        _state.value = HaterState(isFirstReveal = true)
        showNewHaterResponse(isInitialReveal = true)
    }

    private fun showNewHaterResponse(isInitialReveal: Boolean = false) {
        viewModelScope.launch {
            if (!isInitialReveal) {
                _state.value = _state.value.copy(isHaterVisible = false)
                delay(1000)
            }

            val availableResponses =
                HaterResponses.allResponses.filterNot { _state.value.recentlyUsedAnswers.contains(it) }
            val nextAnswer = if (availableResponses.isEmpty()) {
                _state.value = _state.value.copy(recentlyUsedAnswers = emptyList())
                HaterResponses.allResponses.random()
            } else {
                availableResponses.random()
            }

            // Create a new physics body for the new answer
            triangleBody?.let { world.destroyBody(it) }
            triangleBody = createTriangleBody()


            _state.value = _state.value.copy(
                currentAnswer = nextAnswer,
                isHaterVisible = true,
                isCooldownActive = true,
                randomRotation = Random.nextFloat() * 90f - 45f,
                gradientStart = Offset(Random.nextFloat() * 1000, Random.nextFloat() * 1000),
                gradientEnd = Offset(Random.nextFloat() * 1000, Random.nextFloat() * 1000),
                recentlyUsedAnswers = (_state.value.recentlyUsedAnswers + nextAnswer).takeLast(10)
            )

            haterCooldownJob?.cancel()
            haterCooldownJob = launch {
                delay(5000)
                _state.value = _state.value.copy(isHaterVisible = false, isCooldownActive = false)
            }
        }
    }

    private fun createTriangleBody(): Body {
        val bodyDef = BodyDefinition(type = BodyType.DYNAMIC, position = Vec2(0.0, 0.0))
        val body = world.createBody(bodyDef).apply {
            userData = UUID.randomUUID().toString()
            linearDamping = 0.8f
            angularDamping = 0.9f
        }
        val circle = CircleShape(radius = 150f)
        val fixtureDef =
            FixtureDefinition(shape = circle, density = 0.5f, friction = 0.3f, restitution = 0.5f)
        body.createFixture(fixtureDef)
        circle.dispose()
        return body
    }

    override fun onCleared() {
        super.onCleared()
        physicsJob?.cancel()
        world.dispose()
    }
}
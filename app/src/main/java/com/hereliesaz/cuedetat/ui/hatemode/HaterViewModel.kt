package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.dynamics.bodies.BodyType
import de.chaffic.dynamics.fixture.Fixture
import de.chaffic.geometry.Circle
import de.chaffic.math.Vec2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class HaterViewModel @Inject constructor() : ViewModel() {

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
                world.step(1 / 60f)
                triangleBody?.let { body ->
                    _state.value = _state.value.copy(
                        position = Offset(body.position.x.toFloat(), body.position.y.toFloat()),
                        angle = Math.toDegrees(body.angle).toFloat()
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
        val body = Body()
        body.bodyType = BodyType.DYNAMIC
        body.position = Vec2(0.0, 0.0)
        body.userData = UUID.randomUUID().toString()
        body.linearDamping = 0.8
        body.angularDamping = 0.9

        val circle = Circle(150.0)
        val fixture = Fixture(circle)
        fixture.density = 0.5
        fixture.friction = 0.3
        fixture.restitution = 0.5
        body.addFixture(fixture)

        world.addBody(body)
        return body
    }


    override fun onCleared() {
        super.onCleared()
        physicsJob?.cancel()
        world.dispose()
    }
}
// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterViewModel.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.dynamics.bodies.BodyType
import de.chaffic.geometry.Circle
import de.chaffic.geometry.Polygon
import de.chaffic.math.Vec2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

class HaterViewModel @Inject constructor() : ViewModel() {

    private val world = World(Vec2(0.0, 0.0))
    private var physicsJob: Job? = null
    private var haterBody: Body? = null
    private var recentlyUsedAnswers = mutableListOf<Int>()

    private val _state = MutableStateFlow(HaterState())
    val state = _state.asStateFlow()

    private var screenWidthDp: Dp = 0.dp
    private var screenHeightDp: Dp = 0.dp

    fun onEvent(event: HaterEvent) {
        when (event) {
            is HaterEvent.EnterHaterMode -> initialize()
            is HaterEvent.SensorChanged -> {
                val gravityX = event.roll * 9.8f
                val gravityY = event.pitch * 9.8f
                world.gravity = Vec2(gravityX.toDouble(), gravityY.toDouble())
            }

            is HaterEvent.Dragging -> {
                haterBody?.apply {
                    val force =
                        Vec2(event.delta.x.toDouble() * 1000, event.delta.y.toDouble() * 1000)
                    applyForceToCenter(force)
                }
            }

            is HaterEvent.ScreenTapped -> showNewAnswer()
            else -> {}
        }
    }

    fun setScreenSize(width: Dp, height: Dp) {
        screenWidthDp = width
        screenHeightDp = height
    }

    private fun initialize() {
        world.clear()
        createBoundaries()
        showNewAnswer(isInitial = true)
        startPhysicsLoop()
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (true) {
                world.step(1 / 60f)
                _state.value = _state.value.copy(bodies = world.bodies)
                delay(16) // ~60fps
            }
        }
    }

    private fun showNewAnswer(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (!isInitial) {
                _state.value = _state.value.copy(isAnswerVisible = false)
                delay(500)
            }

            haterBody?.let { world.destroyBody(it) }

            val availableAnswers =
                HaterResponses.allResponses.filterNot { it in recentlyUsedAnswers }
            val answer = if (availableAnswers.isNotEmpty()) {
                availableAnswers.random()
            } else {
                recentlyUsedAnswers.clear()
                HaterResponses.allResponses.random()
            }

            recentlyUsedAnswers.add(answer)
            if (recentlyUsedAnswers.size > 10) {
                recentlyUsedAnswers.removeAt(0)
            }

            haterBody = createHaterBody().also {
                it.userData = answer
                world.addBody(it)
            }
            _state.value = _state.value.copy(currentAnswer = answer, isAnswerVisible = true)
        }
    }

    private fun createHaterBody(): Body {
        val body = Body()
        body.bodyType = BodyType.DYNAMIC
        body.position = Vec2(screenWidthDp.value * 0.5, screenHeightDp.value * 0.5)
        body.angularVelocity = Random.nextDouble(-2.0, 2.0)

        val shape = Circle(60.0)
        val fixture = body.createFixture(shape)
        fixture.density = 0.7
        fixture.friction = 0.4
        fixture.restitution = 0.5

        return body
    }

    private fun createBoundaries() {
        val thickness = 100.0
        val width = screenWidthDp.value.toDouble()
        val height = screenHeightDp.value.toDouble()

        // Floor
        createBoundary(width / 2, height + thickness / 2, width, thickness)
        // Ceiling
        createBoundary(width / 2, -thickness / 2, width, thickness)
        // Left Wall
        createBoundary(-thickness / 2, height / 2, thickness, height)
        // Right Wall
        createBoundary(width + thickness / 2, height / 2, thickness, height)
    }

    private fun createBoundary(x: Double, y: Double, width: Double, height: Double) {
        val body = Body()
        body.bodyType = BodyType.STATIC
        body.position.set(x, y)
        val shape = Polygon(width, height)
        body.createFixture(shape)
        world.addBody(body)
    }

    override fun onCleared() {
        super.onCleared()
        physicsJob?.cancel()
    }
}
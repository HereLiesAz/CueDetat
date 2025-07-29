package com.hereliesaz.cuedetat.ui.hatemode

import com.github.minigdx.kphysics.body.BodyDefinition
import com.github.minigdx.kphysics.body.BodyType
import com.github.minigdx.kphysics.fixture.FixtureDefinition
import com.github.minigdx.kphysics.shape.CircleShape
import com.hereliesaz.cuedetat.domain.CueDetatAction
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.math.Vec2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HaterViewModel(
    private val viewModelScope: CoroutineScope,
    private val globalDispatch: (CueDetatAction) -> Unit
) {

    // --- State & Actions ---
    data class BodyState(
        val id: String,
        val x: Float,
        val y: Float,
        val angle: Float
    )

    data class HaterState(
        val haterText: String = "I hate...",
        val bodies: List<BodyState> = emptyList()
    )

    sealed class Action {
        data class UpdatePhysics(val bodies: List<Body>) : Action()
        data class SetHaterText(val text: String) : Action()
    }

    private val _state = MutableStateFlow(HaterState())
    val state = _state.asStateFlow()

    // --- Physics Engine ---
    private val world: World
    private val bodies = mutableMapOf<String, Body>()
    private var physicsJob: Job? = null

    init {
        // Create a world with no gravity
        world = World(gravity = Vec2(0.0, 0.0))
        startPhysics()
    }

    fun dispatch(action: Action) {
        val newState = haterStateReducer(_state.value, action)
        _state.value = newState
    }

    private fun startPhysics() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (true) {
                world.step(1 / 60f, 8, 3)
                // Dispatch an update to the global state container
                val physicsUpdateAction = Action.UpdatePhysics(bodies.values.toList())
                dispatch(physicsUpdateAction)
                delay(16) // Roughly 60 FPS
            }
        }
    }

    fun addHaterObject(x: Float, y: Float) {
        val id = UUID.randomUUID().toString()
        val body = createBody(id, x, y)
        bodies[id] = body

        // Apply a random impulse to make it interesting
        val force = Vec2(
            ((Math.random().toFloat() - 0.5f) * 1000).toDouble(),
            ((Math.random().toFloat() - 0.5f) * 1000).toDouble()
        )
        body.applyForceToCenter(force)
    }

    fun clearAll() {
        viewModelScope.launch {
            bodies.values.forEach { body ->
                world.destroyBody(body)
            }
            bodies.clear()
            dispatch(Action.UpdatePhysics(emptyList()))
        }
    }

    private fun createBody(id: String, x: Float, y: Float): Body {
        val bodyDef = BodyDefinition(
            type = BodyType.DYNAMIC,
            position = Vec2(x, y)
        )
        val body = world.createBody(bodyDef).apply {
            userData = id
        }

        val circle = CircleShape(radius = 10f) // Example radius
        val fixtureDef = FixtureDefinition(
            shape = circle,
            density = 0.5f,
            friction = 0.3f,
            restitution = 0.5f // Bounciness
        )
        body.createFixture(fixtureDef)
        circle.dispose()
        return body
    }

    private fun haterStateReducer(currentState: HaterState, action: Action): HaterState {
        return when (action) {
            is Action.UpdatePhysics -> {
                currentState.copy(
                    bodies = action.bodies.mapNotNull { body ->
                        val bodyId = body.userData as? String ?: return@mapNotNull null
                        BodyState(
                            id = bodyId,
                            x = body.position.x,
                            y = body.position.y,
                            angle = body.angle
                        )
                    }
                )
            }
            is Action.SetHaterText -> {
                currentState.copy(haterText = action.text)
            }
        }
    }

    fun onCleared() {
        physicsJob?.cancel()
        world.dispose()
    }
}
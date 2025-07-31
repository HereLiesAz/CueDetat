// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterViewModel.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.liquidfun.Body
import org.liquidfun.BodyDef
import org.liquidfun.BodyType
import org.liquidfun.FixtureDef
import org.liquidfun.PolygonShape
import org.liquidfun.Vec2
import org.liquidfun.World
import javax.inject.Inject
import kotlin.math.sqrt
import kotlin.random.Random

@HiltViewModel
class HaterViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val shakeDetector: ShakeDetector
) : ViewModel() {

    private val _haterState = MutableStateFlow(HaterState())
    val haterState = _haterState.asStateFlow()

    private val world = World(0f, 0f)
    private var dieBody: Body? = null
    private var isInitialized = false
    private var physicsJob: Job? = null

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

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
            shakeDetector.shakeFlow.collect {
                onEvent(HaterEvent.ShakeDetected)
            }
        }
    }

    fun onEvent(event: HaterEvent) {
        when (event) {
            is HaterEvent.EnterHaterMode -> enterHaterMode()
            is HaterEvent.ShakeDetected -> onShake()
            is HaterEvent.Dragging -> pushDie(event.delta)
            is HaterEvent.SensorChanged -> applyGravity(event.roll, event.pitch)
            is HaterEvent.DragEnd -> { /* No action needed */
            }
        }
    }

    private fun enterHaterMode() {
        reshuffleAnswers()
        viewModelScope.launch {
            while (!isInitialized) {
                delay(16)
            }
            updateDieAndText(_haterState.value.answer, screenWidth, 2.5f)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
            startPhysicsLoop()
            delay(1000)
            _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
        }
    }

    private fun startPhysicsLoop() {
        if (physicsJob?.isActive == true) return
        physicsJob = viewModelScope.launch {
            while (true) {
                world.step(1 / 60f, 8, 3)
                dieBody?.let { body ->
                    _haterState.value = _haterState.value.copy(
                        diePosition = Offset(body.position.x, body.position.y),
                        dieAngle = Math.toDegrees(body.angle.toDouble()).toFloat()
                    )
                }
                delay(16L) // ~60 FPS
            }
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        if (isInitialized) return

        this.screenWidth = width
        this.screenHeight = height

        val halfW = width / 2f
        val halfH = height / 2f
        val thickness = 50f // A thick, invisible wall

        // Top Wall
        val topWallDef =
            BodyDef().apply { type = BodyType.staticBody; position.set(0f, -halfH - thickness / 2) }
        val topWall = world.createBody(topWallDef)
        val topShape = PolygonShape().apply { setAsBox(halfW, thickness / 2) }
        topWall.createFixture(topShape, 0f)

        // Bottom Wall
        val bottomWallDef =
            BodyDef().apply { type = BodyType.staticBody; position.set(0f, halfH + thickness / 2) }
        val bottomWall = world.createBody(bottomWallDef)
        val bottomShape = PolygonShape().apply { setAsBox(halfW, thickness / 2) }
        bottomWall.createFixture(bottomShape, 0f)

        // Left Wall
        val leftWallDef =
            BodyDef().apply { type = BodyType.staticBody; position.set(-halfW - thickness / 2, 0f) }
        val leftWall = world.createBody(leftWallDef)
        val leftShape = PolygonShape().apply { setAsBox(thickness / 2, halfH) }
        leftWall.createFixture(leftShape, 0f)

        // Right Wall
        val rightWallDef =
            BodyDef().apply { type = BodyType.staticBody; position.set(halfW + thickness / 2, 0f) }
        val rightWall = world.createBody(rightWallDef)
        val rightShape = PolygonShape().apply { setAsBox(thickness / 2, halfH) }
        rightWall.createFixture(rightShape, 0f)

        isInitialized = true
    }

    fun updateDieAndText(text: String, canvasWidth: Float, density: Float) {
        if (canvasWidth == 0f) return

        textPaint.textSize = 22.sp.value * density
        val layoutWidth = (canvasWidth * 0.7f)
        val staticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

        val textHeight = staticLayout.height.toFloat()
        val triangleHeight = textHeight * 7.5f

        val topY = -(2.0f / 3.0f) * triangleHeight
        val bottomY = (1.0f / 3.0f) * triangleHeight
        val halfWidth = triangleHeight / (2 * sqrt(3.0f / 4.0f)) / 2
        val newVertices = floatArrayOf(
            0f, topY,
            -halfWidth, bottomY,
            halfWidth, bottomY
        )

        val trianglePath = Path().apply {
            moveTo(0f, topY)
            lineTo(-halfWidth, bottomY)
            lineTo(halfWidth, bottomY)
            close()
        }
        _haterState.value = _haterState.value.copy(diePath = trianglePath)

        dieBody?.let { world.destroyBody(it) }

        val bodyDef = BodyDef().apply {
            type = BodyType.dynamicBody
            position.set(Random.nextFloat() * 50 - 25, Random.nextFloat() * 50 - 25)
            linearDamping = 1.5f
            angularDamping = 2.0f
        }
        val body = world.createBody(bodyDef)
        val shape = PolygonShape().apply { set(newVertices, 3) }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1.0f
            friction = 0.3f
            restitution = 0.6f
        }
        body.createFixture(fixtureDef)
        dieBody = body
    }

    private fun onShake() {
        val currentState = _haterState.value.triangleState
        if (currentState == TriangleState.IDLE || currentState == TriangleState.SETTLING) {
            viewModelScope.launch {
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SUBMERGING)
                delay(1000)

                if (remainingAnswers.isEmpty()) {
                    reshuffleAnswers()
                }
                val newAnswer = remainingAnswers.removeAt(0)
                _haterState.value = _haterState.value.copy(answer = newAnswer)
                updateDieAndText(newAnswer, screenWidth, 2.5f)

                _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
                delay(1000)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
            }
        }
    }

    private fun pushDie(delta: Offset) {
        val impulse = Vec2(delta.x * 2f, delta.y * 2f)
        dieBody?.applyLinearImpulse(impulse, dieBody!!.worldCenter, true)
        angularVelocity += (delta.x * Random.nextFloat() * 0.1f)
    }

    private fun applyGravity(roll: Float, pitch: Float) {
        val gravityMultiplier = 15f
        world.setGravity(Vec2(roll * gravityMultiplier, pitch * gravityMultiplier))
    }

    private fun reshuffleAnswers() {
        remainingAnswers = masterAnswerList.shuffled().toMutableList()
    }

    override fun onCleared() {
        physicsJob?.cancel()
        world.release()
        super.onCleared()
    }
}
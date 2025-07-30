// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterViewModel.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.geometry.Polygon
import de.chaffic.math.Vec2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.random.Random

class HaterViewModel : ViewModel() {

    private val _haterState = MutableStateFlow(HaterState())
    val haterState = _haterState.asStateFlow()

    private val world = World(Vec2(0.0, 0.0))
    private var dieBody: Body? = null
    private var wallBodies = mutableListOf<Body>()
    private var physicsJob: Job? = null

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.White.toArgb()
    }

    private var staticLayout: StaticLayout? = null

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
            updateDieAndText(_haterState.value.answer)
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
                world.step(1.0 / 60.0)
                dieBody?.let { body ->
                    _haterState.value = _haterState.value.copy(
                        diePosition = Offset(body.position.x.toFloat(), body.position.y.toFloat()),
                        dieAngle = body.orientation.toFloat() * (180f / PI.toFloat())
                    )
                }
                delay(16L) // ~60 FPS
            }
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        if (wallBodies.isNotEmpty()) return

        val thickness = 200.0
        val halfW = width / 2.0
        val halfH = height / 2.0

        val topWall = Body(Polygon(width.toDouble(), thickness))
        topWall.position.set(0.0, -halfH - thickness / 2)
        topWall.setStatic()
        world.addBody(topWall)
        wallBodies.add(topWall)

        val bottomWall = Body(Polygon(width.toDouble(), thickness))
        bottomWall.position.set(0.0, halfH + thickness / 2)
        bottomWall.setStatic()
        world.addBody(bottomWall)
        wallBodies.add(bottomWall)

        val leftWall = Body(Polygon(thickness, height.toDouble()))
        leftWall.position.set(-halfW - thickness / 2, 0.0)
        leftWall.setStatic()
        world.addBody(leftWall)
        wallBodies.add(leftWall)

        val rightWall = Body(Polygon(thickness, height.toDouble()))
        rightWall.position.set(halfW + thickness / 2, 0.0)
        rightWall.setStatic()
        world.addBody(rightWall)
        wallBodies.add(rightWall)
    }

    fun updateDieAndText(text: String, density: Float = 2.5f) {
        textPaint.textSize = 16.sp.value * density
        val layoutWidth = 200.dp.value * density
        staticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

        val textWidth = staticLayout!!.width.toFloat()
        val textHeight = staticLayout!!.height.toFloat()

        val padding = 30.dp.value * density
        val triangleHeight = textHeight + padding
        val sideLength = (triangleHeight / (kotlin.math.sqrt(3.0) / 2.0)).toFloat()
        val triangleWidth = max(textWidth + padding, sideLength)

        val halfHeight = triangleHeight / 2.0
        val halfWidth = triangleWidth / 2.0

        val newVertices = arrayOf(
            Vec2(0.0, -halfHeight),
            Vec2(-halfWidth, halfHeight),
            Vec2(halfWidth, halfHeight)
        )

        dieBody?.let { world.removeBody(it) }

        val shape = Polygon(newVertices)
        val body = Body(shape)
        body.position.set(Random.nextDouble() * 50 - 25, Random.nextDouble() * 50 - 25)
        body.orientation = Random.nextDouble() * 2 * PI
        body.dynamicFriction = 0.6
        body.staticFriction = 0.6
        body.density = 1.0
        body.linearDampening = 5.0
        body.angularDampening = 7.0

        dieBody = body
        world.addBody(dieBody!!)
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
                val newAnswer = remainingAnswers.removeFirst()
                _haterState.value = _haterState.value.copy(answer = newAnswer)
                updateDieAndText(newAnswer)

                _haterState.value = _haterState.value.copy(triangleState = TriangleState.EMERGING)
                delay(1000)
                _haterState.value = _haterState.value.copy(triangleState = TriangleState.SETTLING)
            }
        }
    }

    private fun pushDie(delta: Offset) {
        dieBody?.applyLinearImpulse(
            Vec2(delta.x.toDouble(), delta.y.toDouble()).scalar(0.1),
            dieBody!!.position
        )
    }

    private fun applyGravity(roll: Float, pitch: Float) {
        val gravityMultiplier = 0.1f
        world.gravity.set(
            (roll * gravityMultiplier).toDouble(),
            (pitch * gravityMultiplier).toDouble()
        )
    }

    private fun reshuffleAnswers() {
        remainingAnswers = masterAnswerList.shuffled().toMutableList()
    }
}
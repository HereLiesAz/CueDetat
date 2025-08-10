package com.hereliesaz.cuedetat.ui.hatemode

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.geometry.Polygon
import de.chaffic.math.Vec2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

class HaterPhysicsManager {

    // --- Public State ---
    var particlePositions: List<Offset> = emptyList()
        private set
    var diePosition: Offset = Offset.Zero
        private set
    var dieAngle: Float = 0f
        private set

    // --- Physics World ---
    private val world = World(Vec2(0.0, 0.0))
    private val particles = mutableListOf<Body>()
    private var dieBody: Body? = null

    // --- Scaling & Dimensions ---
    private val PPM = 100.0f // Pixels Per Meter
    private var viewWidth = 0f
    private var viewHeight = 0f

    fun destroy() {
        // kphysics is pure Kotlin, so GC should handle it.
    }

    /** Steps the physics world forward by one frame and updates public state. */
    fun step() {
        world.step(1.0 / 60.0)

        particlePositions = particles.map {
            Offset(it.position.x.toFloat() * PPM, it.position.y.toFloat() * PPM)
        }

        dieBody?.let { body ->
            diePosition = Offset(body.position.x.toFloat() * PPM, body.position.y.toFloat() * PPM)
            dieAngle = body.orientation.toFloat() * (180f / Math.PI.toFloat())
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        if (viewWidth != 0f) return
        viewWidth = width
        viewHeight = height

        val w = viewWidth / PPM
        val h = viewHeight / PPM
        val thickness = 0.1f

        val topWall = Body(Polygon(w.toDouble(), thickness.toDouble()), (w / 2f).toDouble(), 0.0)
        topWall.density = 0.0
        val bottomWall = Body(Polygon(w.toDouble(), thickness.toDouble()), (w / 2f).toDouble(), h.toDouble())
        bottomWall.density = 0.0
        val leftWall = Body(Polygon(thickness.toDouble(), h.toDouble()), 0.0, (h / 2f).toDouble())
        leftWall.density = 0.0
        val rightWall = Body(Polygon(thickness.toDouble(), h.toDouble()), w.toDouble(), (h / 2f).toDouble())
        rightWall.density = 0.0

        world.addBody(topWall)
        world.addBody(bottomWall)
        world.addBody(leftWall)
        world.addBody(rightWall)
    }

    fun updateDieAndText(text: String, textPaint: TextPaint) {
        textPaint.textSize = 22.sp.value
        val layoutWidth = (200.dp.value).toInt()
        val staticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        val textHeight = staticLayout.height.toFloat()
        val padding = 30.dp.value
        val triangleHeight = (textHeight + padding) / PPM
        val sideLength = (triangleHeight / (sqrt(3.0) / 2.0)).toFloat()
        val halfWidth = (sideLength / 2.0f)

        val topY = -(2.0f / 3.0f) * triangleHeight
        val bottomY = (1.0f / 3.0f) * triangleHeight

        val vertices = arrayOf(
            Vec2(0.0, topY.toDouble()), Vec2(-halfWidth.toDouble(), bottomY.toDouble()), Vec2(halfWidth.toDouble(), bottomY.toDouble())
        )

        dieBody?.let { world.removeBody(it) }

        val shape = Polygon(vertices)
        val body = Body(shape, (viewWidth / 2f / PPM).toDouble(), (viewHeight / 2f / PPM).toDouble())
        body.density = 2.5
        body.staticFriction = 0.5
        body.dynamicFriction = 0.3
        body.restitution = 0.2

        dieBody = body
        world.addBody(body)
    }


    fun createParticles() {
        if (particles.isNotEmpty()) return

        val numParticles = 500
        val particleRadius = 0.05f
        for (i in 0 until numParticles) {
            val x = Random.nextFloat() * (viewWidth / PPM)
            val y = Random.nextFloat() * (viewHeight / PPM)
            val particleBody = Body(de.chaffic.geometry.Circle(particleRadius.toDouble()), x.toDouble(), y.toDouble())
            particleBody.density = 1.0
            particleBody.restitution = 0.5
            particles.add(particleBody)
            world.addBody(particleBody)
        }
    }

    fun agitateParticles(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        val strength = 150f
        val xForce = (Random.nextFloat() - 0.5f) * strength
        val yForce = (Random.nextFloat() - 0.5f) * strength
        world.gravity = Vec2(xForce.toDouble(), yForce.toDouble())

        scope.launch {
            delay(300)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        val forceMultiplier = 500.0f // Increased force for better effect
        dieBody?.applyForce(Vec2(delta.x * forceMultiplier.toDouble(), delta.y * forceMultiplier.toDouble()))
    }

    fun applyGravity(roll: Float, pitch: Float) {
        val gravityMultiplier = 9.8f
        world.gravity = Vec2(
            (roll / 90f) * gravityMultiplier.toDouble(),
            (-pitch / 90f) * gravityMultiplier.toDouble()
        )
    }
}
package com.hereliesaz.cuedetat.ui.hatemode

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pirckheimer_gymnasium.jbox2d.collision.shapes.PolygonShape
import de.pirckheimer_gymnasium.jbox2d.common.Vec2
import de.pirckheimer_gymnasium.jbox2d.dynamics.Body
import de.pirckheimer_gymnasium.jbox2d.dynamics.BodyDef
import de.pirckheimer_gymnasium.jbox2d.dynamics.BodyType
import de.pirckheimer_gymnasium.jbox2d.dynamics.FixtureDef
import de.pirckheimer_gymnasium.jbox2d.dynamics.World
import de.pirckheimer_gymnasium.jbox2d.particle.ParticleGroupDef
import de.pirckheimer_gymnasium.jbox2d.particle.ParticleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Encapsulates all JBox2D/LiquidFun physics simulation logic for
 * HaterMode. This class is not thread-safe and should be managed by a
 * single owner (e.g., a ViewModel).
 */
class HaterPhysicsManager {

    // --- Public State ---
    var particlePositions: List<Offset> = emptyList()
        private set
    var diePosition: Offset = Offset.Zero
        private set
    var dieAngle: Float = 0f
        private set

    // --- Physics World ---
    private val world = World(Vec2(0.0f, 0.0f))
    private var dieBody: Body? = null

    // --- Scaling & Dimensions ---
    private val PPM = 100.0f // Pixels Per Meter
    private var viewWidth = 0f
    private var viewHeight = 0f

    init {
        world.setParticleRadius(0.15f)
        world.setParticleDamping(0.2f)
    }

    // Since this class doesn't own a lifecycle, the owner must call destroy.
    fun destroy() {
        // JBox2D doesn't have an explicit world.delete(), relying on GC.
        // The native LiquidFun wrapper did, but this port does not.
    }

    /** Steps the physics world forward by one frame and updates public state. */
    fun step() {
        world.step(1.0f / 60.0f, 8, 3)

        particlePositions = world.getParticlePositionBuffer().map {
            Offset(it.x * PPM, it.y * PPM)
        }

        dieBody?.let { body ->
            diePosition = Offset(body.position.x * PPM, body.position.y * PPM)
            dieAngle = body.angle * (180f / Math.PI.toFloat())
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        if (viewWidth != 0f) return
        viewWidth = width
        viewHeight = height

        val w = viewWidth / PPM
        val h = viewHeight / PPM
        val thickness = 0.1f // in meters

        val wallPositions = listOf(
            Vec2(0f, -h / 2f) to Vec2(w, thickness),   // Top
            Vec2(0f, h / 2f) to Vec2(w, thickness),    // Bottom
            Vec2(-w / 2f, 0f) to Vec2(thickness, h),   // Left
            Vec2(w / 2f, 0f) to Vec2(thickness, h)     // Right
        )

        wallPositions.forEach { (pos, size) ->
            val bd = BodyDef().apply {
                type = BodyType.STATIC
                position.set(pos)
            }
            val shape = PolygonShape().apply { setAsBox(size.x / 2f, size.y / 2f) }
            val wall = world.createBody(bd)
            wall.createFixture(shape, 0.0f)
        }
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
            Vec2(0.0f, topY), Vec2(-halfWidth, bottomY), Vec2(halfWidth, bottomY)
        )

        dieBody?.let { world.destroyBody(it) }

        val bd = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(0f, 0f)
            linearDamping = 0.5f
            angularDamping = 0.8f
        }
        val shape = PolygonShape().apply { set(vertices, 3) }
        val fd = FixtureDef().apply {
            this.shape = shape
            density = 2.5f // Denser than water (1.0) so it sinks
            friction = 0.3f
            restitution = 0.2f
        }
        dieBody = world.createBody(bd).apply { createFixture(fd) }
    }


    fun createParticles() {
        if (world.getParticleCount() > 0) return

        val pgd = ParticleGroupDef().apply {
            val shape = PolygonShape()
            shape.setAsBox(viewWidth / PPM * 0.4f, viewHeight / PPM * 0.4f)
            this.shape = shape
            flags = ParticleType.waterParticle
            position.set(0f, 0f)
        }
        world.createParticleGroup(pgd)
    }

    fun agitateParticles(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        val strength = 150f
        val xForce = (Random.nextFloat() - 0.5f) * strength
        val yForce = (Random.nextFloat() - 0.5f) * strength
        world.gravity = Vec2(xForce, yForce)

        scope.launch {
            delay(300)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        val forceMultiplier = 0.05f
        dieBody?.applyForceToCenter(
            Vec2(delta.x * forceMultiplier, delta.y * forceMultiplier)
        )
    }

    fun applyGravity(roll: Float, pitch: Float) {
        val gravityMultiplier = 9.8f
        world.gravity.set(
            (roll / 90f) * gravityMultiplier,
            (-pitch / 90f) * gravityMultiplier
        )
    }
}
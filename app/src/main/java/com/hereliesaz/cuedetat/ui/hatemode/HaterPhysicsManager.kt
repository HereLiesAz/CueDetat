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
import de.pirckheimer_gymnasium.jbox2d.particle.ParticleSystem
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

    companion object {
        private const val PPM = 100.0f // Pixels Per Meter
        private const val PARTICLE_RADIUS = 0.15f
        private const val PARTICLE_DAMPING = 0.2f
        private const val TIME_STEP = 1.0f / 60.0f
        private const val VELOCITY_ITERATIONS = 8
        private const val POSITION_ITERATIONS = 3
        private const val WALL_THICKNESS = 0.1f
        private const val DIE_TEXT_SIZE_SP = 22f
        private const val DIE_LAYOUT_WIDTH_DP = 200f
        private const val DIE_PADDING_DP = 30f
        private const val DIE_DENSITY = 2.5f
        private const val DIE_FRICTION = 0.55f // Average of 0.7 static and 0.4 dynamic
        private const val DIE_RESTITUTION = 0.2f
        private const val DIE_LINEAR_DAMPING = 0.5f
        private const val DIE_ANGULAR_DAMPING = 0.8f
        private const val PARTICLE_BOX_MULTIPLIER = 0.4f
        private const val AGITATION_STRENGTH = 150f
        private const val AGITATION_DELAY_MS = 300L
        private const val PUSH_FORCE_MULTIPLIER = 10.0f
        private const val GRAVITY_MULTIPLIER = 9.8f
    }

    // --- Public State ---
    var particlePositions: List<Offset> = emptyList()
        private set
    var diePosition: Offset = Offset.Zero
        private set
    var dieAngle: Float = 0f
        private set

    // --- Physics World ---
    private val world = World(Vec2(0.0f, 0.0f))
    private val particleSystem: ParticleSystem
    private var dieBody: Body? = null

    // --- Scaling & Dimensions ---
    private var viewWidth = 0f
    private var viewHeight = 0f

    init {
        val pDef = ParticleSystemDef()
        pDef.radius = PARTICLE_RADIUS
        pDef.dampingStrength = PARTICLE_DAMPING
        particleSystem = world.createParticleSystem(pDef)
    }

    // Since this class doesn't own a lifecycle, the owner must call destroy.
    fun destroy() {
        // Destroy the die body if it exists
        dieBody?.let {
            world.destroyBody(it)
            dieBody = null
        }

        // Destroy all particles in the particle system
        if (particleSystem.particleCount > 0) {
            // No bulk delete, so we have to iterate.
            // Iterating backwards to avoid index shifting issues.
            for (i in particleSystem.particleCount - 1 downTo 0) {
                particleSystem.destroyParticle(i, false)
            }
        }
    }

    /** Steps the physics world forward by one frame and updates public state. */
    fun step() {
        world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)

        particlePositions = particleSystem.particlePositionBuffer.map {
            Offset(it.x * PPM, it.y * PPM)
        }

        dieBody?.let { body ->
            diePosition = Offset(body.position.x * PPM, body.position.y * PPM)
            // body.angle is in radians, converting to degrees for UI rendering.
            dieAngle = body.angle * (180f / Math.PI.toFloat())
        }
    }

    fun setupBoundaries(width: Float, height: Float) {
        if (viewWidth != 0f) return
        viewWidth = width
        viewHeight = height

        val w = viewWidth / PPM
        val h = viewHeight / PPM

        val wallPositions = listOf(
            Vec2(0f, -h / 2f) to Vec2(w, WALL_THICKNESS),   // Top
            Vec2(0f, h / 2f) to Vec2(w, WALL_THICKNESS),    // Bottom
            Vec2(-w / 2f, 0f) to Vec2(WALL_THICKNESS, h),   // Left
            Vec2(w / 2f, 0f) to Vec2(WALL_THICKNESS, h)     // Right
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
        textPaint.textSize = DIE_TEXT_SIZE_SP.sp.value
        val layoutWidth = DIE_LAYOUT_WIDTH_DP.dp.value.toInt()
        val staticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        val textHeight = staticLayout.height.toFloat()
        val padding = DIE_PADDING_DP.dp.value
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
            linearDamping = DIE_LINEAR_DAMPING
            angularDamping = DIE_ANGULAR_DAMPING
        }
        val shape = PolygonShape().apply { set(vertices, 3) }
        val fd = FixtureDef().apply {
            this.shape = shape
            density = DIE_DENSITY // Denser than water (1.0) so it sinks
            // Using a single friction value as JBox2D does not separate static and dynamic friction.
            // This value is an average of the suggested 0.7 static and 0.4 dynamic friction.
            friction = DIE_FRICTION
            restitution = DIE_RESTITUTION
        }
        dieBody = world.createBody(bd).apply { createFixture(fd) }
    }


    fun createParticles() {
        if (particleSystem.particleCount > 0) return

        // Using JBox2D's ParticleSystem for efficient simulation of many particles.
        // This is more performant than creating individual rigid bodies for each particle.
        val pgd = ParticleGroupDef().apply {
            val shape = PolygonShape()
            shape.setAsBox(viewWidth / PPM * PARTICLE_BOX_MULTIPLIER, viewHeight / PPM * PARTICLE_BOX_MULTIPLIER)
            this.shape = shape
            flags = ParticleType.b2_waterParticle
            position.set(0f, 0f)
        }
        particleSystem.createParticleGroup(pgd)
    }

    fun agitateParticles(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        val xForce = (Random.nextFloat() - 0.5f) * AGITATION_STRENGTH
        val yForce = (Random.nextFloat() - 0.5f) * AGITATION_STRENGTH
        world.gravity = Vec2(xForce, yForce)

        scope.launch {
            delay(AGITATION_DELAY_MS)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        // Adjusted force multiplier for realistic die movement; tune as needed based on simulation results.
        val force = Vec2(delta.x * PUSH_FORCE_MULTIPLIER, delta.y * PUSH_FORCE_MULTIPLIER)
        dieBody?.applyForce(force, dieBody!!.worldCenter)
    }

    fun applyGravity(roll: Float, pitch: Float) {
        world.gravity.set(
            (roll / 90f) * GRAVITY_MULTIPLIER,
            (-pitch / 90f) * GRAVITY_MULTIPLIER
        )
    }
}
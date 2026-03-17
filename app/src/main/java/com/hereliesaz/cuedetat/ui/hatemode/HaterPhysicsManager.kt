// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterPhysicsManager.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pure-Kotlin Newtonian physics simulation for HaterMode.
 *
 * Manages a rigid-body die floating in a 2-D fluid of particles, responding to
 * gravity (device tilt), user drag, and shake agitation.
 */
class HaterPhysicsManager {

    // --- Boundaries ---
    private var screenWidth  = 0f
    private var screenHeight = 0f
    private val dieRadius    = 150f

    // --- Die state ---
    private var diePos     = Offset.Zero
    private var dieVel     = Offset.Zero
    private var angle      = 0f
    private var angularVel = 0f

    // --- Gravity vector (set by sensor) ---
    private var gravityVec = Offset.Zero

    // --- Phase ---
    private var phase: TriangleState = TriangleState.IDLE

    // --- Particles (parallel lists to avoid per-frame allocation) ---
    private val particlePos = mutableListOf<Offset>()
    private val particleVel = mutableListOf<Offset>()

    // --- Public state ---

    /** Absolute screen positions of all fluid particles. */
    var particlePositions: List<Offset> = emptyList()
        private set

    /** Current center position of the die (offset from screen center). */
    var diePosition: Offset = Offset.Zero
        private set

    /** Current rotation of the die in degrees. */
    var dieAngle: Float = 0f
        private set

    // --- Constants ---
    companion object {
        private const val GRAVITY_SCALE    = 0.18f
        private const val DAMPING          = 0.97f
        private const val ANG_DAMPING      = 0.96f
        private const val RESTITUTION      = 0.35f
        private const val SPRING_STRENGTH  = 0.012f
        private const val PARTICLE_COUNT   = 65
        private const val PARTICLE_DAMPING = 0.985f
        private const val AGITATE_FORCE    = 28f
    }

    private fun rng(min: Float, max: Float) = Random.nextFloat() * (max - min) + min

    // --- API ---

    fun setupBoundaries(width: Float, height: Float) {
        screenWidth  = width
        screenHeight = height
        // Reclamp existing particles to new bounds
        val hw = width / 2f
        val hh = height / 2f
        for (i in particlePos.indices) {
            val p = particlePos[i]
            particlePos[i] = Offset(
                p.x.coerceIn(-hw, hw),
                p.y.coerceIn(-hh, hh)
            )
        }
    }

    fun createParticles() {
        particlePos.clear()
        particleVel.clear()
        val hw = screenWidth / 2f
        val hh = screenHeight / 2f
        repeat(PARTICLE_COUNT) {
            particlePos.add(Offset(rng(-hw, hw), rng(-hh, hh)))
            particleVel.add(Offset.Zero)
        }
        publishParticlePositions()
    }

    fun step() {
        if (screenWidth == 0f) return

        // --- Die ---
        dieVel += gravityVec * GRAVITY_SCALE

        if (phase == TriangleState.SETTLING) {
            dieVel += -diePos * SPRING_STRENGTH
        }

        dieVel  *= DAMPING
        diePos  += dieVel
        angularVel *= ANG_DAMPING
        angle   += angularVel

        // Boundary bounce X
        val maxX = screenWidth / 2f - dieRadius
        if (diePos.x > maxX) {
            diePos = Offset(maxX, diePos.y)
            dieVel = Offset(-dieVel.x * RESTITUTION, dieVel.y)
        } else if (diePos.x < -maxX) {
            diePos = Offset(-maxX, diePos.y)
            dieVel = Offset(-dieVel.x * RESTITUTION, dieVel.y)
        }

        // Boundary bounce Y
        val maxY = screenHeight / 2f - dieRadius
        if (diePos.y > maxY) {
            diePos = Offset(diePos.x, maxY)
            dieVel = Offset(dieVel.x, -dieVel.y * RESTITUTION)
        } else if (diePos.y < -maxY) {
            diePos = Offset(diePos.x, -maxY)
            dieVel = Offset(dieVel.x, -dieVel.y * RESTITUTION)
        }

        diePosition = diePos
        dieAngle    = angle

        // --- Particles ---
        val hw = screenWidth / 2f
        val hh = screenHeight / 2f
        for (i in particlePos.indices) {
            val nudge = Offset(rng(-0.4f, 0.4f), rng(-0.4f, 0.4f))
            particleVel[i] = (particleVel[i] + nudge) * PARTICLE_DAMPING
            var p = particlePos[i] + particleVel[i]

            // Soft boundary wrap
            if (p.x >  hw) p = Offset(p.x - screenWidth,  p.y)
            if (p.x < -hw) p = Offset(p.x + screenWidth,  p.y)
            if (p.y >  hh) p = Offset(p.x, p.y - screenHeight)
            if (p.y < -hh) p = Offset(p.x, p.y + screenHeight)

            particlePos[i] = p
        }

        publishParticlePositions()
    }

    fun setPhase(newPhase: TriangleState) {
        phase = newPhase
        when (newPhase) {
            TriangleState.SUBMERGING -> {
                dieVel     = Offset(rng(-3f, 3f), 18f)
                angularVel = rng(-4f, 4f)
            }
            TriangleState.EMERGING -> {
                diePos     = Offset(rng(-60f, 60f), screenHeight / 2f + dieRadius + 20f)
                dieVel     = Offset(rng(-1f, 1f), -16f)
                angularVel = rng(-2f, 2f)
            }
            TriangleState.SETTLING -> { /* spring activated in step() */ }
            TriangleState.IDLE     -> { /* free float */ }
        }
    }

    fun agitateParticles(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        // Random impulse to every particle
        for (i in particlePos.indices) {
            particleVel[i] = Offset(
                rng(-AGITATE_FORCE, AGITATE_FORCE),
                rng(-AGITATE_FORCE, AGITATE_FORCE)
            )
        }
        // Send the die tumbling during the shake
        dieVel     += Offset(rng(-12f, 12f), rng(-12f, 12f))
        angularVel += rng(-8f, 8f)

        scope.launch {
            delay(1200)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        dieVel     += delta * 0.12f
        angularVel += (delta.x - delta.y) * 0.04f
    }

    fun applyGravity(roll: Float, pitch: Float) {
        gravityVec = Offset(
            sin(Math.toRadians(roll.toDouble())).toFloat(),
            sin(Math.toRadians(pitch.toDouble())).toFloat()
        )
    }

    fun destroy() {
        particlePos.clear()
        particleVel.clear()
    }

    // --- Helpers ---

    private fun publishParticlePositions() {
        particlePositions = particlePos.toList()
    }
}

// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterPhysicsManager.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pure-Kotlin physics simulation for Hater Mode.
 *
 * Models a die floating in a viscous liquid: XY position with damped gravity response,
 * angular momentum, and a Z-axis scale spring that drives emergence/submergence.
 * No particle system.
 */
class HaterPhysicsManager {

    // --- Boundaries ---
    private var screenWidth  = 0f
    private var screenHeight = 0f
    private val dieRadius    = 150f

    // --- Die XY state ---
    private var diePos     = Offset.Zero
    private var dieVel     = Offset.Zero
    private var angle      = 0f
    private var angularVel = 0f

    // --- Gravity vector (set by sensor) ---
    private var gravityVec = Offset.Zero

    // --- Phase ---
    private var phase: TriangleState = TriangleState.IDLE

    // --- Z-scale (depth) state ---
    private var dieScale    = 0f
    private var scaleVel    = 0f
    private var targetScale = 1f
    private var bobPhase    = 0f

    // --- Public state ---
    var diePosition: Offset = Offset.Zero
        private set
    var dieAngle: Float = 0f
        private set
    var currentDieScale: Float = 0f
        private set

    companion object {
        private const val GRAVITY_SCALE   = 0.08f   // sluggish in thick liquid
        private const val DAMPING         = 0.90f   // high viscosity
        private const val ANG_DAMPING     = 0.94f
        private const val RESTITUTION     = 0.20f   // barely bounces
        private const val SPRING_STRENGTH = 0.010f  // gentle return to center
        private const val SCALE_SPRING    = 0.04f   // Z-depth spring stiffness
        private const val SCALE_DAMPING   = 0.88f   // Z-depth spring damping
        private const val BOB_SPEED       = 0.025f  // radians per frame
        private const val BOB_AMPLITUDE   = 0.03f   // ±3% scale oscillation
    }

    private fun rng(min: Float, max: Float) = Random.nextFloat() * (max - min) + min

    fun setupBoundaries(width: Float, height: Float) {
        screenWidth  = width
        screenHeight = height
    }

    fun step() {
        if (screenWidth == 0f) return

        // --- XY physics ---
        dieVel += gravityVec * GRAVITY_SCALE

        // Spring back to center in SETTLING and IDLE
        if (phase == TriangleState.SETTLING || phase == TriangleState.IDLE) {
            dieVel += -diePos * SPRING_STRENGTH
        }

        dieVel     *= DAMPING
        diePos     += dieVel
        angularVel *= ANG_DAMPING
        angle      += angularVel

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

        // --- Z-scale spring ---
        scaleVel += (targetScale - dieScale) * SCALE_SPRING
        scaleVel *= SCALE_DAMPING
        dieScale = (dieScale + scaleVel).coerceIn(0f, 1.12f) // allow tiny overshoot

        // --- Bob: sinusoidal depth oscillation while settling ---
        var publicScale = dieScale
        if (phase == TriangleState.SETTLING) {
            bobPhase += BOB_SPEED
            publicScale += BOB_AMPLITUDE * sin(bobPhase.toDouble()).toFloat()
        }

        diePosition    = diePos
        dieAngle       = angle
        currentDieScale = publicScale.coerceAtLeast(0f)
    }

    fun setPhase(newPhase: TriangleState) {
        phase = newPhase
        when (newPhase) {
            TriangleState.SUBMERGING -> {
                targetScale = 0f
                // Small XY tumble as it sinks
                dieVel     = Offset(rng(-2f, 2f), rng(-2f, 2f))
                angularVel = rng(-3f, 3f)
            }
            TriangleState.EMERGING -> {
                // Reset scale from zero — it grows up from the deep
                dieScale    = 0f
                scaleVel    = 0f
                targetScale = 1f
                // Start near center with a tiny drift
                diePos  = Offset(rng(-40f, 40f), rng(-40f, 40f))
                dieVel  = Offset(rng(-0.5f, 0.5f), rng(-0.5f, 0.5f))
                angularVel = rng(-2f, 2f)
                bobPhase = 0f
            }
            TriangleState.SETTLING -> {
                targetScale = 1f
                bobPhase    = 0f
            }
            TriangleState.IDLE -> {
                targetScale = 1f
            }
        }
    }

    /**
     * Briefly agitates the die (used during the shake/reveal sequence).
     * Calls [onAgitationComplete] after [delay] ms so the ViewModel can swap the answer.
     */
    fun agitate(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        dieVel     += Offset(rng(-10f, 10f), rng(-10f, 10f))
        angularVel += rng(-6f, 6f)
        scope.launch {
            delay(1200)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        dieVel     += delta * 0.08f                   // thick liquid: less responsive to touch
        angularVel += (delta.x - delta.y) * 0.03f
    }

    fun applyGravity(roll: Float, pitch: Float) {
        gravityVec = Offset(
            sin(Math.toRadians(roll.toDouble())).toFloat(),
            sin(Math.toRadians(pitch.toDouble())).toFloat()
        )
    }

    fun destroy() { /* stateless — nothing to release */ }
}

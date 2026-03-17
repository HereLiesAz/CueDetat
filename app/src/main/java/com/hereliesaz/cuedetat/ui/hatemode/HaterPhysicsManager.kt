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
 * Die floats in viscous liquid: gravity-driven XY drift with curved deceleration,
 * no spring back to center, Z-scale spring for emergence/submergence, and a combined
 * scale + Y-position bob while settling.
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
        private const val GRAVITY_SCALE = 0.06f   // slow, deliberate gravity drift
        private const val DAMPING       = 0.97f   // graceful curved deceleration (~380ms to half-speed)
        private const val ANG_DAMPING   = 0.98f
        private const val RESTITUTION   = 0.05f   // near-dead stop at walls
        private const val BUOYANCY      = 0.30f   // upward lift applied only during EMERGING
        private const val SCALE_SPRING  = 0.018f  // slow, deliberate Z-scale rise
        private const val SCALE_DAMPING = 0.96f   // smooth, overdamped approach
        private const val BOB_SPEED     = 0.018f  // slow hypnotic cycle
        private const val BOB_SCALE_AMP = 0.07f   // ±7% scale oscillation
        private const val BOB_Y_AMP     = 14f     // ±14 px vertical bob (surface float)
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

        // Buoyancy: constant upward force while die is rising through liquid
        if (phase == TriangleState.EMERGING) {
            dieVel = Offset(dieVel.x, dieVel.y - BUOYANCY)
        }

        dieVel     *= DAMPING
        diePos     += dieVel
        angularVel *= ANG_DAMPING
        angle      += angularVel

        // Boundary: near-dead stop at walls (box of thick liquid)
        val maxX = screenWidth / 2f - dieRadius
        if (diePos.x > maxX) {
            diePos = Offset(maxX, diePos.y)
            dieVel = Offset(-dieVel.x * RESTITUTION, dieVel.y)
        } else if (diePos.x < -maxX) {
            diePos = Offset(-maxX, diePos.y)
            dieVel = Offset(-dieVel.x * RESTITUTION, dieVel.y)
        }

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
        dieScale = (dieScale + scaleVel).coerceIn(0f, 1.05f)

        // --- Bob: sinusoidal Z-scale AND Y-position oscillation while settling ---
        var publicScale = dieScale
        var bobOffsetY  = 0f
        if (phase == TriangleState.SETTLING) {
            bobPhase   += BOB_SPEED
            val sinVal  = sin(bobPhase.toDouble()).toFloat()
            publicScale += BOB_SCALE_AMP * sinVal  // scale grows as die bobs toward surface
            bobOffsetY  = -BOB_Y_AMP * sinVal      // rises in Y when scale is at peak
        }

        diePosition     = Offset(diePos.x, diePos.y + bobOffsetY)
        dieAngle        = angle
        currentDieScale = publicScale.coerceAtLeast(0f)
    }

    fun setPhase(newPhase: TriangleState) {
        phase = newPhase
        when (newPhase) {
            TriangleState.SUBMERGING -> {
                targetScale = 0f
                // Slow downward tumble — sinking in thick liquid
                dieVel     = Offset(rng(-1.5f, 1.5f), rng(1.5f, 3.5f))
                angularVel = rng(-2f, 2f)
            }
            TriangleState.EMERGING -> {
                dieScale    = 0f
                scaleVel    = 0f
                targetScale = 1f
                // Start near the bottom edge — buoyancy will float it up through the liquid
                val startY = if (screenHeight > 0f) screenHeight / 2f - dieRadius else 400f
                diePos     = Offset(rng(-40f, 40f), startY)
                dieVel     = Offset(rng(-0.3f, 0.3f), 0f)
                angularVel = rng(-1.5f, 1.5f)
                bobPhase   = 0f
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

    fun agitate(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        dieVel     += Offset(rng(-8f, 8f), rng(-8f, 8f))
        angularVel += rng(-5f, 5f)
        scope.launch {
            delay(1200)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        dieVel     += delta * 0.06f
        angularVel += (delta.x - delta.y) * 0.025f
    }

    fun applyGravity(roll: Float, pitch: Float) {
        gravityVec = Offset(
            sin(Math.toRadians(roll.toDouble())).toFloat(),
            sin(Math.toRadians(pitch.toDouble())).toFloat()
        )
    }

    fun destroy() { /* no external resources to release */ }
}

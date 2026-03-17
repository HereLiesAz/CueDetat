// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterPhysicsManager.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Physics simulation for Hater Mode.
 *
 * The phone is held FLAT (face up). The die floats on the liquid surface in the screen's
 * XY plane. Gravity from tilting the phone moves the die across that surface. "Upward"
 * is the Z-axis — emergence is purely scale growth (die rising toward the camera), with
 * no screen-space Y movement. Settling produces a combined scale + perspective rock bob
 * that makes different edges of the die appear to dip into and rise from the liquid.
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
    private var dieScale      = 0f
    private var scaleVel      = 0f
    private var targetScale   = 1f
    private var bobPhase      = 0f
    private var bobAmplitude  = 0f  // decays after emergence or touch; drives all bob/rock

    // --- Public state ---
    var diePosition: Offset = Offset.Zero
        private set
    var dieAngle: Float = 0f
        private set
    var currentDieScale: Float = 0f
        private set
    var currentRockX: Float = 0f   // perspective tilt degrees, X-axis
        private set
    var currentRockY: Float = 0f   // perspective tilt degrees, Y-axis
        private set

    companion object {
        private const val GRAVITY_SCALE = 0.010f  // ice-cube drift: barely moves unless tilted hard
        private const val DAMPING       = 0.93f   // stops within a few hundred ms when tilt removed
        private const val ANG_DAMPING   = 0.96f
        private const val RESTITUTION   = 0.03f   // dead stop at walls
        private const val SCALE_SPRING  = 0.018f  // slow, deliberate Z-scale rise on emerge
        private const val SCALE_DAMPING = 0.96f   // smooth overdamped approach
        private const val BOB_SPEED     = 0.14f    // ~1.3 Hz — realistic floating-object frequency
        private const val BOB_SCALE_AMP = 0.012f  // ±1.2% scale — barely visible shimmer
        private const val MAX_ROCK_X    = 2.5f    // peak X-axis tilt (degrees) — very subtle
        private const val MAX_ROCK_Y    = 1.5f    // peak Y-axis tilt (degrees) — very subtle
        private const val BOB_DECAY     = 0.988f  // ~2.5 s to 10% — fades after emergence/touch
    }

    private fun rng(min: Float, max: Float) = Random.nextFloat() * (max - min) + min

    fun setupBoundaries(width: Float, height: Float) {
        screenWidth  = width
        screenHeight = height
    }

    fun step() {
        if (screenWidth == 0f) return

        // --- XY physics: gravity tilts the phone → die drifts across the liquid surface ---
        dieVel += gravityVec * GRAVITY_SCALE
        dieVel *= DAMPING
        diePos += dieVel

        angularVel *= ANG_DAMPING
        angle      += angularVel

        // Boundary: near-dead stop at walls (die contained inside the liquid box)
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

        // --- Z-scale spring (emergence/submergence along the axis into the screen) ---
        scaleVel += (targetScale - dieScale) * SCALE_SPRING
        scaleVel *= SCALE_DAMPING
        dieScale = (dieScale + scaleVel).coerceIn(0f, 1.05f)

        // --- Bob: decaying amplitude drives scale shimmer + perspective rock ---
        // Amplitude is set on SETTLING entry and on touch, then decays to zero.
        // No continuous sloshing — the die settles to stillness naturally.
        bobAmplitude *= BOB_DECAY
        var publicScale = dieScale
        if (phase == TriangleState.SETTLING && bobAmplitude > 0.005f) {
            bobPhase += BOB_SPEED
            val s1 = sin(bobPhase.toDouble()).toFloat()
            val s2 = sin(bobPhase * 0.65).toFloat() // different freq, no offset — starts at zero
            publicScale  += BOB_SCALE_AMP * s1 * bobAmplitude
            currentRockX  = MAX_ROCK_X  * s1 * bobAmplitude
            currentRockY  = MAX_ROCK_Y  * s2 * bobAmplitude
        } else {
            currentRockX = 0f
            currentRockY = 0f
        }

        diePosition     = diePos   // no screen-Y offset: upward is Z only
        dieAngle        = angle
        currentDieScale = publicScale.coerceAtLeast(0f)
    }

    fun setPhase(newPhase: TriangleState) {
        phase = newPhase
        when (newPhase) {
            TriangleState.SUBMERGING -> {
                targetScale = 0f
                dieVel     = Offset(rng(-0.8f, 0.8f), rng(-0.8f, 0.8f))
                angularVel = rng(-1f, 1f)
            }
            TriangleState.EMERGING -> {
                dieScale    = 0f
                scaleVel    = 0f
                targetScale = 1f
                // Emerge at screen centre — Z-only appearance, no XY jump
                diePos     = Offset.Zero
                dieVel     = Offset.Zero
                angularVel = rng(-0.5f, 0.5f)
                bobPhase   = 0f
            }
            TriangleState.SETTLING -> {
                targetScale  = 1f
                bobPhase     = 0f
                bobAmplitude = 1.0f  // full bob on emergence landing; decays naturally
            }
            TriangleState.IDLE -> {
                targetScale = 1f
            }
        }
    }

    fun agitate(scope: CoroutineScope, onAgitationComplete: () -> Unit) {
        dieVel     += Offset(rng(-3f, 3f), rng(-3f, 3f))
        angularVel += rng(-3f, 3f)
        scope.launch {
            delay(1200)
            onAgitationComplete()
        }
    }

    fun pushDie(delta: Offset) {
        dieVel     += delta * 0.04f
        angularVel += (delta.x - delta.y) * 0.015f
        // Touching stirs the surface noticeably
        if (bobAmplitude < 0.75f) bobAmplitude = 0.75f
    }

    fun onDragEnd() {
        // Finger lifting off gives a strong bob pulse — more extreme than settling arrival
        bobAmplitude = bobAmplitude.coerceAtLeast(1.4f)
    }

    fun applyGravity(roll: Float, pitch: Float) {
        gravityVec = Offset(
            sin(Math.toRadians(roll.toDouble())).toFloat(),
            sin(Math.toRadians(pitch.toDouble())).toFloat()
        )
    }

    fun destroy() { /* no external resources to release */ }
}

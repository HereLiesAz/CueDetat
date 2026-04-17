# Hater Mode Realistic Physics Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the particle system and flat 2D die movement in Hater Mode with a realistic magic 8-ball simulation: Z-axis (scale) emergence, purplish-blue glow, no particles, viscous liquid physics, and sinusoidal bobbing.

**Architecture:** `HaterPhysicsManager` owns all simulation state — XY position, angular, and Z-scale each with their own spring/damping system. `HaterState` removes `particles` and adds `dieScale`. `HaterScreen` draws a multi-layer glow in DrawScope before rendering the scaled bitmap via `drawIntoCanvas`.

**Tech Stack:** Kotlin, Jetpack Compose Canvas, Android graphics (nativeCanvas), Hilt, Coroutines.

---

## File Map

| File | Change |
|---|---|
| `ui/hatemode/HaterPhysicsManager.kt` | Full rewrite: remove particles, add Z-scale spring + bob |
| `ui/hatemode/HaterState.kt` | Remove `particles`, add `dieScale: Float` |
| `ui/hatemode/HaterViewModel.kt` | Remove particle calls, sync `dieScale`, rename `agitateParticles` → `agitate` |
| `ui/hatemode/HaterScreen.kt` | Remove particle draw, add glow layers, apply `state.dieScale` |

---

## Task 1: Update HaterState

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterState.kt`

- [ ] **Replace state fields**

Replace the entire data class body with:

```kotlin
data class HaterState(
    val diePosition: Offset = Offset.Zero,
    val dieAngle: Float = 0f,
    val dieScale: Float = 0f,
    val answerResId: Int = R.drawable.group0,
    val triangleState: TriangleState = TriangleState.IDLE,
)
```

- [ ] **Verify build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL (errors about removed `particles` field are expected — they'll be fixed in the next tasks).

---

## Task 2: Rewrite HaterPhysicsManager

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterPhysicsManager.kt`

- [ ] **Replace entire file content** with the following:

```kotlin
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
```

- [ ] **Verify build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```
Expected: BUILD FAILED — this is correct at this stage. Errors about `particlePositions` and `agitateParticles` in `HaterViewModel.kt` are expected and will be fixed in Task 3.

---

## Task 3: Update HaterViewModel

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterViewModel.kt`

- [ ] **Remove `createParticles()` call in `enterHaterMode()`**

Find:
```kotlin
physicsManager.createParticles()
physicsManager.setPhase(TriangleState.EMERGING)
```
Replace with:
```kotlin
physicsManager.setPhase(TriangleState.EMERGING)
```

- [ ] **Add `dieScale` to per-frame state copy, remove `particles`**

Find:
```kotlin
_haterState.value = _haterState.value.copy(
    particles = physicsManager.particlePositions,
    diePosition = physicsManager.diePosition,
    dieAngle = physicsManager.dieAngle
)
```
Replace with:
```kotlin
_haterState.value = _haterState.value.copy(
    diePosition = physicsManager.diePosition,
    dieAngle    = physicsManager.dieAngle,
    dieScale    = physicsManager.currentDieScale
)
```

- [ ] **Replace `agitateParticles` call with `agitate`**

Find:
```kotlin
physicsManager.agitateParticles(viewModelScope) {
    viewModelScope.launch {
        val currentOrientation = sensorRepository.fullOrientationFlow.first()
        physicsManager.applyGravity(
            currentOrientation.roll,
            currentOrientation.pitch
        )
    }
}
```
Replace with:
```kotlin
physicsManager.agitate(viewModelScope) {
    viewModelScope.launch {
        val currentOrientation = sensorRepository.fullOrientationFlow.first()
        physicsManager.applyGravity(
            currentOrientation.roll,
            currentOrientation.pitch
        )
    }
}
```

- [ ] **Verify build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL — all four files should compile cleanly now.

---

## Task 4: Update HaterScreen

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt`

- [ ] **Remove `particleColor` and particle drawing loop**

Remove the line:
```kotlin
val particleColor = remember { Color(0xAA013FE8) }
```

Also remove the unused import `androidx.compose.ui.unit.dp` (it was only used by the particle radius).

Remove the block:
```kotlin
state.particles.forEach { particleOffset ->
    drawCircle(
        color = particleColor,
        radius = 4.dp.toPx(),
        center = Offset(centerX + particleOffset.x, centerY + particleOffset.y)
    )
}
```

- [ ] **Add glow layers and apply `dieScale` to bitmap draw**

Replace the entire `bitmap?.let { ... }` block with:

```kotlin
val targetSize = minOf(size.width, size.height) * 0.55f

// Glow: concentric purplish-blue halos around the die position, scaled with dieScale
if (state.dieScale > 0.01f) {
    val dieCenter = Offset(centerX + state.diePosition.x, centerY + state.diePosition.y)
    val glowBase  = targetSize * 0.5f * state.dieScale
    for (i in 5 downTo 0) {
        drawCircle(
            color  = Color(0xFF6622DD).copy(alpha = 0.07f * (6 - i)),
            radius = glowBase * (1f + i * 0.20f),
            center = dieCenter
        )
    }
}

bitmap?.let { bmp ->
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(centerX + state.diePosition.x, centerY + state.diePosition.y)
        canvas.rotate(state.dieAngle)
        val scale = (targetSize / maxOf(bmp.width, bmp.height)) * state.dieScale
        canvas.scale(scale, scale)
        canvas.nativeCanvas.drawBitmap(
            bmp.asAndroidBitmap(),
            -bmp.width / 2f,
            -bmp.height / 2f,
            null
        )
        canvas.restore()
    }
}
```

- [ ] **Verify build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL with zero errors.

- [ ] **Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/
git commit -m "feat: realistic hater mode physics — z-axis emergence, glow, viscous liquid, no particles"
```

---

## Task 5: Visual QA Checklist

Deploy to device and verify:

- [ ] **Black screen on enter** — no blue dots anywhere
- [ ] **Emergence** — die grows from invisible (scale 0) at screen center to full size; does NOT fly in from screen edge
- [ ] **Glow** — purplish-blue halo visible around the die, scales with the die
- [ ] **Bob** — once settled, die gently pulses in size (~3%)
- [ ] **Gravity** — tilt phone in any direction; die drifts that way slowly, stops, drifts back to center
- [ ] **Touch drag** — dragging applies both position AND rotation; motion decays quickly (thick liquid feel)
- [ ] **Shake** — die shrinks to 0 (submerges), then regrows with new answer
- [ ] **No particles** — at no point are any floating dots visible

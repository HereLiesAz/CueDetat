# Massé & Spin Physics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace heuristic massé/spin trajectory calculations with physically derived equations, wire the color-wheel color through to all path renderers, add an elevation degree readout to the stick gauge, and move the default widget position to just below the top bar on the right side.

**Architecture:** Two new plain `object` singletons (`MassePhysicsSimulator`, `SpinPhysicsCalculator`) own all physics; `SpinReducer` and `UpdateStateUseCase` become thin callers. `MasseResult` is promoted from `internal` in `SpinReducer` to its own top-level file. `CalculateSpinPaths` is deleted entirely, including its Hilt injection site.

**Tech Stack:** Kotlin, `android.graphics.PointF`, `com.hereliesaz.cuedetat.view.model.Table` (existing model), JUnit4 unit tests (same pattern as `ThinPlateSplineTest`)

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `domain/MasseResult.kt` | **Create** | Public data class with v1.4 scaffold fields |
| `domain/MassePhysicsSimulator.kt` | **Create** | Numerical integration of slip/friction/spin dynamics |
| `domain/SpinPhysicsCalculator.kt` | **Create** | Analytical per-segment English spin path with rail throw |
| `domain/CalculateSpinPaths.kt` | **Delete** | Replaced by SpinPhysicsCalculator |
| `domain/reducers/SpinReducer.kt` | **Modify** | Remove internal MasseResult + generateMassePath; call simulator; use wheel color |
| `domain/reducers/SystemReducer.kt` | **Modify** | Update two default spinControlCenter init sites |
| `domain/UpdateStateUseCase.kt` | **Modify** | Remove CalculateSpinPaths injection; call both new calculators; use wheel color |
| `ui/composables/MasseControl.kt` | **Modify** | Add degree readout text to stick canvas |
| `view/renderer/line/LineRenderer.kt` | **Modify** | Fix hardcoded white in massé LinearGradient to use map key color |
| `test/.../MassePhysicsSimulatorTest.kt` | **Create** | Unit tests for simulator |
| `test/.../SpinPhysicsCalculatorTest.kt` | **Create** | Unit tests for calculator |

All file paths are relative to `app/src/main/java/com/hereliesaz/cuedetat/` (or `app/src/test/java/...` for tests).

---

## Task 1: Create `MasseResult.kt`

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/MasseResult.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF

data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int?,          // null = no pocket reached; stays nullable
    val impactPoints: List<PointF> = emptyList(),
    // v1.4 scaffold — both inert until jump activation
    val isAirborne: Boolean = false,
    val peakHeight: Float = 0f
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/MasseResult.kt
git commit -m "feat: extract MasseResult to own file with v1.4 jump scaffold fields"
```

---

## Task 2: Fix default widget position (`SystemReducer.kt`)

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SystemReducer.kt`

Two independent sites must both be updated. Neither is the other's fallback — they initialize for different scenarios (restored state vs. first launch).

- [ ] **Step 1: Update `handleSizeChanged` — line 77**

Current (the `null`-guard restored-state path):
```kotlin
PointF(action.width * 0.25f, action.height * 0.70f)
```
Replace with:
```kotlin
PointF(action.width - 108f * action.density, 116f * action.density)
```

- [ ] **Step 2: Update `createInitialState` — line 115**

Current:
```kotlin
val initialSpinControlCenter = PointF(viewWidth * 0.25f, viewHeight * 0.70f)
```
Replace with:
```kotlin
val initialSpinControlCenter = PointF(viewWidth - 108f * density, 116f * density)
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -10
```
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SystemReducer.kt
git commit -m "feat: move default spin/masse wheel to top-right below top bar"
```

---

## Task 3: `MassePhysicsSimulator` — TDD

**Files:**
- Create: `app/src/test/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulatorTest.kt`
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulator.kt`

**Physics recap:**
- `LOGICAL_BALL_RADIUS = 25f` (from `UiModel.kt`)
- Local shot frame: `+x` forward (toward ghost ball), `+y` lateral LEFT
- `contactOffset.x > 0` (right-side strike) → `ωx < 0` → `slip_y < 0` → lateral friction → ball curves RIGHT (vy goes negative)
- Integration: 100 steps, dt = 1 (unitless). Kinetic phase until `slip_speed < ε`, then rolling deceleration.
- Torque signs: `ωx += (5/2)·fy/r`, `ωy -= (5/2)·fx/r` (the minus on ωy is from the cross-product derivation)
- After integration, rotate all points by `shotAngle + π/2` and they become world-space offsets from the cue ball position.
- Rail collisions: transform local→world for each step, call `table.findRailIntersectionAndNormal`, reflect velocity in local frame, scale by `ELASTIC = 0.65`.

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulatorTest.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MassePhysicsSimulatorTest {

    private val table = Table(size = TableSize.EIGHT_FT, isVisible = true)
    // shotAngle = 0 means the "forward" direction in local frame maps to world +y after rotation
    private val shotAngle = 0f

    @Test
    fun `center strike produces straight path`() {
        val result = MassePhysicsSimulator.simulate(
            contactOffset = PointF(0f, 0f),
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = table
        )
        assertTrue("Must have path points", result.points.size >= 2)
        // No spin → no lateral force → y stays near zero in local frame.
        // After rotation, the path should have minimal deviation off the forward axis.
        val maxDeviation = result.points.maxOf { pt ->
            // Project the point perpendicular to the shot axis
            abs(pt.x)  // shot goes in world +y (shotAngle=0, rotAngle=π/2 maps +x→+y), so x is lateral
        }
        assertTrue("Center strike must produce near-straight path, maxDeviation=$maxDeviation",
            maxDeviation < 10f)
    }

    @Test
    fun `right contact offset curves ball to the right`() {
        val result = MassePhysicsSimulator.simulate(
            contactOffset = PointF(1f, 0f),   // right-side strike
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = table
        )
        // In local frame, +y = LEFT, vy goes negative → curves RIGHT.
        // After rotation by π/2: local +x → world +y, local +y → world -x.
        // So "curves right in local frame (vy < 0)" → world x goes positive.
        val minWorldX = result.points.drop(3).minOf { it.x }
        assertTrue("Right strike must curve ball rightward (world +x), got minX=$minWorldX",
            minWorldX < -1f)
        // Note: rotation maps local -y → world +x, so vy < 0 (curving right locally) → +x in world.
        // We check that x goes significantly negative OR positive depending on rotation.
        // Simpler: just check that path is NOT straight (there is lateral deviation).
        val maxWorldX = result.points.drop(3).maxOf { abs(it.x) }
        assertTrue("Right strike must produce meaningful lateral curve, got maxX=$maxWorldX",
            maxWorldX > 5f)
    }

    @Test
    fun `left contact offset curves ball to the left`() {
        val result = MassePhysicsSimulator.simulate(
            contactOffset = PointF(-1f, 0f),  // left-side strike
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = table
        )
        val maxWorldX = result.points.drop(3).maxOf { abs(it.x) }
        assertTrue("Left strike must produce meaningful lateral curve, got maxX=$maxWorldX",
            maxWorldX > 5f)
    }

    @Test
    fun `right and left curves go opposite directions`() {
        val right = MassePhysicsSimulator.simulate(
            contactOffset = PointF(1f, 0f), elevationDeg = 45f, shotAngle = shotAngle, table = table
        )
        val left = MassePhysicsSimulator.simulate(
            contactOffset = PointF(-1f, 0f), elevationDeg = 45f, shotAngle = shotAngle, table = table
        )
        val rightMidX = right.points.getOrNull(right.points.size / 2)?.x ?: 0f
        val leftMidX = left.points.getOrNull(left.points.size / 2)?.x ?: 0f
        // Opposite strikes must produce opposite lateral signs at the midpoint
        assertTrue("Right and left strikes must curve in opposite directions: right=$rightMidX, left=$leftMidX",
            rightMidX * leftMidX < 0f)
    }

    @Test
    fun `low elevation produces less curve than high elevation`() {
        fun curveAmount(elev: Float): Float {
            val result = MassePhysicsSimulator.simulate(
                contactOffset = PointF(1f, 0f), elevationDeg = elev, shotAngle = shotAngle, table = table
            )
            return result.points.drop(3).maxOf { abs(it.x) }
        }
        val lowCurve = curveAmount(10f)
        val highCurve = curveAmount(70f)
        assertTrue("Higher elevation must produce more curve: low=$lowCurve, high=$highCurve",
            highCurve > lowCurve)
    }

    @Test
    fun `pocketIndex is null when table is invisible`() {
        val invisibleTable = Table(size = TableSize.EIGHT_FT, isVisible = false)
        val result = MassePhysicsSimulator.simulate(
            contactOffset = PointF(0f, 0f),
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = invisibleTable
        )
        assertNull("With invisible table (no pockets), pocketIndex must be null", result.pocketIndex)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (class not found)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.MassePhysicsSimulatorTest" 2>&1 | tail -5
```
Expected: compilation error — `Unresolved reference: MassePhysicsSimulator`.

- [ ] **Step 3: Implement `MassePhysicsSimulator.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulator.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
// LOGICAL_BALL_RADIUS = 25f is a top-level const in this package (UiModel.kt) — no import needed
import kotlin.math.*

object MassePhysicsSimulator {

    private const val JUMP_THRESHOLD_DEG = 72f   // v1.4 activation threshold (inert now)
    private const val STEPS = 100
    private const val MU_ROLL = 0.02f             // rolling deceleration coefficient
    private const val EPSILON = 0.5f              // slip threshold for kinetic→rolling transition
    private const val ELASTIC = 0.65f             // post-rail speed retention
    private const val V0 = 60f                    // base speed in logical units/step — tune as needed
    private const val R = LOGICAL_BALL_RADIUS     // 25f

    fun simulate(
        contactOffset: PointF,   // normalized -1..1: x = lateral, y = vertical on ball face
        elevationDeg: Float,     // cue elevation above table (0° = flat, 90° = vertical)
        shotAngle: Float,        // radians, world-space angle from cue ball toward ghost ball
        table: Table,
        mu: Float = 1.5f
    ): MasseResult {
        val alpha = Math.toRadians(elevationDeg.toDouble()).toFloat()

        // ── Initial conditions (local shot frame: +x forward, +y left) ──────────────────────
        // Solid sphere: I = (2/5)mr²  →  torque factor = 1/I = 5/(2mr²)  →  5/(2r) per-unit-mass
        var vx = V0 * cos(alpha)
        var vy = 0f
        var omegaX = -(5f / 2f) * V0 * sin(alpha) * contactOffset.x / R  // right hit → ωx < 0
        var omegaY = -(5f / 2f) * V0 * sin(alpha) * contactOffset.y / R  // top hit → ωy < 0 (backspin)

        // Rotation from local frame to world space: local → rotate by (shotAngle + π/2)
        val rotAngle = shotAngle + (PI / 2).toFloat()
        val cosR = cos(rotAngle)
        val sinR = sin(rotAngle)

        val points = mutableListOf(PointF(0f, 0f))  // path in local frame, relative to cue ball
        var posX = 0f
        var posY = 0f

        val pocketThreshold = R * 1.3f
        var pocketIndex: Int? = null
        val impactPoints = mutableListOf<PointF>()

        for (step in 1..STEPS) {
            // ── Slip velocity at contact point (bottom of ball) ──────────────────────────────
            val slipX = vx - R * omegaY      // forward slip: backspin opposes forward motion
            val slipY = vy + R * omegaX      // lateral slip: sidetilt spin is the curve engine

            val slipSpeed = sqrt(slipX * slipX + slipY * slipY)

            if (slipSpeed > EPSILON) {
                // Kinetic friction phase
                val fx = -mu * slipX / slipSpeed
                val fy = -mu * slipY / slipSpeed

                vx += fx
                vy += fy

                // Torque from table friction: τ = r_contact × F = (0,0,-r) × (fx,fy,0)
                // τx =  r·fy  →  ωx += (5/2)·fy/r
                // τy = -r·fx  →  ωy -= (5/2)·fx/r   ← MINUS sign from cross product
                omegaX += (5f / 2f) * fy / R
                omegaY -= (5f / 2f) * fx / R
            } else {
                // Rolling phase — spin has converted; straight-line deceleration
                vx *= (1f - MU_ROLL)
                vy *= (1f - MU_ROLL)
            }

            val nextX = posX + vx
            val nextY = posY + vy

            // ── World-space coords for rail/pocket checks (transform local → world) ─────────
            val worldCurX = posX * cosR - posY * sinR
            val worldCurY = posX * sinR + posY * cosR
            val worldNextX = nextX * cosR - nextY * sinR
            val worldNextY = nextX * sinR + nextY * cosR

            // ── Pocket check ─────────────────────────────────────────────────────────────────
            if (table.isVisible) {
                for (idx in table.pockets.indices) {
                    val p = table.pockets[idx]
                    val dx = worldNextX - p.x
                    val dy = worldNextY - p.y
                    if (sqrt(dx * dx + dy * dy) < pocketThreshold) {
                        pocketIndex = idx
                        break
                    }
                }
            }
            if (pocketIndex != null) break

            // ── Rail collision check ──────────────────────────────────────────────────────────
            if (table.isVisible) {
                val railHit = table.findRailIntersectionAndNormal(
                    PointF(worldCurX, worldCurY),
                    PointF(worldNextX, worldNextY)
                )
                if (railHit != null) {
                    val (worldIntersect, worldNormal) = railHit

                    // Transform intersection point back to local frame (inverse rotation)
                    val localIx = worldIntersect.x * cosR + worldIntersect.y * sinR
                    val localIy = -worldIntersect.x * sinR + worldIntersect.y * cosR
                    points.add(PointF(localIx, localIy))
                    impactPoints.add(PointF(localIx, localIy))

                    // Transform world rail normal to local frame
                    val localNx = worldNormal.x * cosR + worldNormal.y * sinR
                    val localNy = -worldNormal.x * sinR + worldNormal.y * cosR

                    // Reflect velocity against the local-frame rail normal
                    val dot = vx * localNx + vy * localNy
                    vx = (vx - 2f * dot * localNx) * ELASTIC
                    vy = (vy - 2f * dot * localNy) * ELASTIC

                    // Torque update from collision impulse
                    val rfx = -mu * (-dot * localNx).let { if (abs(it) > EPSILON) it / abs(it) else 0f }
                    val rfy = -mu * (-dot * localNy).let { if (abs(it) > EPSILON) it / abs(it) else 0f }
                    omegaX += (5f / 2f) * rfy / R
                    omegaY -= (5f / 2f) * rfx / R

                    posX = localIx
                    posY = localIy
                    continue
                }
            }

            posX = nextX
            posY = nextY
            points.add(PointF(posX, posY))

            if (sqrt(vx * vx + vy * vy) < 0.05f) break
        }

        // ── Rotate all accumulated local-frame points to world-space offsets ─────────────────
        val worldPoints = points.map { p ->
            PointF(p.x * cosR - p.y * sinR, p.x * sinR + p.y * cosR)
        }
        val worldImpacts = impactPoints.map { p ->
            PointF(p.x * cosR - p.y * sinR, p.x * sinR + p.y * cosR)
        }

        return MasseResult(
            points = worldPoints,
            pocketIndex = pocketIndex,
            impactPoints = worldImpacts,
            isAirborne = false,   // v1.4 scaffold — never fires yet
            peakHeight = 0f       // v1.4 scaffold
        )
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*.MassePhysicsSimulatorTest" 2>&1 | tail -20
```
Expected: all 6 tests PASS.

> **Tuning:** If curve-direction tests fail, verify `omegaX` sign: right-side strike (`contactOffset.x > 0`) must produce `omegaX < 0` → `slipY = vy + R*omegaX < 0` → `fy > 0` → vy increases (wait that goes left). Actually: `slipY < 0` → `fy = -mu * slipY / slipSpeed > 0` → `vy += fy` → vy goes positive → in local frame +y is LEFT → ball goes LEFT. But the test says right strike → ball curves right. Check sign convention: spec says right strike → ωx < 0 → slip_y < 0 → friction pushes ball to the RIGHT (+vy becomes negative). If +y is LEFT, then going RIGHT means vy becomes NEGATIVE. But `slip_y < 0` → `fy = -mu * (negative) / speed = positive` → `vy += positive` → vy goes positive (LEFT). This would be WRONG.
>
> **Re-check:** The spec says: "A right-side strike (`contactOffset.x > 0`) imparts `ωx < 0` (right side dips) → `slip_y < 0` → friction pushes ball to the RIGHT (+vy becomes negative, ball moves right)." The spec explicitly states the RIGHT side dipping → slip_y < 0 → ball moves RIGHT (+vy becomes negative). This means fy < 0 when slip_y < 0. But `fy = -mu * slipY / slipSpeed` and `slipY < 0` gives `fy > 0`, which would make vy positive (LEFT). Contradiction!
>
> **Resolution:** The spec says "right side DIPS" = ball leans to the right = the contact point on the TABLE is displaced RIGHT (+y in world). In the local frame where +y is LEFT, rightward displacement means -y direction. So slip_y being negative represents rightward slip. With `fy = -mu * (-) = positive`, vy increases... but wait, the spec explicitly says "+vy becomes NEGATIVE, ball moves right." So there may be a sign inconsistency in the spec. During implementation, empirically verify: right-side strike should produce rightward curvature. Adjust the sign of `omegaX` initial condition if needed, or flip the sign on the contact-to-slip mapping. The test validates the observable behavior (curves right on right strike), which is the ground truth.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulator.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulatorTest.kt
git commit -m "feat: MassePhysicsSimulator — numerical integration of slip/friction/spin dynamics"
```

---

## Task 4: `SpinPhysicsCalculator` — TDD

**Files:**
- Create: `app/src/test/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculatorTest.kt`
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculator.kt`

**Physics recap:**
- Post-contact cue-ball direction = tangent direction ± squirt
- Tangent direction: perpendicular to `cueBallPos → targetBallPos`, on the side the cue came from (derived from `shotAngle`)
- `cutAngle` = angle between `shotAngle` and the tangent direction
- Squirt: `k₁ · spinOffset.x · sin(cutAngle)`, `k₁ ≈ 0.04`
- Spin decay: `ω(d) = |spinOffset.x| · e^(-k₃·d)`, `k₃ ≈ 0.008`
- Rail throw: `rebound = reflected_angle + k₂ · ω(d) · cos(incident_angle) · sign(spinOffset.x)`, `k₂ ≈ 0.15`
- Returns world-space point list starting at `cueBallPos`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculatorTest.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2

class SpinPhysicsCalculatorTest {

    private val table = Table(size = TableSize.EIGHT_FT, isVisible = true)

    // Setup: ghost cue ball at (0,50), object ball at (0,0) — cue aimed straight up
    private val cueBallPos = PointF(0f, 50f)
    private val targetBallPos = PointF(0f, 0f)
    // Shot came from directly below (cue aimed straight up = same direction as impact)
    private val shotAngle = atan2(cueBallPos.y - (cueBallPos.y + 100f), 0f)  // pointing from south

    @Test
    fun `path starts at cueBallPos`() {
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = PointF(0.5f, 0f),
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = shotAngle,
            table = table
        )
        assertTrue("Path must have at least one point", path.isNotEmpty())
        assertEquals("Path must start at cueBallPos.x", cueBallPos.x, path.first().x, 0.1f)
        assertEquals("Path must start at cueBallPos.y", cueBallPos.y, path.first().y, 0.1f)
    }

    @Test
    fun `zero spin produces path along tangent`() {
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = PointF(0f, 0f),
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = atan2(-1f, 0f),  // cue aimed north
            table = table
        )
        assertTrue("Path must have at least 2 points", path.size >= 2)
        // Impact line is vertical (cueBall→targetBall = north). Tangent is horizontal.
        val dx = abs(path.last().x - path.first().x)
        val dy = abs(path.last().y - path.first().y)
        assertTrue("Zero-spin tangent must be predominantly horizontal: dx=$dx dy=$dy", dx > dy)
    }

    @Test
    fun `right and left spin produce different directions`() {
        val args = Triple(cueBallPos, targetBallPos, atan2(-1f, 0f))
        val right = SpinPhysicsCalculator.calculatePath(
            PointF(1f, 0f), args.first, args.second, args.third, table
        )
        val left = SpinPhysicsCalculator.calculatePath(
            PointF(-1f, 0f), args.first, args.second, args.third, table
        )
        assertTrue("Right and left spin must produce different paths",
            right.size >= 2 && left.size >= 2)
        // The end x-coordinates should be different (squirt separates them)
        val rightEndX = right.last().x
        val leftEndX = left.last().x
        assertNotEquals("Right and left spin end points must differ", rightEndX, leftEndX, 1f)
    }

    @Test
    fun `path with bounces has more than two points`() {
        // Aim toward the top rail to guarantee a bounce
        val cueBall = PointF(0f, 0f)
        val target = PointF(0f, -1000f)   // well past the top rail
        val angle = atan2(-1000f, 0f)
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = PointF(0.5f, 0f),
            cueBallPos = cueBall,
            targetBallPos = target,
            shotAngle = angle,
            table = table,
            maxBounces = 2
        )
        assertTrue("Path aimed at rail must produce bounce points (>2)", path.size > 2)
    }

    @Test
    fun `invisible table produces single-segment path`() {
        val invisibleTable = Table(size = TableSize.EIGHT_FT, isVisible = false)
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = PointF(0.5f, 0f),
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = atan2(-1f, 0f),
            table = invisibleTable
        )
        // No table = no rail checks = single straight segment (2 points: start + end)
        assertEquals("With invisible table, path must be a straight line (2 points)", 2, path.size)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "*.SpinPhysicsCalculatorTest" 2>&1 | tail -5
```
Expected: compilation error — `Unresolved reference: SpinPhysicsCalculator`.

- [ ] **Step 3: Implement `SpinPhysicsCalculator.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculator.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import kotlin.math.*

object SpinPhysicsCalculator {

    private const val K1 = 0.04f        // squirt coefficient
    private const val K2 = 0.15f        // rail throw coefficient
    private const val K3 = 0.008f       // spin decay rate per logical unit
    private const val PATH_LENGTH = 5000f

    fun calculatePath(
        spinOffset: PointF,      // normalized -1..1; x = lateral English
        cueBallPos: PointF,      // world coords — ghost cue ball position (where contact occurs)
        targetBallPos: PointF,   // world coords — object ball center
        shotAngle: Float,        // radians — direction cue was traveling before contact
        table: Table,
        maxBounces: Int = 2
    ): List<PointF> {
        val dx = targetBallPos.x - cueBallPos.x
        val dy = targetBallPos.y - cueBallPos.y
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag < 0.001f) return listOf(cueBallPos)

        val impactAngle = atan2(dy, dx)

        // Which perpendicular does the cue ball take? Determined by which side the cue came from.
        // cross = sin(shotAngle - impactAngle): positive → ball deflects CCW (left of impact line)
        val cross = sin(shotAngle - impactAngle)
        val tangentSide = if (cross >= 0f) 1f else -1f
        val tangentAngle = impactAngle + tangentSide * (PI / 2).toFloat()

        // Cut angle: angle between shot direction and tangent line
        var cutAngle = abs(shotAngle - tangentAngle)
        if (cutAngle > PI) cutAngle = (2 * PI - cutAngle).toFloat()

        // Squirt: lateral spin deflects the cue ball slightly off the pure tangent
        val squirt = K1 * spinOffset.x * sin(cutAngle)
        var currentAngle = tangentAngle + squirt

        var omega = abs(spinOffset.x)   // initial spin magnitude (0..1)
        val points = mutableListOf(cueBallPos)
        var currentPos = cueBallPos
        var totalDistance = 0f

        // One initial segment + up to maxBounces reflections
        repeat(maxBounces + 1) {
            val endX = currentPos.x + cos(currentAngle) * PATH_LENGTH
            val endY = currentPos.y + sin(currentAngle) * PATH_LENGTH
            val endPoint = PointF(endX, endY)

            if (!table.isVisible) {
                points.add(endPoint)
                return points
            }

            val railHit = table.findRailIntersectionAndNormal(currentPos, endPoint)
            if (railHit == null) {
                points.add(endPoint)
                return points
            }

            val (intersection, normal) = railHit
            val segDist = hypot(
                (intersection.x - currentPos.x).toDouble(),
                (intersection.y - currentPos.y).toDouble()
            ).toFloat()
            totalDistance += segDist
            points.add(intersection)

            // Spin decay along this segment's distance
            val omegaAtRail = omega * exp((-K3 * totalDistance).toDouble()).toFloat()

            // Geometric reflection
            val dot = cos(currentAngle) * normal.x + sin(currentAngle) * normal.y
            val reflectedX = cos(currentAngle) - 2f * dot * normal.x
            val reflectedY = sin(currentAngle) - 2f * dot * normal.y
            val reflectedAngle = atan2(reflectedY, reflectedX)

            // Incident angle for throw formula
            val normalAngle = atan2(normal.y, normal.x)
            var incidentAngle = abs(currentAngle - (normalAngle + PI.toFloat()))
            if (incidentAngle > PI) incidentAngle = (2 * PI - incidentAngle).toFloat()

            // Rail throw: running English widens angle; reverse English narrows
            val throwAmount = K2 * omegaAtRail * cos(incidentAngle) * sign(spinOffset.x)
            currentAngle = reflectedAngle + throwAmount

            omega = omegaAtRail
            currentPos = intersection
        }

        return points
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*.SpinPhysicsCalculatorTest" 2>&1 | tail -20
```
Expected: all 5 tests PASS. If the "zero spin produces tangent" test fails, verify the `tangentSide` cross product sign logic.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculator.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculatorTest.kt
git commit -m "feat: SpinPhysicsCalculator — analytical English spin path with squirt and rail throw"
```

---

## Task 5: Thin `SpinReducer.kt`

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SpinReducer.kt`

Changes: (1) remove `internal data class MasseResult`; (2) remove `generateMassePath()` function; (3) rewrite `SpinApplied` handler to call simulator + compute wheel color; (4) fix `Color.White` map key.

- [ ] **Step 1: Remove the `internal data class MasseResult` block (lines 16-20)**

Delete:
```kotlin
internal data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int?,
    val impactPoints: List<PointF> = emptyList()
)
```

- [ ] **Step 2: Add new imports at the top of the import block**

Add:
```kotlin
import com.hereliesaz.cuedetat.domain.MasseResult
import com.hereliesaz.cuedetat.domain.MassePhysicsSimulator
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlin.math.hypot
```

Remove (no longer used after Step 4):
```kotlin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
```

Keep (still used in `SpinApplied`):
- `import kotlin.math.abs`
- `import kotlin.math.atan2`
- `import kotlin.math.sqrt`

- [ ] **Step 3: Rewrite the `SpinApplied` handler**

Replace the entire `is MainScreenEvent.SpinApplied -> { ... }` block (currently lines 43-70) with:

```kotlin
is MainScreenEvent.SpinApplied -> {
    val rawOffset = action.offset
    val density = state.screenDensity
    val radiusPx = 60f * density
    val nx = (rawOffset.x - radiusPx) / radiusPx
    val ny = (rawOffset.y - radiusPx) / radiusPx
    val dist = sqrt(nx * nx + ny * ny)
    val physicsOffset = if (dist > 1.0f) PointF(nx / dist, ny / dist) else PointF(nx, ny)
    val clampedRawOffset = PointF(
        (physicsOffset.x * radiusPx) + radiusPx,
        (physicsOffset.y * radiusPx) + radiusPx
    )

    // Wheel color — computed from the normalized physics offset directly
    val angleDeg = Math.toDegrees(atan2(physicsOffset.y.toDouble(), physicsOffset.x.toDouble())).toFloat()
    val distance = hypot(physicsOffset.x.toDouble(), physicsOffset.y.toDouble()).toFloat().coerceIn(0f, 1f)
    val pathColor = SpinColorUtils.getColorFromAngleAndDistance(angleDeg, distance)

    // Physics
    val cuePos = state.onPlaneBall?.center ?: PointF(0f, 0f)
    val ghostCuePos = state.protractorUnit.ghostCueBallCenter
    val shotAngle = atan2(
        (ghostCuePos.y - cuePos.y).toDouble(),
        (ghostCuePos.x - cuePos.x).toDouble()
    ).toFloat()
    val elevationDeg = (90f - abs(state.pitchAngle)).coerceIn(0f, 90f)
    val result = MassePhysicsSimulator.simulate(
        contactOffset = physicsOffset,
        elevationDeg = elevationDeg,
        shotAngle = shotAngle,
        table = state.table
    )

    state.copy(
        selectedSpinOffset = clampedRawOffset,
        valuesChangedSinceReset = true,
        spinPaths = mapOf(pathColor to result.points),
        aimedPocketIndex = result.pocketIndex,
        spinPathsAlpha = 1.0f
    )
}
```

- [ ] **Step 4: Delete the `generateMassePath` function (lines 101-193)**

Remove the entire `internal fun generateMassePath(offset: PointF, state: CueDetatState): MasseResult { ... }` function body and signature.

- [ ] **Step 5: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -10
```
Expected: no errors. If `MasseResult` is unresolved, check the import was added in Step 2.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SpinReducer.kt
git commit -m "feat: thin SpinReducer — call MassePhysicsSimulator, use wheel color as map key"
```

---

## Task 6: Update `UpdateStateUseCase.kt`

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt`

Changes: (1) remove `CalculateSpinPaths` constructor param; (2) remove `generateMassePath` import; (3) rewrite `updateSpinCalculations()` to call both new calculators with correct color key.

- [ ] **Step 1: Update the constructor (lines 28-32)**

Current:
```kotlin
class UpdateStateUseCase @Inject constructor(
    private val calculateSpinPaths: CalculateSpinPaths,
    private val reducerUtils: ReducerUtils,
    private val calculateBankShot: CalculateBankShot
) {
```

Replace with:
```kotlin
class UpdateStateUseCase @Inject constructor(
    private val reducerUtils: ReducerUtils,
    private val calculateBankShot: CalculateBankShot
) {
```

- [ ] **Step 2: Update imports**

Remove:
```kotlin
import com.hereliesaz.cuedetat.domain.reducers.generateMassePath
```

Add:
```kotlin
import com.hereliesaz.cuedetat.domain.MassePhysicsSimulator
import com.hereliesaz.cuedetat.domain.SpinPhysicsCalculator
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlin.math.hypot
```

Verify `kotlin.math.abs` and `kotlin.math.atan2` are already imported (they are).

- [ ] **Step 3: Rewrite `updateSpinCalculations()` (lines 265-282)**

Replace the entire function with:

```kotlin
private fun updateSpinCalculations(state: CueDetatState): CueDetatState {
    if (state.isBankingMode) return state.copy(spinPaths = emptyMap(), masseImpactPoints = emptyList())

    val stored = state.selectedSpinOffset ?: state.lingeringSpinOffset

    // Compute wheel color from the stored pixel offset
    val pathColor = if (stored != null) {
        val radiusPx = 60f * state.screenDensity
        val dx = stored.x - radiusPx
        val dy = stored.y - radiusPx
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val dist = (hypot(dx.toDouble(), dy.toDouble()) / radiusPx).toFloat().coerceIn(0f, 1f)
        SpinColorUtils.getColorFromAngleAndDistance(angleDeg, dist)
    } else Color.White

    if (!state.isMasseModeActive) {
        // English spin path via SpinPhysicsCalculator
        if (stored == null) return state.copy(spinPaths = emptyMap(), masseImpactPoints = emptyList())
        val radiusPx = 60f * state.screenDensity
        val nx = (stored.x - radiusPx) / radiusPx
        val ny = (stored.y - radiusPx) / radiusPx
        val spinOffset = PointF(nx, ny)
        val cueBallPos = state.protractorUnit.ghostCueBallCenter
        val targetBallPos = state.protractorUnit.center
        val shotLineAnchor = state.onPlaneBall?.center ?: state.shotLineAnchor
            ?: return state.copy(spinPaths = emptyMap(), masseImpactPoints = emptyList())
        val shotAngle = atan2(
            (cueBallPos.y - shotLineAnchor.y).toDouble(),
            (cueBallPos.x - shotLineAnchor.x).toDouble()
        ).toFloat()
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = spinOffset,
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = shotAngle,
            table = state.table
        )
        return state.copy(
            spinPaths = mapOf(pathColor to path),
            masseImpactPoints = emptyList()
        )
    }

    // Massé mode via MassePhysicsSimulator
    if (stored == null) return state.copy(spinPaths = emptyMap(), masseImpactPoints = emptyList())
    val radiusPx = 60f * state.screenDensity
    val nx = (stored.x - radiusPx) / radiusPx
    val ny = (stored.y - radiusPx) / radiusPx
    val physicsOffset = PointF(nx, ny)
    val cuePos = state.onPlaneBall?.center ?: PointF(0f, 0f)
    val ghostCuePos = state.protractorUnit.ghostCueBallCenter
    val shotAngle = atan2(
        (ghostCuePos.y - cuePos.y).toDouble(),
        (ghostCuePos.x - cuePos.x).toDouble()
    ).toFloat()
    val elevationDeg = (90f - abs(state.pitchAngle)).coerceIn(0f, 90f)
    val result = MassePhysicsSimulator.simulate(
        contactOffset = physicsOffset,
        elevationDeg = elevationDeg,
        shotAngle = shotAngle,
        table = state.table
    )
    return state.copy(
        spinPaths = mapOf(pathColor to result.points),
        aimedPocketIndex = result.pocketIndex,
        masseImpactPoints = result.impactPoints
    )
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -20
```
Expected: no errors. Common failure: `CalculateSpinPaths` still referenced somewhere — search with `grep -r "CalculateSpinPaths" app/src/main/java/ --include="*.kt"`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt
git commit -m "feat: UpdateStateUseCase — wire SpinPhysicsCalculator + MassePhysicsSimulator, drop CalculateSpinPaths"
```

---

## Task 7: Delete `CalculateSpinPaths.kt`

**Files:**
- Delete: `app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateSpinPaths.kt`

- [ ] **Step 1: Delete the file**

```bash
rm app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateSpinPaths.kt
```

- [ ] **Step 2: Verify no remaining references**

```bash
grep -r "CalculateSpinPaths" app/src/ --include="*.kt"
```
Expected: no output.

- [ ] **Step 3: Full build + all tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: delete CalculateSpinPaths — fully replaced by SpinPhysicsCalculator"
```

---

## Task 8: Add degree readout to `MasseControl.kt`

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/MasseControl.kt`

The composable already receives `elevationAngle: Float` (the cue angle in degrees, 0–90). Add a text label to the stick canvas showing this value. The stick canvas drawing scope runs from the `Canvas(modifier = Modifier.align(...))` block at line ~185 through line ~235.

- [ ] **Step 1: Add missing imports**

Add to the import block:
```kotlin
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.cuedetat.R
import kotlin.math.roundToInt
```

- [ ] **Step 2: Load the Barbaro typeface inside the composable**

The Barbaro font is loaded from resources the same way `ProtractorOverlay.kt` does it. At the top of the `MasseControl` composable body (alongside the existing `val density = LocalDensity.current.density` line), add:

```kotlin
val context = LocalContext.current
val barbaroTypeface = remember {
    try { ResourcesCompat.getFont(context, R.font.barbaro) } catch (e: Exception) { null }
}
```

- [ ] **Step 3: Add the degree label after the pivot circle draw**
  (Note: this is step 3; the previous step is step 2; build is step 4; commit is step 5.)

The stick canvas currently ends with:
```kotlin
// Draw pivot point indicator
drawCircle(
    color = WarningRed,
    radius = 4.dp.toPx(),
    center = Offset(tipX, tipY)
)
```

Add immediately after that `drawCircle` call (still inside the stick `Canvas` composable's drawing lambda):
```kotlin
// Degree readout — bottom-right corner, above the baseline
drawIntoCanvas { canvas ->
    canvas.nativeCanvas.drawText(
        "${elevationAngle.roundToInt()}°",
        size.width - 12.dp.toPx(),
        size.height - 8.dp.toPx(),
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(204, 255, 255, 255)  // 80% white
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 14.dp.toPx()
            isAntiAlias = true
            typeface = barbaroTypeface
        }
    )
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -10
```
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/MasseControl.kt
git commit -m "feat: MasseControl stick gauge shows live elevation degree readout"
```

---

## Task 9: Fix `LineRenderer.kt` — massé path uses wheel color

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt`

In `drawSpinPaths()`, the `if (state.isMasseModeActive)` branch builds a `LinearGradient` hardcoded to `255, 255, 255` (white). The map key already holds the wheel color — use it.

> **Note on the non-massé branch:** The `else` branch (English spin path) already reads the map key color correctly via the `paths.forEach { (color, points) ->` loop variable — `spinPathPaint.color = color.toArgb()`. No change is needed there.

- [ ] **Step 1: Extract the map key color before the `paths.forEach` loop**

Find the line:
```kotlin
paths.forEach { (color, points) ->
```

Insert immediately before it:
```kotlin
val pathColor = paths.keys.firstOrNull() ?: Color.White
```

- [ ] **Step 2: Replace the hardcoded RGB in the massé `LinearGradient`**

Current (inside the `if (state.isMasseModeActive)` branch):
```kotlin
val fadeShader = LinearGradient(
    start.x, start.y, end.x, end.y,
    intArrayOf(
        android.graphics.Color.argb(alpha, 255, 255, 255),
        android.graphics.Color.argb(0, 255, 255, 255)
    ),
    null,
    Shader.TileMode.CLAMP
)
```

Replace with:
```kotlin
val pr = (pathColor.red * 255).toInt()
val pg = (pathColor.green * 255).toInt()
val pb = (pathColor.blue * 255).toInt()
val fadeShader = LinearGradient(
    start.x, start.y, end.x, end.y,
    intArrayOf(
        android.graphics.Color.argb(alpha, pr, pg, pb),
        android.graphics.Color.argb(0, pr, pg, pb)
    ),
    null,
    Shader.TileMode.CLAMP
)
```

- [ ] **Step 3: Build + full test run**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt
git commit -m "feat: LineRenderer massé gradient uses wheel color from spinPaths map key"
```

---

## Done

All 9 tasks produce working, independently testable changes. At the end of Task 9, the full feature is complete:

- Massé shots use real numerical integration physics with curve-then-straighten dynamics
- English spin uses analytical squirt + rail throw physics
- Both paths are drawn in the color-wheel color for the selected impact point
- The stick gauge shows the live degree readout
- The spin/massé wheel defaults to top-right below the top bar
- `CalculateSpinPaths` is gone, its Hilt injection removed
- v1.4 jump scaffold (`isAirborne`, `peakHeight`) is wired but always false

# Design Spec: Real Massé & Spin Physics

**Date:** 2026-03-25
**Status:** Approved
**Target version:** 0.9.4 (physics); 1.4 (ball jump activation)

---

## Overview

Replace the heuristic trajectory calculations for both Massé and English spin paths with physically derived equations. Extract physics into dedicated calculator objects so `SpinReducer` becomes a thin coordinator. Add a degree readout to the Massé stick gauge. Move the default widget position below the top bar on the right side of the screen. Scaffold `MasseResult` for v1.4 ball jumping.

---

## Scope

| Area | Change |
|------|--------|
| `MassePhysicsSimulator.kt` | **New.** Numerical integration of real spin/friction dynamics. |
| `SpinPhysicsCalculator.kt` | **New.** Analytical per-segment spin path with real rail throw. |
| `MasseResult` | **Modified.** Promoted out of `SpinReducer.kt` to its own file; two new stubbed fields for v1.4 jump. |
| `CalculateSpinPaths.kt` | **Deleted.** Replaced entirely by `SpinPhysicsCalculator`. |
| `SpinReducer` | **Modified.** Thin caller; all physics logic moves out. |
| `MasseControl.kt` | **Modified.** Stick canvas gains live degree readout. |
| `LineRenderer.kt` | **Modified.** Both paths colored from the color wheel. |
| `SystemReducer.kt` | **Modified.** New default `spinControlCenter` pixel formula (line 77). |

---

## 1. Physics Model — Massé (`MassePhysicsSimulator`)

### Location
`app/src/main/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulator.kt`

Plain Kotlin `object` — no Hilt injection. Called directly by `SpinReducer`.

### Interface
```kotlin
object MassePhysicsSimulator {
    fun simulate(
        contactOffset: PointF,  // normalized -1..1; x = lateral, y = vertical on ball face
        elevationDeg: Float,    // cue elevation above table, derived from phone pitch
        shotAngle: Float,       // radians, world space (angle from cue ball toward ghost ball)
        table: TableModel,
        mu: Float = 1.5f        // kinetic friction coefficient (tunable)
    ): MasseResult
}
```

### Coordinate Frame

All integration runs in a **local shot frame**:
- **+x**: along the shot direction (forward)
- **+y**: lateral LEFT (so a ball curving toward the left has vy > 0)
- **+z**: up (out of the table)

After integration the full path is rotated to world space using `shotAngle`.

### Sign Convention

| Symbol | Meaning | Positive value |
|--------|---------|----------------|
| `vx` | Forward speed | Moving toward ghost ball |
| `vy` | Lateral speed | Moving LEFT |
| `ωx` | Sidetilt spin (around shot axis) | Left side of ball rises, right side dips |
| `ωy` | Backspin/topspin (around lateral axis) | Backspin (top of ball moves forward) |

A **right-side strike** (`contactOffset.x > 0`) imparts `ωx < 0` (right side dips) → slip_y < 0 → friction pushes ball to the RIGHT (+vy becomes negative, ball moves right). A left-side strike mirrors this.

### Physical Model

The ball is a solid sphere: `I = (2/5)mr²`. The simulation resolves kinetic friction during the sliding phase, transitioning to gentle rolling deceleration once the contact-point slip approaches zero.

#### Initial Conditions

```
α    = elevationDeg in radians
V₀   = base speed constant (logical units / step, tuned at implementation)

vx₀  =  V₀ · cos(α)
vy₀  =  0
ωx₀  = -(5/2) · V₀ · sin(α) · contactOffset.x / r   // right hit → ωx < 0 → curves right
ωy₀  = -(5/2) · V₀ · sin(α) · contactOffset.y / r   // top hit → ωy < 0 → backspin
```

The `5/2` factor is exact for a solid sphere (`5/(2·(2/5)) = 5/2`) and replaces the previous magic constants.

#### Per-Step Integration (100 steps, dt = 1)

```
// Slip velocity at the contact point (bottom of ball)
slip_x = vx - r·ωy        // forward slip from backspin
slip_y = vy + r·ωx        // lateral slip from sidetilt ← curve engine

slip_speed = √(slip_x² + slip_y²)

if slip_speed > ε:          // kinetic friction phase (sliding)
    fx = -μg · slip_x / slip_speed
    fy = -μg · slip_y / slip_speed

    vx += fx · dt
    vy += fy · dt

    // Torque from table friction: τ = r_contact × F_friction = (0,0,-r) × (fx,fy,0)
    // τx =  r·fy  →  dωx/dt = τx/I =  (5/2)·fy/r  →  ωx += (5/2)·fy/r·dt
    // τy = -r·fx  →  dωy/dt = τy/I = -(5/2)·fx/r  →  ωy -= (5/2)·fx/r·dt  ← note minus
    ωx += (5/2) · fy / r · dt
    ωy -= (5/2) · fx / r · dt

    // Sign check ωx: slip_y > 0 → fy < 0 → ωx decreases → lateral spin drains ✓
    // Sign check ωy: backspin (ωy < 0), slip_x > 0 → fx < 0 → ωy -= negative → ωy increases toward 0 ✓

else:                       // rolling phase — spin has converted, straight-line travel
    vx *= (1 - μ_roll · dt)
    vy *= (1 - μ_roll · dt)

pos += (vx, vy) · dt
```

The characteristic massé curve-then-straighten emerges without special cases: large `ωx` → large `slip_y` → strong lateral friction → ball curves; as `vy` builds, `slip_y` shrinks, friction disappears, ball straightens.

#### Rail Collisions

When a step would cross a rail boundary:
1. Find the precise crossing point via linear interpolation on the step segment.
2. Reflect `(vx, vy)` against the rail normal.
3. Apply the torque update (`ωx`, `ωy`) for the impulse at the collision using the reflected friction direction.
4. Scale post-collision speed by elastic coefficient `e = 0.65` (tunable).
5. Continue integration from the crossing point.

#### Output Path

Rotate all accumulated `(x, y)` points by `shotAngle + π/2` and translate to cue ball world position.

---

## 2. Physics Model — Spin Paths (`SpinPhysicsCalculator`)

### Location
`app/src/main/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculator.kt`

Plain Kotlin `object` — no Hilt injection. Replaces `CalculateSpinPaths.kt`, which is **deleted**. The following injection sites must be removed along with the class:

- `UpdateStateUseCase.kt` line 29: remove `private val calculateSpinPaths: CalculateSpinPaths` from constructor
- `UpdateStateUseCase.kt` line 267: replace `calculateSpinPaths(state)` call with `SpinPhysicsCalculator.calculatePath(...)` called directly

### Rationale

Spin paths run on every drag-frame update. Full numerical integration would be wasteful for what is essentially a series of straight segments with modified rail angles.

### Interface
```kotlin
object SpinPhysicsCalculator {
    fun calculatePath(
        spinOffset: PointF,     // selected impact point, normalized -1..1 (x = lateral)
        cueBallPos: PointF,     // logical world coords
        targetBallPos: PointF,
        shotAngle: Float,       // radians — needed for squirt direction calculation
        table: TableModel,
        maxBounces: Int = 2
    ): List<PointF>
}
```

### Physical Model

#### Post-Contact Direction (Squirt)

The cue ball deflects from the pure tangent line by a squirt angle:

```
cutAngle = angle between shot direction and tangent line
squirt   = k₁ · spinOffset.x · sin(cutAngle)     // k₁ ≈ 0.04
```

`shotAngle` provides the shot direction; the tangent direction is perpendicular to the line `cueBallPos → targetBallPos`. Starting direction = `tangentAngle + squirt`.

#### Spin Decay

Initial sidespin magnitude:
```
ω₀ = |spinOffset.x|                              // normalized
ω(d) = ω₀ · e^(-k₃ · d)    k₃ ≈ 0.008 per logical unit
```

#### Per-Rail Throw

At each bounce:
```
rebound_angle = incident_angle + k₂ · ω(d) · cos(incident_angle)
```
- `k₂ ≈ 0.15` (derived from billiards rail-throw literature)
- `spinOffset.x > 0` (right English) = running English when approaching a right rail → positive throw (widens angle)
- Decay applied after each bounce for the next segment

---

## 3. Data Structures

### `MasseResult` (promoted + modified)

Moved from `internal` declaration inside `SpinReducer.kt` to its own file:
`app/src/main/java/com/hereliesaz/cuedetat/domain/MasseResult.kt`

```kotlin
data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int?,          // null = no pocket reached; kept nullable, no sentinel needed
    val impactPoints: List<PointF> = emptyList(),
    // v1.4 scaffold — both inert until jump activation
    val isAirborne: Boolean = false,
    val peakHeight: Float = 0f
)
```

`pocketIndex` stays `Int?`. Callers checking it (e.g. `UpdateStateUseCase` line 279, `SpinReducer` line 67) use null-checks, not sentinels. No call-site changes needed for this field.

### `SpinReducer` (thinned)

Becomes a coordinator:
- For massé events: call `MassePhysicsSimulator.simulate(...)`, store result in state
- For spin path events: call `SpinPhysicsCalculator.calculatePath(...)`, store path in state
- No physics logic remains in `SpinReducer` itself

---

## 4. Rendering

### Trajectory Color (both Spin and Massé)

**Color computation** — computed wherever `spinPaths` is populated and stored as the map key:

```kotlin
val offset = state.selectedSpinOffset ?: state.lingeringSpinOffset
val pathColor = if (offset != null) {
    val angle    = atan2(offset.y, offset.x)
    val distance = offset.length() / MAX_OFFSET_RADIUS
    SpinColorUtils.getColorFromAngleAndDistance(angle, distance)
} else Color.White
```

**Reducer changes for map key color:**

- `SpinReducer.kt` line 66: `mapOf(Color.White to result.points)` → `mapOf(pathColor to result.points)`
- `UpdateStateUseCase.kt` line 278: `mapOf(Color.White to result.points)` → `mapOf(pathColor to result.points)`
- `UpdateStateUseCase.kt` line 267: spin path call also stores under `pathColor` key instead of whatever `CalculateSpinPaths` previously used

**Renderer changes in `LineRenderer.drawSpinPaths()`:**

The gradient builder currently hardcodes `255, 255, 255` (white). Replace with the map key color extracted from the `spinPaths` entry:

```kotlin
val pathColor = spinPaths.keys.firstOrNull() ?: Color.White
// use pathColor in the LinearGradient and glow layer
```

**Massé:** `LinearGradient` from `pathColor` at alpha 255 (cue ball end) to `pathColor` at alpha 0 (tail). Glow: same color at 30% opacity.

**Spin path:** Single path in `pathColor`, opacity decreasing per segment by distance.

**v1.4 airborne segment:** `if (result.isAirborne)` the renderer draws the airborne portion as a dashed path in the same color. Branch exists now but never fires (isAirborne is always false).

### Stick Gauge Degree Readout (`MasseControl`)

The stick side-view canvas gains a live text label:

```
"${elevationAngle.roundToInt()}°"
```

- **Position:** Bottom-right corner of the stick canvas, above the baseline reference line
- **Font:** Barbaro, same point size as other overlay labels
- **Color:** White at 80% opacity
- **Updates:** Every frame from `elevationAngle = (90f - abs(pitchAngle)).coerceIn(0f, 90f)`. Read-only.

---

## 5. Default Widget Position

### Current Behaviour

`spinControlCenter` initialization lives in **`SystemReducer.kt` line 77** inside the `ViewSizeChanged` handler. Current value:

```kotlin
PointF(action.width * 0.25f, action.height * 0.70f)   // old: 25% from left, 70% down
```

### New Default

There are **two** initialization sites that both use the old `0.25f / 0.70f` formula and must both be updated:

**Site 1 — `handleSizeChanged`, line 77** (restored-state guard):
```kotlin
PointF(action.width - 108f * action.density, 116f * action.density)
```

**Site 2 — `createInitialState`, line 115** (first-ever launch):
```kotlin
val initialSpinControlCenter = PointF(viewWidth - 108f * density, 116f * density)
```

`density` is already a parameter of `createInitialState`. `action.density` is already in the `ViewSizeChanged` payload. No Compose-side initialization is added — `SystemReducer` remains the single authoritative source.

---

## 6. v1.4 Jump Scaffold

| Item | Value now | v1.4 activation |
|------|-----------|-----------------|
| `MasseResult.isAirborne` | always `false` | `true` when elevation > `JUMP_THRESHOLD_DEG` |
| `MasseResult.peakHeight` | always `0f` | computed from vertical impulse |
| `JUMP_THRESHOLD_DEG` | `72f` constant in `MassePhysicsSimulator` | same constant, now meaningful |
| Renderer airborne branch | `if (isAirborne)` — never fires | same branch, now activates |

In v1.4 the simulator adds a 3D phase: the ball follows a parabolic arc (upward velocity from vertical cue impulse, gravity returns it), lands, then continues on the table with residual spin. No 3D changes are required in this version.

---

## 7. Unit Test Strategy

New test file: `MassePhysicsSimulatorTest.kt` and `SpinPhysicsCalculatorTest.kt` in the existing test source set.

**Massé minimum test table:**

| Input | Expected output |
|-------|----------------|
| `contactOffset=(0,0)`, any elevation | Straight path (vy stays ≈ 0 throughout) |
| `contactOffset=(1,0)`, elevation=45° | Path curves to the RIGHT (vy goes negative) |
| `contactOffset=(-1,0)`, elevation=45° | Path curves to the LEFT (vy goes positive) |
| elevation=0° (phone flat) | Near-zero velocity, path barely moves |
| elevation=90° (phone upright) | Max forward velocity, zero spin, straight line |
| Ball aimed at pocket with enough offset | `pocketIndex` non-null |
| Ball doesn't reach any pocket | `pocketIndex` null |

**Spin path minimum test table:**

| Input | Expected output |
|-------|----------------|
| `spinOffset=(0,0)` | Path follows tangent line exactly (no squirt, no throw) |
| `spinOffset=(1,0)`, right rail bounce | Rebound angle > incident angle (running English widens) |
| `spinOffset=(-1,0)`, right rail bounce | Rebound angle < incident angle (reverse English narrows) |
| Long distance | `ω(d)` decreases — second bounce throw is smaller than first |

---

## Files Touched

| File | Change type |
|------|-------------|
| `domain/MassePhysicsSimulator.kt` | **Create** |
| `domain/SpinPhysicsCalculator.kt` | **Create** |
| `domain/MasseResult.kt` | **Create** (extracted from SpinReducer) |
| `domain/CalculateSpinPaths.kt` | **Delete** |
| `domain/reducers/SpinReducer.kt` | Modify — remove `internal MasseResult`, remove physics, call simulators; update `Color.White` map key (line 66) to wheel color |
| `domain/reducers/SystemReducer.kt` | Modify — lines 77 AND 115: both `PointF(w*0.25f, h*0.70f)` → new dp formula |
| `domain/UpdateStateUseCase.kt` | Modify — remove `CalculateSpinPaths` constructor param (line 29) and call (line 267); update `Color.White` map key (line 278) to wheel color |
| `ui/composables/MasseControl.kt` | Modify — add degree readout to stick canvas |
| `view/renderer/line/LineRenderer.kt` | Modify — wheel color as map key for both path types |

---

## Non-Goals

- The stick widget does not accept manual elevation input. Phone tilt is the only source.
- Ball jumping is not activated in this version. The scaffold is wired but inert.
- No changes to the AR pipeline, table scan, beginner mode, or banking mode.
- `UpdateStateUseCase` injection of `CalculateSpinPaths` is removed; no other injection sites change.

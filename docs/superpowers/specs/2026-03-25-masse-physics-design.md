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
| `MasseResult` | **Modified.** Two new stubbed fields for v1.4 jump. |
| `SpinReducer` | **Modified.** Thin caller; all physics logic moves out. |
| `MasseControl.kt` | **Modified.** Stick canvas gains live degree readout. |
| `LineRenderer.kt` | **Modified.** Both paths colored from the color wheel. |
| `ProtractorScreen.kt` | **Modified.** New default `spinControlCenter` position. |

---

## 1. Physics Model — Massé (`MassePhysicsSimulator`)

### Location
`app/src/main/java/com/hereliesaz/cuedetat/domain/MassePhysicsSimulator.kt`

### Interface
```kotlin
object MassePhysicsSimulator {
    fun simulate(
        contactOffset: PointF,  // normalized -1..1 on each axis; x = lateral, y = vertical
        elevationDeg: Float,    // cue elevation above table, derived from phone pitch
        shotAngle: Float,       // radians, world space (angle from cue ball to ghost ball)
        table: TableModel,
        mu: Float = 1.5f        // kinetic friction coefficient (tunable)
    ): MasseResult
}
```

### Physical Model

The ball is modelled as a solid sphere (moment of inertia `I = (2/5)mr²`). After the cue strike the ball slides on the table; friction converts spin into lateral velocity, producing the characteristic massé curve. When slip drops below a threshold the ball enters the rolling phase and travels in a straight line.

**Coordinate frame:** All simulation runs in a local frame with +x along the shot direction. After simulation the path is rotated to world space using `shotAngle`.

#### Initial Conditions

```
α   = elevationDeg in radians
V₀  = base speed constant (logical units/step)

vx₀ = V₀ · cos(α)                                  // forward speed; reduced by elevation
vy₀ = 0                                             // no initial lateral speed
ωx₀ = -(5/2) · V₀ · sin(α) · contactOffset.x / r  // sidetilt spin (curves the path)
ωy₀ = -(5/2) · V₀ · sin(α) · contactOffset.y / r  // backspin/topspin (affects forward decay)
```

The `5/2` factor is exact for a solid sphere and replaces the previous magic constants.

#### Per-Step Integration (100 steps)

```
// Slip velocity at the contact point (bottom of ball)
slip_x = vx - r·ωy
slip_y = vy + r·ωx      ← this drives the lateral curve

slip_speed = √(slip_x² + slip_y²)

if slip_speed > ε:       // kinetic friction phase
    fx = -μg · slip_x / slip_speed
    fy = -μg · slip_y / slip_speed

    vx += fx · dt
    vy += fy · dt
    ωy += (5/2) · fx / r · dt    // real torque from table friction
    ωx += (5/2) · fy / r · dt

else:                    // rolling phase — spin has converted, ball travels straight
    vx *= (1 - μ_roll · dt)
    vy *= (1 - μ_roll · dt)

pos += (vx, vy) · dt
```

The lateral curve emerges naturally: high elevation → large `ωx` → large `slip_y` → strong lateral friction → ball curves. As lateral velocity builds, `slip_y` decreases, friction disappears, the ball straightens. This is physically correct massé behaviour.

#### Rail Collisions

When a step would cross a rail:
1. Find the precise intersection point via linear interpolation.
2. Reflect `(vx, vy)` against the rail normal.
3. Apply the same torque equation to `(ωx, ωy)` using the reflected friction impulse.
4. Scale post-collision speed by elastic coefficient `e = 0.65` (tunable).

#### Output Path

After 100 steps (or pocket/stop), rotate all accumulated points by `shotAngle + π/2` and translate to cue ball world position.

---

## 2. Physics Model — Spin Paths (`SpinPhysicsCalculator`)

### Location
`app/src/main/java/com/hereliesaz/cuedetat/domain/SpinPhysicsCalculator.kt`

### Rationale

Spin paths run on every drag-frame update and need to be cheap. Full numerical integration is not required: the post-contact cue ball trajectory is essentially linear with modified rail angles. An analytical per-segment model gives physically correct results at a fraction of the cost.

### Interface
```kotlin
object SpinPhysicsCalculator {
    fun calculatePath(
        spinOffset: PointF,     // selected impact point, normalized -1..1
        cueBallPos: PointF,     // logical world coords
        targetBallPos: PointF,
        table: TableModel,
        maxBounces: Int = 2
    ): List<PointF>
}
```

### Physical Model

#### Post-Contact Cue Ball Direction

The cue ball deflects from the tangent line by a squirt angle:

```
squirt = k₁ · spinOffset.x · sin(cutAngle)
```

where `cutAngle` is the angle between the shot line and the tangent line, and `k₁ ≈ 0.04` (derived from standard squirt coefficients). The resulting direction is `tangentAngle + squirt`.

#### Spin State

Initial sidespin magnitude:
```
ω₀ = |spinOffset.x|    // normalized, direction encoded by sign
```

Spin decays exponentially with distance travelled:
```
ω(d) = ω₀ · e^(-k₃ · d)    // k₃ ≈ 0.008 per logical unit
```

#### Per-Rail Throw

At each rail bounce, the rebound angle is modified by the spin at the moment of contact:

```
rebound_angle = incident_angle + k₂ · ω(d) · cos(incident_angle)
```

- `k₂ ≈ 0.15` (rail throw coefficient, derived from billiards physics literature)
- Positive `spinOffset.x` = running English (widens angle); negative = reverse English (narrows)
- After the bounce, ω decays for the next segment

#### Segment Construction

1. Compute post-contact direction (squirt-adjusted tangent line).
2. Find first rail intersection.
3. Apply throw formula to get new direction.
4. Decay spin. Repeat up to `maxBounces`.
5. Return list of segment endpoint `PointF`s.

---

## 3. Data Structures

### `MasseResult` (modified)

```kotlin
data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int,
    val impactPoints: List<PointF>,
    // v1.4 scaffold — both stubbed to inert values until jump activation
    val isAirborne: Boolean = false,   // true when elevation > JUMP_THRESHOLD_DEG (~72°)
    val peakHeight: Float = 0f         // max height above table in logical units
)
```

`JUMP_THRESHOLD_DEG` is defined as a constant in `MassePhysicsSimulator`. The simulator returns `isAirborne = false` and `peakHeight = 0f` for all inputs until v1.4. The renderer checks `isAirborne` and renders the airborne segment as a dashed line when true.

### `SpinReducer` (modified)

Becomes a thin coordinator:

```kotlin
// Massé
MassePhysicsSimulator.simulate(contactOffset, elevationDeg, shotAngle, table)

// Spin paths
SpinPhysicsCalculator.calculatePath(spinOffset, cueBallPos, targetBallPos, table)
```

All heuristic physics equations are removed from `SpinReducer`. No physics logic remains in the reducer itself.

---

## 4. Rendering

### Trajectory Color (both Spin and Massé)

Both path types derive their render color from the same formula applied to the selected impact point:

```kotlin
val angle    = atan2(selectedOffset.y, selectedOffset.x)
val distance = selectedOffset.length() / MAX_OFFSET_RADIUS  // normalized 0..1
val pathColor = SpinColorUtils.getColorFromAngleAndDistance(angle, distance)
```

- **Massé path:** `LinearGradient` from `pathColor` (opaque, at cue ball) to `pathColor` with alpha=0 at the tail. Glow layer uses `pathColor` at 30% opacity. Replaces the previous hardcoded white gradient.
- **Spin path:** Single path rendered in `pathColor` with opacity decreasing per segment by distance. Replaces any prior multi-color or fixed-color logic.
- **Fallback:** If no offset is selected, use white. If `selectedSpinOffset` is null but `lingeringSpinOffset` is not, use the lingering offset's color.

### Stick Gauge Degree Readout (`MasseControl`)

The stick side-view canvas gains a live text label:

```kotlin
"${elevationAngle.roundToInt()}°"
```

- **Position:** Bottom-right of the stick canvas, above the baseline reference line.
- **Font:** Barbaro, same point size as other overlay labels.
- **Color:** White at 80% opacity.
- **Updates:** Every frame via the existing `elevationAngle` parameter (derived from `90f - abs(pitchAngle)`). Read-only — no user interaction. The stick gauge remains a pure readout of phone tilt.

---

## 5. Default Widget Position

### Current Behaviour

`spinControlCenter: PointF?` initializes to `null`. Both `SpinControl` and `MasseControl` are invisible until it is set. The first-time initialization occurs in `ProtractorScreen` (via `BoxWithConstraints` or `LaunchedEffect`).

### New Default

```kotlin
// Center X: right side, clear of zoom slider (~40dp wide) + half-wheel (60dp) + 8dp gap
val defaultX = constraints.maxWidth.toFloat() - 108.dp.toPx()

// Center Y: below top bar (48dp) + half-wheel (60dp) + 8dp gap
val defaultY = 116.dp.toPx()

spinControlCenter = PointF(defaultX, defaultY)
```

This places the wheel center approximately 108dp from the right edge and 116dp from the top — visually below the app icon row and well clear of the zoom slider. The existing double-tap-drag relocation gesture is unchanged.

---

## 6. v1.4 Jump Scaffold

The following are defined now but produce no visible output until v1.4:

| Item | Value now | v1.4 activation |
|------|-----------|-----------------|
| `MasseResult.isAirborne` | always `false` | `true` when elevation > `JUMP_THRESHOLD_DEG` |
| `MasseResult.peakHeight` | always `0f` | computed from vertical impulse |
| `JUMP_THRESHOLD_DEG` | `72f` (constant) | same value, now meaningful |
| Renderer airborne branch | `if (isAirborne)` draws dashed | same branch, now triggers |

In v1.4 the simulator extends the integration to 3D for the airborne phase: the ball follows a parabolic arc (initial upward velocity from vertical cue impulse, gravity pulls it back down), lands, then continues on the table surface with residual spin.

---

## Files Touched

| File | Change type |
|------|-------------|
| `domain/MassePhysicsSimulator.kt` | **Create** |
| `domain/SpinPhysicsCalculator.kt` | **Create** |
| `domain/reducers/SpinReducer.kt` | Modify — remove physics, call simulators |
| `data/VisionData.kt` (or wherever `MasseResult` lives) | Modify — add two fields |
| `ui/composables/MasseControl.kt` | Modify — add degree readout to stick canvas |
| `view/renderer/line/LineRenderer.kt` | Modify — color from wheel for both path types |
| `ui/ProtractorScreen.kt` | Modify — new default `spinControlCenter` |

---

## Non-Goals

- The stick widget does not accept manual elevation input. Phone tilt is the only source.
- Ball jumping is not activated in this version. The scaffold is wired but inert.
- No changes to the AR pipeline, table scan, beginner mode, or banking mode.

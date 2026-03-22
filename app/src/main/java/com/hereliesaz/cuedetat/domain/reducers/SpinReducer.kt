package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Reducer function responsible for handling Spin (English) related events.
 *
 * This manages the physics of failure:
 * - Dragging/Applying spin.
 * - Calculating non-linear trajectories (masse, squirt) with cloth jitter.
 * - Truncating paths on ball collisions and pocket entries.
 * - Reflecting paths on cushion collisions (rebounds) with speed loss.
 * - Fading the path visibility over time via a decay alpha.
 */
internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.SpinApplied -> {
            state.copy(
                selectedSpinOffset = action.offset,
                valuesChangedSinceReset = true,
                lingeringSpinOffset = null,
                spinPaths = mapOf(Color.White to generateMassePath(action.offset, state)),
                spinPathsAlpha = 1.0f
            )
        }

        is MainScreenEvent.SpinPathTick -> {
            val nextAlpha = (state.spinPathsAlpha - 0.05f).coerceAtLeast(0f)
            state.copy(
                spinPathsAlpha = nextAlpha,
                spinPaths = if (nextAlpha <= 0f) emptyMap() else state.spinPaths
            )
        }

        is MainScreenEvent.SpinSelectionEnded -> {
            state.copy(
                lingeringSpinOffset = state.selectedSpinOffset,
                selectedSpinOffset = null
            )
        }

        is MainScreenEvent.DragSpinControl -> {
            val currentCenter = state.spinControlCenter ?: return state
            val newCenter = PointF(
                currentCenter.x + action.delta.x,
                currentCenter.y + action.delta.y
            )
            state.copy(spinControlCenter = newCenter)
        }

        is MainScreenEvent.ClearSpinState -> {
            state.copy(
                lingeringSpinOffset = null,
                spinPaths = emptyMap(),
                spinPathsAlpha = 0f
            )
        }

        else -> state
    }
}

/**
 * Calculates a trajectory that respects the laws of friction, deflection, and pockets.
 *
 * @param offset The normalized PointF representing cue tip contact.
 * @param state The current application state.
 * @return A list of [PointF] coordinates tracing the path relative to the cue ball.
 */
private fun generateMassePath(offset: PointF, state: CueDetatState): List<PointF> {
    val points = mutableListOf<PointF>()
    val steps = 60
    val mu = 1.8f
    val random = Random(42)

    // Physics Constants
    val compression = if (offset.y < 0) {
        (1.0f - (abs(offset.y) * 0.45f)).coerceAtLeast(0.4f)
    } else {
        1.0f + (offset.y * 0.2f)
    }

    val baseDeflection = 40f
    val dynamicDeflection = baseDeflection + (abs(offset.y) * 55f)
    val masseIntensity = 220f
    val velocityBase = 350f

    val energyRestitution = 0.75f
    var currentVelocityScale = 1.0f

    val cuePos = state.onPlaneBall?.center ?: PointF(0f, 0f)
    val obstacles = state.obstacleBalls
    val table = state.table

    // Pocket drop threshold: slightly larger than ball radius to account for gravity grab.
    val pocketThreshold = LOGICAL_BALL_RADIUS * 1.3f

    var reflectionXMultiplier = 1f
    var reflectionYMultiplier = 1f

    var lastRawX = 0f
    var lastRawY = 0f

    points.add(PointF(0f, 0f))

    for (i in 1..steps) {
        val t = i.toFloat() / steps

        val rawX = (-offset.x * dynamicDeflection * t) + ((offset.x * masseIntensity) * mu * t.pow(2.2f))
        val rawY = -(t * velocityBase * compression)

        val jitter = (random.nextFloat() - 0.5f) * 1.2f * t

        val dRawX = (rawX - lastRawX) + jitter
        val dRawY = (rawY - lastRawY) + jitter

        var dx = dRawX * reflectionXMultiplier * currentVelocityScale
        var dy = dRawY * reflectionYMultiplier * currentVelocityScale

        val currentPoint = points.last()
        val nextLocalPoint = PointF(currentPoint.x + dx, currentPoint.y + dy)
        val worldNext = PointF(cuePos.x + nextLocalPoint.x, cuePos.y + nextLocalPoint.y)

        // 1. Pocket Check: If the ball hits a pocket, it stops existing.
        var fellInPocket = false
        for (pocket in table.pockets) {
            val distToPocket = sqrt((worldNext.x - pocket.x).pow(2) + (worldNext.y - pocket.y).pow(2))
            if (distToPocket < pocketThreshold) {
                fellInPocket = true
                break
            }
        }
        if (fellInPocket) break

        // 2. Cushion Collision using Table's geometry.
        val intersection = table.findRailIntersectionAndNormal(
            PointF(cuePos.x + currentPoint.x, cuePos.y + currentPoint.y),
            worldNext
        )

        if (intersection != null) {
            val (_, normal) = intersection
            val reflectedVelocity = table.reflect(PointF(dx, dy), normal, offset.x)

            dx = reflectedVelocity.x * energyRestitution
            dy = reflectedVelocity.y * energyRestitution

            if (abs(normal.x) > 0.5f) reflectionXMultiplier *= -1f
            if (abs(normal.y) > 0.5f) reflectionYMultiplier *= -1f

            currentVelocityScale *= energyRestitution
        }

        // 3. Ball Collision (Terminal).
        val finalNextPoint = PointF(currentPoint.x + dx, currentPoint.y + dy)
        val finalWorldPoint = PointF(cuePos.x + finalNextPoint.x, cuePos.y + finalNextPoint.y)

        var hitBall = false
        for (obstacle in obstacles) {
            val dist = sqrt((finalWorldPoint.x - obstacle.center.x).pow(2) + (finalWorldPoint.y - obstacle.center.y).pow(2))
            if (dist < LOGICAL_BALL_RADIUS * 2) {
                hitBall = true
                break
            }
        }

        if (hitBall) break

        points.add(finalNextPoint)
        lastRawX = rawX
        lastRawY = rawY

        if (currentVelocityScale < 0.1f) break
    }

    return points
}
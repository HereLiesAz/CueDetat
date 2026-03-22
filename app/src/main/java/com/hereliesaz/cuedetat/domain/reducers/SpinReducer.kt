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
 * Data class to hold the results of a trajectory calculation.
 */
private data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int?
)

internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleMasseMode -> {
            val nextActive = !state.isMasseModeActive
            state.copy(
                isMasseModeActive = nextActive,
                // Clear highlights and paths when exiting the mode.
                spinPaths = if (!nextActive) emptyMap() else state.spinPaths,
                aimedPocketIndex = if (!nextActive) null else state.aimedPocketIndex
            )
        }

        is MainScreenEvent.SpinApplied -> {
            val rawOffset = action.offset
            val distance = sqrt(rawOffset.x.pow(2) + rawOffset.y.pow(2))
            val clampedOffset = if (distance > 1f) {
                PointF(rawOffset.x / distance, rawOffset.y / distance)
            } else {
                rawOffset
            }

            val result = generateMassePath(clampedOffset, state)
            state.copy(
                selectedSpinOffset = clampedOffset,
                valuesChangedSinceReset = true,
                lingeringSpinOffset = null,
                spinPaths = mapOf(Color.White to result.points),
                aimedPocketIndex = result.pocketIndex,
                spinPathsAlpha = 1.0f
            )
        }

        is MainScreenEvent.SpinPathTick -> {
            val nextAlpha = (state.spinPathsAlpha - 0.05f).coerceAtLeast(0f)
            state.copy(
                spinPathsAlpha = nextAlpha,
                spinPaths = if (nextAlpha <= 0f) emptyMap() else state.spinPaths,
                // Extinguish the pocket highlight when the path evaporates.
                aimedPocketIndex = if (nextAlpha <= 0f) null else state.aimedPocketIndex
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
                spinPathsAlpha = 0f,
                aimedPocketIndex = null
            )
        }

        else -> state
    }
}

/**
 * Calculates a trajectory and identifies any pocket intersections.
 */
private fun generateMassePath(offset: PointF, state: CueDetatState): MasseResult {
    val points = mutableListOf<PointF>()
    val steps = 60
    val mu = 1.8f
    val random = Random(42)

    val compression = if (offset.y < 0) (1.0f - (abs(offset.y) * 0.45f)).coerceAtLeast(0.4f) else 1.0f + (offset.y * 0.2f)
    val dynamicDeflection = 40f + (abs(offset.y) * 55f)
    val velocityBase = 350f

    val cuePos = state.onPlaneBall?.center ?: PointF(0f, 0f)
    val obstacles = state.obstacleBalls
    val table = state.table
    val pocketThreshold = LOGICAL_BALL_RADIUS * 1.3f

    var reflectionXMultiplier = 1f
    var reflectionYMultiplier = 1f
    var currentVelocityScale = 1.0f
    var lastRawX = 0f
    var lastRawY = 0f
    var hitPocketIndex: Int? = null

    points.add(PointF(0f, 0f))

    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val rawX = (-offset.x * dynamicDeflection * t) + ((offset.x * 220f) * mu * t.pow(2.2f))
        val rawY = -(t * velocityBase * compression)
        val jitter = (random.nextFloat() - 0.5f) * 1.2f * t

        val dRawX = (rawX - lastRawX) + jitter
        val dRawY = (rawY - lastRawY) + jitter

        var dx = dRawX * reflectionXMultiplier * currentVelocityScale
        var dy = dRawY * reflectionYMultiplier * currentVelocityScale

        val currentLocalPoint = points.last()
        val nextLocalPoint = PointF(currentLocalPoint.x + dx, currentLocalPoint.y + dy)
        val worldNext = PointF(cuePos.x + nextLocalPoint.x, cuePos.y + nextLocalPoint.y)

        // 1. Pocket Check: If the trajectory finds a home, we record the index and stop.
        var fellInPocket = false
        for (idx in table.pockets.indices) {
            val pocket = table.pockets[idx]
            val distToPocket = sqrt((worldNext.x - pocket.x).pow(2) + (worldNext.y - pocket.y).pow(2))
            if (distToPocket < pocketThreshold) {
                fellInPocket = true
                hitPocketIndex = idx
                break
            }
        }
        if (fellInPocket) break

        // 2. Cushion Collision
        val worldCurrent = PointF(cuePos.x + currentLocalPoint.x, cuePos.y + currentLocalPoint.y)
        val intersection = table.findRailIntersectionAndNormal(worldCurrent, worldNext)
        if (intersection != null) {
            val (_, normal) = intersection
            val reflectedVelocity = table.reflect(PointF(dx, dy), normal, offset.x)
            dx = reflectedVelocity.x * 0.75f
            dy = reflectedVelocity.y * 0.75f
            if (abs(normal.x) > 0.5f) reflectionXMultiplier *= -1f
            if (abs(normal.y) > 0.5f) reflectionYMultiplier *= -1f
            currentVelocityScale *= 0.75f
        }

        // 3. Ball Collision
        val finalNextPoint = PointF(currentLocalPoint.x + dx, currentLocalPoint.y + dy)
        val finalWorldPoint = PointF(cuePos.x + finalNextPoint.x, cuePos.y + finalNextPoint.y)
        var hitBall = false
        for (obstacle in obstacles) {
            if (sqrt((finalWorldPoint.x - obstacle.center.x).pow(2) + (finalWorldPoint.y - obstacle.center.y).pow(2)) < LOGICAL_BALL_RADIUS * 2) {
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

    return MasseResult(points, hitPocketIndex)
}
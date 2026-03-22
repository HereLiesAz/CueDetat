package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

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
                isSpinControlVisible = if (nextActive) false else state.isSpinControlVisible,
                spinPaths = if (!nextActive) emptyMap() else state.spinPaths,
                aimedPocketIndex = if (!nextActive) null else state.aimedPocketIndex
            )
        }

        is MainScreenEvent.ToggleSpinControl -> {
            val nextVisible = !state.isSpinControlVisible
            state.copy(
                isSpinControlVisible = nextVisible,
                isMasseModeActive = if (nextVisible) false else state.isMasseModeActive,
                spinPaths = if (!nextVisible) emptyMap() else state.spinPaths
            )
        }

        is MainScreenEvent.SpinApplied -> {
            val rawOffset = action.offset
            val density = state.screenDensity
            val radiusPx = 60f * density

            // Normalize for physics (-1.0 to 1.0)
            val nx = (rawOffset.x - radiusPx) / radiusPx
            val ny = (rawOffset.y - radiusPx) / radiusPx
            val dist = sqrt(nx.pow(2) + ny.pow(2))

            val physicsOffset = if (dist > 1.0f) PointF(nx / dist, ny / dist) else PointF(nx, ny)

            // Calculate the clamped pixel offset for the UI drawing logic
            // This prevents the dot from being "stuck" outside the circle.
            val clampedRawOffset = PointF(
                (physicsOffset.x * radiusPx) + radiusPx,
                (physicsOffset.y * radiusPx) + radiusPx
            )

            val result = generateMassePath(physicsOffset, state)
            state.copy(
                selectedSpinOffset = clampedRawOffset,
                valuesChangedSinceReset = true,
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
            state.copy(spinControlCenter = PointF(currentCenter.x + action.delta.x, currentCenter.y + action.delta.y))
        }

        is MainScreenEvent.ClearSpinState -> {
            state.copy(lingeringSpinOffset = null, spinPaths = emptyMap(), spinPathsAlpha = 0f, aimedPocketIndex = null)
        }

        else -> state
    }
}

private fun generateMassePath(offset: PointF, state: CueDetatState): MasseResult {
    val points = mutableListOf<PointF>()
    val steps = 60
    val mu = 1.8f
    val random = Random(42)

    val compression = if (offset.y < 0) (1.0f - (abs(offset.y) * 0.45f)).coerceAtLeast(0.4f) else 1.0f + (offset.y * 0.2f)
    val dynamicDeflection = 40f + (abs(offset.y) * 55f)
    val velocityBase = 350f

    val cuePos = state.onPlaneBall?.center ?: PointF(0f, 0f)
    val table = state.table
    val pocketThreshold = LOGICAL_BALL_RADIUS * 1.3f

    var rx = 1f; var ry = 1f; var vScale = 1.0f
    var lx = 0f; var ly = 0f; var hitIdx: Int? = null

    points.add(PointF(0f, 0f))

    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val rawX = (-offset.x * dynamicDeflection * t) + ((offset.x * 220f) * mu * t.pow(2.2f))
        val rawY = -(t * velocityBase * compression)
        val jitter = (random.nextFloat() - 0.5f) * 1.2f * t

        val dx = (rawX - lx) + jitter
        val dy = (rawY - ly) + jitter

        var vX = dx * rx * vScale
        var vY = dy * ry * vScale

        val lastP = points.last()
        val nextP = PointF(lastP.x + vX, lastP.y + vY)
        val worldN = PointF(cuePos.x + nextP.x, cuePos.y + nextP.y)

        for (idx in table.pockets.indices) {
            val p = table.pockets[idx]
            if (sqrt((worldN.x - p.x).pow(2) + (worldN.y - p.y).pow(2)) < pocketThreshold) {
                hitIdx = idx; break
            }
        }
        if (hitIdx != null) break

        val worldC = PointF(cuePos.x + lastP.x, cuePos.y + lastP.y)
        val intersection = table.findRailIntersectionAndNormal(worldC, worldN)
        if (intersection != null) {
            val (_, normal) = intersection
            val reflected = table.reflect(PointF(vX, vY), normal, offset.x)
            vX = reflected.x * 0.75f
            vY = reflected.y * 0.75f
            if (abs(normal.x) > 0.5f) rx *= -1f
            if (abs(normal.y) > 0.5f) ry *= -1f
            vScale *= 0.75f
        }

        points.add(PointF(lastP.x + vX, lastP.y + vY))
        lx = rawX; ly = rawY
        if (vScale < 0.1f) break
    }
    return MasseResult(points, hitIdx)
}
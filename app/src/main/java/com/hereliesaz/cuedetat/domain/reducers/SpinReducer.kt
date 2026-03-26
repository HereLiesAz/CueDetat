package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MassePhysicsSimulator
import com.hereliesaz.cuedetat.domain.MasseResult
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleMasseMode -> {
            val nextActive = !state.isMasseModeActive
            state.copy(
                isMasseModeActive = nextActive,
                isSpinControlVisible = if (nextActive) false else state.isSpinControlVisible,
                spinPaths = emptyMap(),
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

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlin.math.sqrt

internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleMasseMode -> {
            val nextActive = !state.isMasseModeActive
            if (nextActive) {
                state.copy(isMasseModeActive = true, isSpinControlVisible = false)
            } else {
                state.copy(
                    isMasseModeActive = false,
                    spinPaths = emptyMap(),
                    selectedSpinOffset = null,
                    lingeringSpinOffset = null,
                    masseImpactPoints = emptyList(),
                    masseConnectsTarget = false,
                    aimedPocketIndex = null
                )
            }
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
            state.copy(
                selectedSpinOffset = clampedRawOffset,
                valuesChangedSinceReset = true,
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

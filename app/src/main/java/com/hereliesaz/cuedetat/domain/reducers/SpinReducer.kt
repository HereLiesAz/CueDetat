package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlin.math.atan2
import kotlin.math.sqrt

internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleMasseMode -> {
            val nextActive = !state.isMasseModeActive
            if (nextActive) {
                val cuePos = state.onPlaneBall?.center ?: PointF(0f, 0f)
                val ghostCuePos = state.protractorUnit.ghostCueBallCenter
                val initAngleDeg = Math.toDegrees(
                    atan2(
                        (ghostCuePos.y - cuePos.y).toDouble(),
                        (ghostCuePos.x - cuePos.x).toDouble(),
                    )
                ).toFloat()
                state.copy(
                    isMasseModeActive = true,
                    // See handleToggleBankingMode: these are alternative shot
                    // types, and leaving both set produced a masse dial drawn
                    // over bank-shot geometry that the aiming pass ignored.
                    isBankingMode = false,
                    bankingAimTarget = null,
                    isSpinControlVisible = false,
                    lingeringSpinOffset = null,
                    selectedSpinOffset = null,
                    masseShotAngleDeg = initAngleDeg,
                    spinPaths = emptyMap(),
                    masseImpactPoints = emptyList(),
                    masseConnectsTarget = false,
                    aimedPocketIndex = null
                )
            } else {
                state.copy(
                    isMasseModeActive = false,
                    masseShotAngleDeg = 0f,
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
            if (nextVisible) {
                // Enabling spin: fully exit masse mode if it was active
                state.copy(
                    isSpinControlVisible = true,
                    isMasseModeActive = false,
                    masseShotAngleDeg = 0f,
                    spinPaths = emptyMap(),
                    masseImpactPoints = emptyList(),
                    masseConnectsTarget = false,
                    selectedSpinOffset = null,
                    lingeringSpinOffset = null,
                    aimedPocketIndex = null
                )
            } else {
                state.copy(
                    isSpinControlVisible = false,
                    spinPaths = emptyMap(),
                    aimedPocketIndex = null
                )
            }
        }

        is MainScreenEvent.SpinApplied -> {
            val rawOffset = action.offset
            val density = state.screenDensity
            val radiusPx = 60f * density
            
            // Convert raw screen pixels (0..120dp) to normalized units (-1..1).
            // Convention: y is SCREEN-DOWN, matching the dial's indicator drawing. +y = striking
            // below centre = draw. Physics consumers (SpinPhysicsCalculator, CoreBridge.spinFrom)
            // use +y = follow and must negate y at their boundary.
            val nx = (rawOffset.x - radiusPx) / radiusPx
            val ny = (rawOffset.y - radiusPx) / radiusPx
            val dist = sqrt((nx * nx + ny * ny).toDouble()).toFloat()
            val normalizedOffset = if (dist > 1.0f) PointF(nx / dist, ny / dist) else PointF(nx, ny)
            
            state.copy(
                selectedSpinOffset = normalizedOffset,
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
            state.copy(
                spinControlCenter = clampSpinControlCenter(
                    PointF(currentCenter.x + action.delta.x, currentCenter.y + action.delta.y),
                    state.viewWidth, state.viewHeight, state.screenDensity,
                )
            )
        }

        is MainScreenEvent.ClearSpinState -> {
            state.copy(lingeringSpinOffset = null, spinPaths = emptyMap(), spinPathsAlpha = 0f, aimedPocketIndex = null)
        }

        else -> state
    }
}

/** Half the spin/masse dial's 120 dp square: the dial's centre must stay at least this far inside the view. */
private const val SPIN_CONTROL_HALF_SIZE_DP = 60f

/**
 * Keeps the dial's centre where the whole 120 dp square is on screen. The centre is persisted in
 * absolute pixels, so a drag toward an edge, or a rotation to a smaller dimension, could otherwise
 * leave the dial unreachable while its rail toggle still read "on". A view not yet measured
 * ([width] or [height] not positive) leaves [center] alone.
 */
internal fun clampSpinControlCenter(center: PointF, width: Int, height: Int, density: Float): PointF {
    if (width <= 0 || height <= 0) return center
    return PointF(
        clampSpinControlCoordinate(center.x, width, density),
        clampSpinControlCoordinate(center.y, height, density),
    )
}

/** One axis of [clampSpinControlCenter]; pure, so it is unit-testable off-device. */
internal fun clampSpinControlCoordinate(value: Float, extent: Int, density: Float): Float {
    val half = SPIN_CONTROL_HALF_SIZE_DP * density
    // A view smaller than the dial: centre it rather than produce an inverted range.
    return if (extent <= 2 * half) extent / 2f else value.coerceIn(half, extent - half)
}

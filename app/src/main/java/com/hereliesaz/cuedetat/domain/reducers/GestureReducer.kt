// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/GestureReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.Matrix
import android.graphics.PointF
import androidx.room.util.copy
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.hypot

class GestureReducer @Inject constructor(
    private val reducerUtils: ReducerUtils
) {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.Drag -> handleDrag(event, state)
            is MainScreenEvent.Release -> handleRelease(event, state)
            else -> state
        }
    }

    private fun handleDrag(event: MainScreenEvent.Drag, state: OverlayState): OverlayState {
        val logicalPoint = state.inversePitchMatrix.mapPoint(event.position)
        val isChanged = state.copy(valuesChangedSinceReset = true)

        return if (state.isBankingMode) {
            isChanged.copy(bankingAimTarget = logicalPoint)
        } else {
            val distToTarget = hypot((logicalPoint.x - state.protractorUnit.center.x).toDouble(), (logicalPoint.y - state.protractorUnit.center.y).toDouble()).toFloat()
            val distToCue = state.onPlaneBall?.let { hypot((logicalPoint.x - it.center.x).toDouble(), (logicalPoint.y - it.center.y).toDouble()).toFloat() } ?: Float.MAX_VALUE

            when {
                event.isLongPress || distToTarget < state.protractorUnit.radius -> isChanged.copy(
                    protractorUnit = state.protractorUnit.copy(center = logicalPoint)
                ).let(reducerUtils::snapViolatingBalls)

                distToCue < state.protractorUnit.radius -> isChanged.copy(
                    onPlaneBall = state.onPlaneBall?.copy(center = logicalPoint)
                ).let(reducerUtils::snapViolatingBalls)

                else -> isChanged.copy(
                    protractorUnit = state.protractorUnit.withRotation(
                        Math.toDegrees(
                            atan2(
                                (logicalPoint.y - state.protractorUnit.center.y).toDouble(),
                                (logicalPoint.x - state.protractorUnit.center.x).toDouble()
                            )
                        ).toFloat()
                    )
                )
            }
        }
    }

    private fun handleRelease(event: MainScreenEvent.Release, state: OverlayState): OverlayState {
        return state.copy(
            isMagnifierVisible = false,
            spinControlCenter = null
        )
    }

    private fun Matrix.mapPoint(point: PointF): PointF {
        val pts = floatArrayOf(point.x, point.y)
        this.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }
}
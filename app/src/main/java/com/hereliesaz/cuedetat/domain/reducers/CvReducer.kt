package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

internal fun reduceCvAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.CvDataUpdated -> {
            // Forward the data, but let the SnapReducer handle the candidates
            state.copy(visionData = action.visionData)
        }

        is MainScreenEvent.LockOrUnlockColor -> {
            if (state.lockedHsvColor == null) {
                state.copy(
                    lockedHsvColor = state.visionData?.detectedHsvColor,
                    lockedHsvStdDev = state.visionData?.detectedHsvColor?.let {
                        floatArrayOf(
                            10f,
                            50f,
                            50f
                        )
                    }
                )
            } else {
                state.copy(lockedHsvColor = null, lockedHsvStdDev = null)
            }
        }

        is MainScreenEvent.LockColor -> {
            state.copy(lockedHsvColor = action.hsvMean, lockedHsvStdDev = action.hsvStdDev)
        }

        is MainScreenEvent.ClearSamplePoint -> {
            state.copy(colorSamplePoint = null)
        }

        else -> state
    }
}
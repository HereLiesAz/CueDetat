package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState

internal fun reduceCvAction(state: CueDetatState, action: CueDetatAction): CueDetatState {
    return when (action) {
        is CueDetatAction.CvDataUpdated -> {
            // Forward the data, but let the SnapReducer handle the candidates
            state.copy(visionData = action.visionData)
        }

        is CueDetatAction.LockOrUnlockColor -> {
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

        is CueDetatAction.LockColor -> {
            state.copy(lockedHsvColor = action.hsvMean, lockedHsvStdDev = action.hsvStdDev)
        }

        is CueDetatAction.ClearSamplePoint -> {
            state.copy(colorSamplePoint = null)
        }

        else -> state
    }
}
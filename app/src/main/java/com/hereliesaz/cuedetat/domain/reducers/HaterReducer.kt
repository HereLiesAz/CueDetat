package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.hatemode.HaterState
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel

internal fun reduceHaterAction(
    currentState: HaterState,
    action: HaterViewModel.Action
): HaterState {
    return when (action) {
        is HaterViewModel.Action.UpdatePhysics -> {
            currentState.copy(
                bodies = action.bodies.map { body ->
                    HaterState.BodyState(
                        id = body.userData as? String ?: "",
                        x = body.position.x,
                        y = body.position.y,
                        angle = body.angle
                    )
                }
            )
        }

        is HaterViewModel.Action.SetHaterText -> {
            currentState.copy(haterText = action.text)
        }
    }
}
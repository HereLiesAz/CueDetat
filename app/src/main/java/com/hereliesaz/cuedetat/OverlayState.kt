package com.hereliesaz.cuedetat

import com.google.ar.sceneform.math.Vector3

/**
 * OverlayState holds the logical representation of all relevant entities:
 * cue ball, object ball, target, predicted aim, etc.
 * It is intended to drive rendering in AR or on 2D overlays.
 */
data class OverlayState(
    val cueBall: Vector3? = null,
    val objectBall: Vector3? = null,
    val targetPocket: Vector3? = null,
    val aimDirection: Vector3? = null,
    val tableCorners: List<Vector3>? = null,
    val predictedSpin: SpinType = SpinType.NONE,
    val shotConfidence: Float? = null
) {
    fun isComplete(): Boolean {
        return cueBall != null && objectBall != null && aimDirection != null
    }

    fun reset(): OverlayState {
        return OverlayState()
    }

    fun withUpdatedCueBall(newCue: Vector3): OverlayState {
        return copy(cueBall = newCue)
    }

    fun withUpdatedObjectBall(newObject: Vector3): OverlayState {
        return copy(objectBall = newObject)
    }

    fun withUpdatedAim(aim: Vector3): OverlayState {
        return copy(aimDirection = aim)
    }

    fun withConfidence(score: Float): OverlayState {
        return copy(shotConfidence = score)
    }

    fun withSpin(spin: SpinType): OverlayState {
        return copy(predictedSpin = spin)
    }
}

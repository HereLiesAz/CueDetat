package com.hereliesaz.cuedetat.view.model

import com.google.ar.sceneform.math.Vector3

/**
 * Holds transient UI and world state for ghost ball overlays.
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
    fun isComplete(): Boolean = cueBall != null && objectBall != null && aimDirection != null

    fun withUpdatedCueBall(newCue: Vector3) = copy(cueBall = newCue)
    fun withUpdatedObjectBall(newObject: Vector3) = copy(objectBall = newObject)
    fun withUpdatedAim(aim: Vector3) = copy(aimDirection = aim)
    fun withConfidence(score: Float) = copy(shotConfidence = score)
    fun withSpin(spin: SpinType) = copy(predictedSpin = spin)
}
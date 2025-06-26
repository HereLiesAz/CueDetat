package com.hereliesaz.cuedetat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.ar.sceneform.math.Vector3
import com.hereliesaz.cuedetat.view.model.OverlayState
import com.hereliesaz.cuedetat.model.SpinType
import com.hereliesaz.cuedetat.view.model.SpinType

class OverlayViewModel : ViewModel() {

    private val _overlayState = MutableLiveData(OverlayState())
    val overlayState: LiveData<OverlayState> get() = _overlayState

    fun updateCueBall(position: Vector3) {
        _overlayState.value = _overlayState.value?.withUpdatedCueBall(position)
    }

    fun updateObjectBall(position: Vector3) {
        _overlayState.value = _overlayState.value?.withUpdatedObjectBall(position)
    }

    fun updateAimDirection(direction: Vector3) {
        _overlayState.value = _overlayState.value?.withUpdatedAim(direction)
    }

    fun updateSpin(spin: SpinType) {
        _overlayState.value = _overlayState.value?.withSpin(spin)
    }

    fun updateConfidence(conf: Float) {
        _overlayState.value = _overlayState.value?.withConfidence(conf)
    }

    fun updatePocket(pocket: Vector3) {
        _overlayState.value = _overlayState.value?.copy(targetPocket = pocket)
    }

    fun updateTableCorners(corners: List<Vector3>) {
        _overlayState.value = _overlayState.value?.copy(tableCorners = corners)
    }

    fun resetState() {
        _overlayState.value = OverlayState()
    }
}

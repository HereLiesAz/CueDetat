package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.hereliesaz.cuedetat.ui.state.BallState
import com.hereliesaz.cuedetat.ui.state.TableState
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: UiEvent) {
        viewModelScope.launch {
            when (event) {
                is UiEvent.OnTap -> handlePlaneTap(event.hitResult)
                is UiEvent.OnReset -> resetScene()
                is UiEvent.ToggleDrawer -> toggleDrawer()
                is UiEvent.SetShotPower -> setShotPower(event.power)
                is UiEvent.SetSpin -> setSpin(event.spin)
                is UiEvent.ExecuteShot -> executeShot()
            }
        }
    }

    private fun handlePlaneTap(hitResult: HitResult) {
        if (_uiState.value.table != null) return

        val anchor = hitResult.createAnchor()
        _uiState.update { currentState ->
            currentState.copy(
                table = TableState(pose = anchor.pose),
                cueBall = BallState(pose = anchor.pose.compose(Pose.makeTranslation(0f, 0.05f, -0.5f))),
                objectBall = BallState(pose = anchor.pose.compose(Pose.makeTranslation(0f, 0.05f, 0.5f))),
                isAiming = true
            )
        }
    }

    private fun resetScene() {
        _uiState.update {
            it.copy(table = null, cueBall = null, objectBall = null, isAiming = true)
        }
    }

    private fun toggleDrawer() {
        _uiState.update { it.copy(isDrawerOpen = !it.isDrawerOpen) }
    }

    private fun setShotPower(power: Float) {
        _uiState.update { it.copy(shotPower = power) }
    }

    private fun setSpin(spin: Offset) {
        _uiState.update { it.copy(cueballSpin = spin) }
    }

    private fun executeShot() {
        if (_uiState.value.table == null) return
        _uiState.update { it.copy(isAiming = false) }

        // TODO: Implement Physics Simulation
        // 1. Get current state: ball positions, power, spin.
        // 2. Start a physics loop (e.g., using another coroutine).
        // 3. In the loop, use PhysicsUtil to calculate collisions and new positions.
        // 4. Update the cueBall and objectBall states in the UiState flow.
        // 5. When the simulation ends, set isAiming = true.
    }
}
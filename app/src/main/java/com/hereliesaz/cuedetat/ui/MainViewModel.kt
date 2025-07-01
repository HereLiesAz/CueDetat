package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.xr.runtime.Session
import androidx.xr.arcore.Plane
import androidx.xr.scenecore.Entity
import com.hereliesaz.cuedetat.data.UserPreferenceRepository
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var session: Session? = null

    init {
        viewModelScope.launch {
            userPreferenceRepository.userPreferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        isDarkMode = preferences.isDarkMode,
                        showHelp = preferences.showHelp
                    )
                }
            }
        }
    }

    fun onEvent(event: UiEvent) {
        viewModelScope.launch {
            when (event) {
                is UiEvent.OnScreenTap -> handleScreenTap(event.offset)
                is UiEvent.OnReset -> resetScene()
                is UiEvent.SetShotPower -> setShotPower(event.power)
                is UiEvent.SetSpin -> setSpin(event.spin)
                is UiEvent.ExecuteShot -> executeShot()
                is UiEvent.SetSession -> setSession(event.session)
                is UiEvent.ToggleHelp -> toggleHelp()
            }
        }
    }

    private fun setSession(session: Session?) {
        this.session = session
    }

    private fun handleScreenTap(offset: Offset) {
        val currentSession = session ?: return
        if (_uiState.value.table != null) return

        // The correct way to get frame data is to subscribe to frame updates.
        currentSession.subscribe(this) { frame ->
            val hitResults = frame.hitTest(offset.x, offset.y)
            val planeHit = hitResults.firstOrNull {
                it.trackable is Plane && (it.trackable as Plane).type == Plane.Type.HORIZONTAL_UPWARD_FACING
            }

            if (planeHit != null) {
                // An entity is created via the session, not by direct instantiation.
                val tableEntity = currentSession.createEntity().apply {
                    // An anchor is created from the hit result's pose.
                    addAnchor(currentSession.createAnchor(planeHit.pose))
                }
                _uiState.update {
                    it.copy(table = tableEntity)
                }

                // Unsubscribe after we've found a plane and placed the table.
                currentSession.unsubscribe(this)
            }
        }
    }

    private fun resetScene() {
        // TODO: Properly dispose of entities before nulling them out.
        _uiState.update {
            it.copy(table = null, cueBall = null, objectBall = null, isAiming = true)
        }
    }

    private fun toggleHelp() {
        _uiState.update { it.copy(showHelp = !it.showHelp) }
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
    }
}

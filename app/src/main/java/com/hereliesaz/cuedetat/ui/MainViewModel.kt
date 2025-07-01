package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
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

    private var arSession: Session? = null

    init {
        viewModelScope.launch {
            userPreferenceRepository.userPreferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        isDarkMode = preferences.isDarkMode,
                        showHelp = preferences.showHelp,
                        shotType = preferences.shotType
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
                is UiEvent.ToggleHelpDialog -> toggleHelpDialog()
            }
        }
    }

    private fun setSession(session: Session?) {
        this.arSession = session
    }

    private fun handleScreenTap(offset: Offset) {
        val session = arSession ?: return

        // We get the current frame here, on demand, when a tap happens.
        val frame = try {
            session.update()
        } catch (e: Exception) {
            // Can happen if the session is paused or shutting down
            null
        } ?: return

        if (frame.camera.trackingState != TrackingState.TRACKING) return

        // Perform the hit test inside the ViewModel
        val hitResults = frame.hitTest(offset.x, offset.y)
        val hitResult = hitResults.firstOrNull {
            val trackable = it.trackable
            trackable is Plane && trackable.isPoseInPolygon(it.hitPose)
        } ?: return

        // Now call the logic to handle the specific hit
        handleHitResult(hitResult)
    }

    private fun handleHitResult(hitResult: HitResult) {
        val session = arSession ?: return
        if (_uiState.value.table != null) return // Only place table once

        val trackable = hitResult.trackable
        if (trackable is Plane && trackable.isPoseInPolygon(hitResult.hitPose) && trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING) {
            val newAnchor = session.createAnchor(hitResult.hitPose)
            _uiState.update {
                it.copy(table = newAnchor)
            }
        }
    }

    private fun resetScene() {
        // Dispose of anchors before removing them from state to clean up ARCore resources
        _uiState.value.table?.detach()
        _uiState.value.cueBall?.detach()
        _uiState.value.objectBall?.detach()

        _uiState.update {
            it.copy(table = null, cueBall = null, objectBall = null, isAiming = true)
        }
    }

    private fun toggleHelpDialog() {
        val show = !_uiState.value.showHelp
        _uiState.update { it.copy(showHelp = show) }
        viewModelScope.launch {
            userPreferenceRepository.saveShowHelp(show)
        }
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
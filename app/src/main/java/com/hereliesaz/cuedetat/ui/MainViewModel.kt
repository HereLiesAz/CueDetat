package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.*
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
                is UiEvent.SetShotPower -> _uiState.update { it.copy(shotPower = event.power) }
                is UiEvent.SetSpin -> _uiState.update { it.copy(cueballSpin = event.spin) }
                is UiEvent.ExecuteShot -> { /* TODO: Implement Shot */ }
                is UiEvent.SetSession -> arSession = event.session
                is UiEvent.ToggleHelpDialog -> _uiState.update { it.copy(showHelp = !it.showHelp) }
                is UiEvent.ToggleArMode -> toggleArMode()
                is UiEvent.ToggleFlashlight -> { /* Handled by Activity */ }
            }
        }
    }

    private fun handleScreenTap(offset: Offset) {
        val session = arSession ?: return
        val frame = try { session.update() } catch (e: Exception) { null } ?: return

        // Do not proceed if ARCore is not tracking
        if (frame.camera.trackingState != TrackingState.TRACKING) return

        // Always perform a hit test.
        val hitResults = frame.hitTest(offset.x, offset.y)
        val hitResult = hitResults.firstOrNull {
            val trackable = it.trackable
            trackable is Plane && trackable.isPoseInPolygon(it.hitPose)
        } ?: return // If no valid plane was hit, do nothing.

        // Determine what to place based on the current state
        when {
            _uiState.value.table == null -> placeAnchor(hitResult, "table")
            _uiState.value.cueBall == null -> placeAnchor(hitResult, "cueBall")
            _uiState.value.objectBall == null -> placeAnchor(hitResult, "objectBall")
            else -> {
                // TODO: Logic to move existing balls
            }
        }
    }

    private fun placeAnchor(hitResult: HitResult, objectToPlace: String) {
        val newAnchor = arSession?.createAnchor(hitResult.hitPose) ?: return
        when(objectToPlace) {
            "table" -> _uiState.update { it.copy(table = newAnchor, instructionText = "Tap on table to place Cue Ball") }
            "cueBall" -> _uiState.update { it.copy(cueBall = newAnchor, instructionText = "Tap on table to place Object Ball") }
            "objectBall" -> _uiState.update { it.copy(objectBall = newAnchor, instructionText = "Aim your shot") }
        }
    }

    private fun toggleArMode() {
        val newMode = !_uiState.value.isArMode
        _uiState.update { it.copy(isArMode = newMode) }
        resetScene()
    }

    private fun resetScene() {
        // Detach anchors to clean up ARCore resources
        _uiState.value.table?.detach()
        _uiState.value.cueBall?.detach()
        _uiState.value.objectBall?.detach()

        _uiState.update {
            it.copy(
                table = null,
                cueBall = null,
                objectBall = null,
                isAiming = true,
                instructionText = if(it.isArMode) "Move phone to find a surface..." else "Manual Mode: Tap to place table"
            )
        }
        if (!_uiState.value.isArMode) {
            placeTableManually()
        }
    }

    private fun placeTableManually() {
        val session = arSession ?: return
        val frame = try { session.update() } catch (e: Exception) { null } ?: return
        val cameraPose = frame.camera.pose
        // Place table 1m down and 1.5m in front of the camera
        val tablePose = cameraPose.compose(Pose.makeTranslation(0f, -1.0f, -1.5f))
        val newAnchor = session.createAnchor(tablePose)
        _uiState.update {
            it.copy(
                table = newAnchor,
                instructionText = "Manual Mode: Tap on table to place balls"
            )
        }
    }
}
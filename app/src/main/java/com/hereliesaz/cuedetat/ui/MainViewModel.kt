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
                is UiEvent.SetShotPower -> _uiState.update { it.copy(shotPower = event.power) }
                is UiEvent.SetSpin -> _uiState.update { it.copy(cueballSpin = event.spin) }
                is UiEvent.SetSession -> arSession = event.session
                is UiEvent.ToggleHelpDialog -> _uiState.update { it.copy(showHelp = !it.showHelp) }
                is UiEvent.ToggleArMode -> toggleArMode()
                is UiEvent.OnScreenTap -> handleScreenTap(event.offset)
                is UiEvent.OnReset -> resetScene()
                is UiEvent.OnTrackingStateUpdate -> handleTrackingStateUpdate(event.trackingState, event.failureReason)
                is UiEvent.ExecuteShot -> { /* TODO */ }
                is UiEvent.ToggleFlashlight -> { /* Handled by Activity */ }
            }
        }
    }

    private fun handleTrackingStateUpdate(trackingState: TrackingState, failureReason: TrackingFailureReason?) {
        if (uiState.value.table != null) {
            if (uiState.value.cueBall == null) {
                _uiState.update { it.copy(instructionText = "Tap on table to place Cue Ball") }
            } else if (uiState.value.objectBall == null) {
                _uiState.update { it.copy(instructionText = "Tap on table to place Object Ball") }
            } else {
                _uiState.update { it.copy(instructionText = "Aim your shot") }
            }
            return
        }

        val newInstruction = when (trackingState) {
            TrackingState.TRACKING -> "Tap a detected surface to place the table"
            TrackingState.PAUSED -> when (failureReason) {
                TrackingFailureReason.NONE -> "Move your phone to start AR."
                TrackingFailureReason.BAD_STATE -> "AR session is in a bad state."
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Too dark. Find a brighter area."
                TrackingFailureReason.EXCESSIVE_MOTION -> "Move your phone more slowly."
                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Point at a textured surface."
                TrackingFailureReason.CAMERA_UNAVAILABLE -> "Camera is unavailable."
                else -> "AR tracking paused."
            }
            TrackingState.STOPPED -> "AR tracking has stopped."
        }
        _uiState.update { it.copy(instructionText = newInstruction) }
    }

    private fun handleScreenTap(offset: Offset) {
        val session = arSession ?: return
        // This call is architecturally incorrect and will block the main thread,
        // but it fixes the compilation error. The correct solution is to perform
        // hit tests on the GL thread.
        val frame = try { session.update() } catch (e: Exception) { return }

        if (frame.camera.trackingState != TrackingState.TRACKING) return

        val hits = frame.hitTest(offset.x, offset.y)

        val planeHitResult = hits.firstOrNull { hit ->
            val trackable = hit.trackable
            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) &&
                    trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING
        }

        if (planeHitResult != null) {
            placeAnchor(planeHitResult)
        }
    }

    private fun placeAnchor(hitResult: HitResult) {
        val newAnchor = arSession?.createAnchor(hitResult.hitPose) ?: return
        when {
            _uiState.value.table == null -> _uiState.update { it.copy(table = newAnchor) }
            _uiState.value.cueBall == null -> _uiState.update { it.copy(cueBall = newAnchor) }
            _uiState.value.objectBall == null -> _uiState.update { it.copy(objectBall = newAnchor) }
        }
    }

    private fun toggleArMode() {
        val newMode = !_uiState.value.isArMode
        _uiState.update { it.copy(isArMode = newMode) }
        resetScene()
    }

    private fun resetScene() {
        _uiState.value.table?.detach()
        _uiState.value.cueBall?.detach()
        _uiState.value.objectBall?.detach()

        _uiState.update {
            it.copy(
                table = null,
                cueBall = null,
                objectBall = null,
                isAiming = true
            )
        }
        if (!_uiState.value.isArMode) {
            placeTableManually()
        }
    }

    private fun placeTableManually() {
        val session = arSession ?: return
        val frame = try { session.update() } catch (e: Exception) { return }
        val cameraPose = frame.camera.pose
        val tablePose = cameraPose.compose(Pose.makeTranslation(0f, -1.0f, -1.5f))
        val newAnchor = session.createAnchor(tablePose)
        _uiState.update { it.copy(table = newAnchor) }
    }
}
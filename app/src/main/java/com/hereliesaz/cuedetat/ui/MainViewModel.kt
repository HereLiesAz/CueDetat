package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult // Make sure this is the correct import
import com.google.ar.core.Plane
import com.google.ar.core.Pose
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
// import androidx.xr.arcore.HitResult // REMOVE THIS incorrect import if it exists

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
                is UiEvent.ToggleArMode -> toggleArMode()
                is UiEvent.ToggleFlashlight -> { /* Handled by Activity */ }
            }
        }
    }

    private fun toggleArMode() {
        val newMode = !_uiState.value.isArMode
        _uiState.update { it.copy(isArMode = newMode) }

        if (!newMode && _uiState.value.table == null) {
            placeTableManually()
        } else if (newMode) {
            resetScene()
        }
    }

    private fun placeTableManually() {
        val session = arSession ?: return
        val frame = try { session.update() } catch (e: Exception) { null } ?: return
        val cameraPose = frame.camera.pose
        val tablePose = cameraPose.compose(Pose.makeTranslation(0f, -1.0f, -1.5f))

        val newAnchor = session.createAnchor(tablePose)
        _uiState.update {
            it.copy(
                table = newAnchor,
                instructionText = "Manual Mode: Tap to place balls"
            )
        }
    }

    private fun setSession(session: Session?) { this.arSession = session }

    private fun handleScreenTap(offset: Offset) {
        val session = arSession ?: return
        val frame = try { session.update() } catch (e: Exception) { null } ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING && _uiState.value.isArMode) return

        // Use a different logic path for manual vs AR mode
        if (!_uiState.value.isArMode) {
            handleManualTap()
        } else {
            handleArTap(offset, frame)
        }
    }

    private fun handleArTap(offset: Offset, frame: Frame) {
        val hitResults = frame.hitTest(offset.x, offset.y)
        val hitResult = hitResults.firstOrNull {
            val trackable = it.trackable
            (trackable is Plane && trackable.isPoseInPolygon(it.hitPose))
        } ?: return

        when {
            _uiState.value.table == null -> handleTablePlacement(hitResult)
            _uiState.value.cueBall == null -> handleBallPlacement(hitResult, isCueBall = true)
            _uiState.value.objectBall == null -> handleBallPlacement(hitResult, isCueBall = false)
        }
    }

    private fun handleManualTap() {
        val session = arSession ?: return
        val tablePose = _uiState.value.table?.pose ?: return

        // In manual mode, we don't have a real surface, so we place the ball on the virtual table's plane.
        // For simplicity, we'll place it near the center. More advanced logic could use raycasting.
        val ballPose = tablePose.compose(Pose.makeTranslation(0f, 0.05f, 0f)) // Slightly above the table anchor
        val ballAnchor = session.createAnchor(ballPose)

        // Since we don't have a real HitResult, we can just update the state directly.
        if (_uiState.value.cueBall == null) {
            _uiState.update { it.copy(cueBall = ballAnchor, instructionText = "Tap to place Object Ball") }
        } else if (_uiState.value.objectBall == null) {
            _uiState.update { it.copy(objectBall = ballAnchor, instructionText = "Aim your shot") }
        }
    }

    private fun handleTablePlacement(hitResult: HitResult) {
        if (hitResult.trackable.trackingState == TrackingState.TRACKING) {
            val newAnchor = arSession?.createAnchor(hitResult.hitPose)
            _uiState.update {
                it.copy(
                    table = newAnchor,
                    instructionText = "Tap on table to place Cue Ball"
                )
            }
        }
    }

    private fun handleBallPlacement(hitResult: HitResult, isCueBall: Boolean) {
        if (hitResult.trackable.trackingState == TrackingState.TRACKING) {
            val newAnchor = arSession?.createAnchor(hitResult.hitPose)
            if (isCueBall) {
                _uiState.update {
                    it.copy(
                        cueBall = newAnchor,
                        instructionText = "Tap on table to place Object Ball"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        objectBall = newAnchor,
                        instructionText = "Aim your shot"
                    )
                }
            }
        }
    }

    private fun resetScene() {
        _uiState.value.table?.detach()
        _uiState.value.cueBall?.detach()
        _uiState.value.objectBall?.detach()

        val isAr = _uiState.value.isArMode
        _uiState.update {
            it.copy(
                table = null,
                cueBall = null,
                objectBall = null,
                isAiming = true,
                instructionText = if (isAr) "Move phone to find a surface..." else "Manual Mode"
            )
        }
        if (!isAr) {
            placeTableManually()
        }
    }

    private fun toggleHelpDialog() {
        val show = !_uiState.value.showHelp
        _uiState.update { it.copy(showHelp = show) }
        viewModelScope.launch {
            userPreferenceRepository.saveShowHelp(show)
        }
    }

    private fun setShotPower(power: Float) { _uiState.update { it.copy(shotPower = power) } }

    private fun setSpin(spin: Offset) { _uiState.update { it.copy(cueballSpin = spin) } }

    private fun executeShot() {
        if (_uiState.value.table == null) return
        _uiState.update { it.copy(isAiming = false) }
    }
}
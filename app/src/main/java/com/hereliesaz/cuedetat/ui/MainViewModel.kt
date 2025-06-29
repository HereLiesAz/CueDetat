package com.hereliesaz.cuedetat.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ar.ArConstants
import com.hereliesaz.cuedetat.ar.toF3
import com.hereliesaz.cuedetat.ui.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.math.toTransform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    private val warnings: Array<String> = context.resources.getStringArray(R.array.sarcastic_warnings)

    fun onEvent(event: UiEvent) {
        if (_uiState.value.warningMessage != null) {
            _uiState.update { it.copy(warningMessage = null) }
        }
        when (event) {
            is UiEvent.OnTablePlaceTapped -> placeTable()
            is UiEvent.OnPlaneTapped -> handlePlaneTap(event.pose)
            is UiEvent.OnBallTapped -> handleBallTap(event.ballId)
            is UiEvent.OnReset -> reset()
            is UiEvent.ToggleHelpDialog -> _uiState.update { it.copy(showHelpDialog = !it.showHelpDialog) }
            is UiEvent.SetShotType -> setShotType(event.type)
            is UiEvent.SetCueElevation -> _uiState.update { it.copy(cueElevation = event.elevation) }
            is UiEvent.SetSpin -> _uiState.update { it.copy(spinOffset = event.offset.toF3()) }
            is UiEvent.SetARSession -> _uiState.update { it.copy(arSession = event.session) }
            is UiEvent.OnPlanesDetected -> {
                if (_uiState.value.appState == AppState.DetectingPlanes) {
                    _uiState.update { it.copy(appState = AppState.ReadyToPlace, statusText = getStatusText(AppState.ReadyToPlace)) }
                }
            }
        }
    }

    private fun placeTable() {
        val cameraPose = uiState.value.arSession?.camera?.pose ?: return
        val forwardVector = cameraPose.forward.toF3() * 1.5f
        val tablePosition = cameraPose.translation.toF3() + forwardVector
        val tablePose = Pose(tablePosition.toFloatArray(), cameraPose.rotation)

        _uiState.update { it.copy(appState = AppState.ScenePlaced, tablePose = tablePose, statusText = getStatusText(AppState.ScenePlaced)) }
    }

    private fun handlePlaneTap(pose: Pose) {
        val currentState = _uiState.value
        val localPos = getLocalPosition(pose, currentState.tablePose) ?: return
        if (!isWithinTableBounds(localPos)) return

        val newBallPose = Pose(localPos.toFloatArray(), pose.rotation)

        if (currentState.cueBallPose == null) {
            _uiState.update { it.copy(cueBallPose = BallState(newBallPose)) }
        } else if (currentState.objectBallPose == null) {
            _uiState.update { it.copy(objectBallPose = BallState(newBallPose)) }
        } else {
            // Logic to move a selected ball can be added here
            val selectedId = currentState.selectedBall
            if (selectedId == 0) {
                _uiState.update { it.copy(cueBallPose = BallState(newBallPose), selectedBall = null) }
            } else if (selectedId == 1) {
                _uiState.update { it.copy(objectBallPose = BallState(newBallPose), selectedBall = null) }
            }
        }
        updateStatusText()
    }

    private fun handleBallTap(ballId: Int) {
        val currentSelected = _uiState.value.selectedBall
        // Toggle selection off if tapping the same ball
        if (currentSelected == ballId) {
            _uiState.update { it.copy(selectedBall = null) }
        } else {
            _uiState.update { it.copy(selectedBall = ballId, statusText = "${if (ballId == 0) "Cue" else "Object"} Ball Selected. Tap table to move.") }
        }
    }

    private fun setShotType(type: ShotType) {
        _uiState.update { it.copy(shotType = type, statusText = getStatusText(it.appState, type)) }
    }

    private fun reset() {
        _uiState.update { MainUiState(arSession = it.arSession, appState = AppState.ReadyToPlace, statusText = getStatusText(AppState.ReadyToPlace)) }
    }

    private fun getLocalPosition(hitPose: Pose, tablePose: Pose?): Float3? {
        if (tablePose == null) return null
        val inverseTableMatrix = inverse(tablePose.toTransform())
        val hitPosition = hitPose.translation.toF3()
        val localPosition = inverseTableMatrix * hitPosition
        return localPosition.copy(y = 0f)
    }

    private fun isWithinTableBounds(localPosition: Float3): Boolean {
        val halfWidth = ArConstants.TABLE_WIDTH / 2f
        val halfDepth = ArConstants.TABLE_DEPTH / 2f
        return localPosition.x in -halfWidth..halfWidth && localPosition.z in -halfDepth..halfDepth
    }

    private fun getStatusText(appState: AppState, shotType: ShotType = ShotType.CUT): String {
        return when (appState) {
            AppState.DetectingPlanes -> "Move phone to detect a surface"
            AppState.ReadyToPlace -> "Surface Detected. Tap button to place table."
            AppState.ScenePlaced -> when {
                uiState.value.cueBallPose == null -> "Tap on table to place cue ball"
                uiState.value.objectBallPose == null -> "Tap to place object ball"
                else -> "All set. Ready to visualize shot."
            }
        }
    }

    fun updateStatusText() {
        _uiState.update { it.copy(statusText = getStatusText(it.appState, it.shotType)) }
    }

    // Helper extensions
    private val Pose.forward: FloatArray get() = floatArrayOf(0f, 0f, -1f).apply { this@forward.rotateVector(this, 0) }
    private fun Offset.toF3() = Float3(this.x, this.y, 0f)
}

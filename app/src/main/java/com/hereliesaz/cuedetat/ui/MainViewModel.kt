package com.hereliesaz.cuedetat.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ar.MathUtils.toF3
import com.hereliesaz.cuedetat.ui.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.math.Position
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
            is UiEvent.SetSpin -> _uiState.update { it.copy(spinOffset = event.offset) }
        }
    }

    private fun placeTable() {
        val cameraPose = uiState.value.arSession?.camera?.pose ?: return
        val tablePose = cameraPose.copy(
            translation = cameraPose.translation + cameraPose.forward * 1.5f
        )
        _uiState.update { it.copy(appState = AppState.ScenePlaced, tablePose = tablePose, statusText = getStatusText(AppState.ScenePlaced)) }
    }

    private fun handlePlaneTap(pose: Pose) {
        val currentState = _uiState.value
        val localPos = getLocalPosition(pose, currentState.tablePose)
        if (!isWithinTableBounds(localPos)) return

        if (currentState.cueBallPose == null) {
            _uiState.update { it.copy(cueBallPose = BallState(Pose(localPos))) }
        } else if (currentState.objectBallPose == null) {
            _uiState.update { it.copy(objectBallPose = BallState(Pose(localPos))) }
        } else {
            _uiState.update { it.copy(selectedBall = null) }
        }
        updateStatusText()
    }

    private fun handleBallTap(ballId: Int) {
        _uiState.update { it.copy(selectedBall = ballId, statusText = "${if(ballId == 0) "Cue" else "Object"} Ball Selected. Tap table to move.") }
    }

    private fun setShotType(type: ShotType) {
        _uiState.update { it.copy(shotType = type, statusText = getStatusText(it.appState, type)) }
    }

    private fun reset() {
        _uiState.update { MainUiState(appState = AppState.ReadyToPlace, statusText = getStatusText(AppState.ReadyToPlace)) }
    }

    private fun getLocalPosition(hitPose: Pose, tablePose: Pose?): FloatArray {
        if (tablePose == null) return hitPose.translation
        val tableMatrix = tablePose.matrix
        val inverseTableMatrix = tableMatrix.clone().apply { android.opengl.Matrix.invertM(this, 0) }
        val hitVector = floatArrayOf(hitPose.translation[0], hitPose.translation[1], hitPose.translation[2], 1f)
        val localVector = FloatArray(4)
        android.opengl.Matrix.multiplyMV(localVector, 0, inverseTableMatrix, 0, hitVector, 0)
        return floatArrayOf(localVector[0], 0f, localVector[2])
    }

    private fun isWithinTableBounds(localPosition: FloatArray): Boolean {
        val halfWidth = ARConstants.TABLE_WIDTH / 2f
        val halfDepth = ARConstants.TABLE_DEPTH / 2f
        return localPosition[0] in -halfWidth..halfWidth && localPosition[2] in -halfDepth..halfDepth
    }

    private fun getStatusText(appState: AppState, shotType: ShotType = ShotType.CUT): String {
        return when (appState) {
            AppState.DetectingPlanes -> "Move phone to detect a surface"
            AppState.ReadyToPlace -> "Surface Detected. Tap button to place table."
            AppState.ScenePlaced -> when(shotType) {
                ShotType.CUT, ShotType.BANK, ShotType.KICK -> "Tap on table to place balls."
                ShotType.JUMP, ShotType.MASSE -> "Use spatial controls to adjust shot."
            }
        }
    }

    fun updateStatusText() {
        _uiState.update { it.copy(statusText = getStatusText(it.appState, it.shotType)) }
    }
}
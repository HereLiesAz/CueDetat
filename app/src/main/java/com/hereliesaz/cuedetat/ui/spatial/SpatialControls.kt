package com.hereliesaz.cuedetat.ui.spatial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.Button
import androidx.xr.compose.material3.ButtonDefaults
import androidx.xr.compose.material3.Card
import androidx.xr.compose.material3.RangeSlider
import androidx.xr.compose.material3.Text
import androidx.xr.compose.material3.rememberMaterial
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.Rail
import com.hereliesaz.cuedetat.ui.ShotType
import com.hereliesaz.cuedetat.ui.UiEvent
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import dev.romainguy.kotlin.math.Float3

@Composable
fun SpatialControls(tablePose: Pose, viewModel: MainViewModel) {
    val uiState = viewModel.uiState.collectAsState().value

    // Main container rail for all controls, positioned to the right of the table
    val controlRailPose = tablePose.copy(
        translation = tablePose.translation + floatArrayOf(ARConstants.TABLE_WIDTH / 2f + 0.35f, 0.4f, 0f)
    )

    Card(pose = controlRailPose, size = Triple(0.3.dp, 0.9.dp, 0.05.dp)) {
        // --- Shot Type Selection Buttons ---
        Text("Shot Type", modifier = Modifier.padding(8.dp), pose = Pose(Float3(0f, 0.4f, 0.06f)))
        ShotTypeButton(text = "Cut", pose = Pose(Float3(0f, 0.3f, 0.06f)), isSelected = uiState.shotType == ShotType.CUT) { viewModel.onEvent(UiEvent.SetShotType(ShotType.CUT)) }
        ShotTypeButton(text = "Bank", pose = Pose(Float3(0f, 0.2f, 0.06f)), isSelected = uiState.shotType == ShotType.BANK) { viewModel.onEvent(UiEvent.SetShotType(ShotType.BANK)) }
        ShotTypeButton(text = "Kick", pose = Pose(Float3(0f, 0.1f, 0.06f)), isSelected = uiState.shotType == ShotType.KICK) { viewModel.onEvent(UiEvent.SetShotType(ShotType.KICK)) }
        ShotTypeButton(text = "Jump", pose = Pose(Float3(0f, 0.0f, 0.06f)), isSelected = uiState.shotType == ShotType.JUMP) { viewModel.onEvent(UiEvent.SetShotType(ShotType.JUMP)) }
        ShotTypeButton(text = "Masse", pose = Pose(Float3(0f, -0.1f, 0.06f)), isSelected = uiState.shotType == ShotType.MASSE) { viewModel.onEvent(UiEvent.SetShotType(ShotType.MASSE)) }

        // --- Contextual Controls Based on Shot Type ---
        when (uiState.shotType) {
            ShotType.BANK, ShotType.KICK -> {
                SpatialRailSelector(
                    pose = Pose(Float3(0f, -0.25f, 0.06f)),
                    selectedRail = uiState.selectedRail,
                    onRailSelected = { viewModel.onEvent(UiEvent.SetRail(it)) }
                )
            }
            ShotType.JUMP, ShotType.MASSE -> {
                Text("Elevation", pose = Pose(Float3(0f, -0.25f, 0.06f)))
                SpatialSlider(
                    pose = Pose(Float3(0f, -0.35f, 0.06f)),
                    value = uiState.cueElevation,
                    onValueChange = { viewModel.onEvent(UiEvent.SetCueElevation(it)) }
                )
            }
            ShotType.CUT -> {
                // The 2D spin control will be shown on the screen overlay, not here.
            }
        }
    }
}

@Composable
fun ShotTypeButton(text: String, onClick: () -> Unit, pose: Pose, isSelected: Boolean) {
    val selectedMaterial = rememberMaterial(color = AccentGold)
    val defaultMaterial = rememberMaterial(color = Color.DarkGray)
    Button(
        onClick = onClick,
        pose = pose,
        size = Triple(0.25.dp, 0.08.dp, 0.04.dp),
        material = if (isSelected) selectedMaterial else defaultMaterial
    ) {
        Text(text)
    }
}

@Composable
fun SpatialRailSelector(pose: Pose, selectedRail: Rail, onRailSelected: (Rail) -> Unit) {
    // A group of 4 buttons arranged in a diamond pattern
    val offset = 0.06f
    Button(onClick = { onRailSelected(Rail.TOP) }, pose = pose.copy(translation = pose.translation + floatArrayOf(0f, offset, 0f)), size = Triple(0.05.dp, 0.05.dp, 0.02.dp)) { Text("T") }
    Button(onClick = { onRailSelected(Rail.BOTTOM) }, pose = pose.copy(translation = pose.translation + floatArrayOf(0f, -offset, 0f)), size = Triple(0.05.dp, 0.05.dp, 0.02.dp)) { Text("B") }
    Button(onClick = { onRailSelected(Rail.LEFT) }, pose = pose.copy(translation = pose.translation + floatArrayOf(-offset, 0f, 0f)), size = Triple(0.05.dp, 0.05.dp, 0.02.dp)) { Text("L") }
    Button(onClick = { onRailSelected(Rail.RIGHT) }, pose = pose.copy(translation = pose.translation + floatArrayOf(offset, 0f, 0f)), size = Triple(0.05.dp, 0.05.dp, 0.02.dp)) { Text("R") }
}

@Composable
fun SpatialSlider(pose: Pose, value: Float, onValueChange: (Float) -> Unit) {
    RangeSlider(
        pose = pose,
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f, // Normalized value for elevation/power
        size = Pair(0.25.dp, 0.05.dp)
    )
}
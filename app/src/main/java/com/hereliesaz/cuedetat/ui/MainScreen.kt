package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.icons.filled.Refresh
import androidx.compose.material3.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.xr.compose.Scene
import androidx.xr.compose.frame.FrameState
import androidx.xr.compose.material3.Button
import androidx.xr.compose.material3.Orbiter
import androidx.xr.compose.material3.OrbiterDefaults
import androidx.xr.compose.material3.Panel
import androidx.xr.compose.spatial.rememberSpatialLookAtNode
import androidx.xr.compose.spatial.rememberSpatialPositionNode
import androidx.xr.compose.spatial.simulation.rememberSimulatedHeadNode
import androidx.xr.runtime.trackable.Plane
import com.hereliesaz.cuedetat.ar.scene.Table
import com.hereliesaz.cuedetat.ui.composables.HelpDialog
import com.hereliesaz.cuedetat.ui.composables.ShotControls
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    uiState: UiState
) {
    val head = rememberSimulatedHeadNode()

    // The Scene composable is the root for all spatial content.
    // It provides the FrameState needed for hit-testing.
    Scene(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                // The hit-test is now performed in the UI layer.
                val frameState: FrameState? = this.coroutineContext[FrameState]
                if (frameState != null) {
                    val hitResults = frameState.hitTest(offset)
                    val planeHit = hitResults.firstOrNull {
                        it.trackable is Plane && (it.trackable as Plane).type == Plane.Type.HORIZONTAL_UPWARD_FACING
                    }
                    if (planeHit != null) {
                        viewModel.onEvent(UiEvent.OnHitResult(planeHit))
                    }
                }
            }
        }
    ) {
        // The head node is used to position UI relative to the user.
        head.position.set(0f, 1.6f, 0f)

        // Render the 3D table entity if it exists in the state
        if (uiState.table != null) {
            Table(entity = uiState.table)
        }

        // The main UI is now a Panel, which is the correct component for hosting 2D UI in 3D space.
        Panel(
            modifier = Modifier.then(rememberSpatialPositionNode(head.position + OrbiterDefaults.PanelOffset))
                .then(rememberSpatialLookAtNode(head.position))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    ShotControls(
                        shotPower = uiState.shotPower,
                        spin = uiState.cueballSpin,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }

        // The Orbiter provides contextual actions.
        Orbiter(
            headNode = head,
            radius = 1.0f
        ) {
            Button(onClick = { viewModel.onEvent(UiEvent.OnReset) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Scene")
            }
            Button(onClick = { viewModel.onEvent(UiEvent.ToggleHelp) }) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = "Help")
            }
        }

        // Show the help dialog as a separate, modal-like panel when toggled
        if (uiState.showHelp) {
            Panel(
                modifier = Modifier.then(rememberSpatialPositionNode(head.position + OrbiterDefaults.PanelOffset * 0.8f))
                    .then(rememberSpatialLookAtNode(head.position))
            ) {
                HelpDialog(onDismiss = { viewModel.onEvent(UiEvent.ToggleHelp) })
            }
        }
    }
}

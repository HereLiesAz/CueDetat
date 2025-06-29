package com.hereliesaz.cuedetat.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.ar.ArConstants
import com.hereliesaz.cuedetat.ar.renderables.ShotVisualization
import com.hereliesaz.cuedetat.ar.rendering.BallNode
import com.hereliesaz.cuedetat.ar.rendering.LineNode
import com.hereliesaz.cuedetat.ar.rendering.TableNode
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import com.hereliesaz.cuedetat.ui.theme.CueDetatARTheme
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.getUpdatedPlanes

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    MenuDrawer(
        uiState = uiState,
        onEvent = viewModel::onEvent
    ) {
        MainScreenContent(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
fun MainScreenContent(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL,
            onSessionCreated = { session ->
                onEvent(UiEvent.SetARSession(session))
            },
            onSessionFailed = { exception ->
                Log.e("MainScreen", "AR Session failed", exception)
            },
            onFrame = { frame ->
                if (uiState.tablePose == null && frame.getUpdatedPlanes().any { it.trackingState == TrackingState.TRACKING }) {
                    onEvent(UiEvent.OnPlanesDetected)
                }
            },
            onTap = { hitResult ->
                if(hitResult.trackable is com.google.ar.core.Plane){
                    onEvent(UiEvent.OnPlaneTapped(hitResult.pose))
                }
            }
        ) {
            uiState.tablePose?.let { tablePose ->
                TableNode(pose = tablePose)

                val cueState = uiState.cueBallPose
                val objState = uiState.objectBallPose

                if (cueState != null) {
                    BallNode(
                        id = 0,
                        pose = cueState.pose,
                        isSelected = uiState.selectedBall == 0,
                        onBallTapped = { onEvent(UiEvent.OnBallTapped(it)) },
                        color = Color(ArConstants.CUE_BALL_COLOR[0], ArConstants.CUE_BALL_COLOR[1], ArConstants.CUE_BALL_COLOR[2])
                    )
                }

                if (objState != null) {
                    BallNode(
                        id = 1,
                        pose = objState.pose,
                        isSelected = uiState.selectedBall == 1,
                        onBallTapped = { onEvent(UiEvent.OnBallTapped(it)) },
                        color = Color(ArConstants.OBJECT_BALL_COLOR[0], ArConstants.OBJECT_BALL_COLOR[1], ArConstants.OBJECT_BALL_COLOR[2])
                    )
                }

                if (cueState != null && objState != null) {
                    val shotData = ShotVisualization.calculateCutShot(cueState.pose, objState.pose)

                    shotData.ghostBallPose?.let { ghostPose ->
                        BallNode(id = -1, pose = ghostPose, isSelected = false, onBallTapped = {},
                            color = Color(ArConstants.GHOST_BALL_COLOR[0], ArConstants.GHOST_BALL_COLOR[1], ArConstants.GHOST_BALL_COLOR[2], ArConstants.GHOST_BALL_COLOR[3])
                        )
                    }
                    if(shotData.pocketingLinePoints.size == 2) {
                        LineNode(start = shotData.pocketingLinePoints[0], end = shotData.pocketingLinePoints[1], color = Color.Green)
                    }
                    if(shotData.tangentLinePoints.size == 2) {
                        LineNode(start = shotData.tangentLinePoints[0], end = shotData.tangentLinePoints[1], color = Color.Blue)
                    }
                    if(shotData.cueBallPathPoints.size == 2) {
                        LineNode(start = shotData.cueBallPathPoints[0], end = shotData.cueBallPathPoints[1], color = Color.Yellow)
                    }
                }
            }
        }
        Text(text = uiState.statusText, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    CueDetatARTheme {
        Box(modifier = Modifier.fillMaxSize()){
            Text("AR View Preview not available", modifier = Modifier.align(Alignment.Center))
        }
    }
}

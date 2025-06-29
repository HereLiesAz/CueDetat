package com.hereliesaz.cuedetat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.xr.compose.ar.*
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ar.renderables.Ball
import com.hereliesaz.cuedetat.ar.renderables.ShotVisualization
import com.hereliesaz.cuedetat.ar.renderables.Table
import com.hereliesaz.cuedetat.ui.composables.HelpDialog
import com.hereliesaz.cuedetat.ui.composables.MenuDrawerContent
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.spatial.SpatialControls
import com.hereliesaz.cuedetat.ui.state.*
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    if (uiState.showHelpDialog) {
        HelpDialog(onDismiss = { viewModel.onEvent(UiEvent.ToggleHelpDialog) })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { MenuDrawerContent(viewModel = viewModel, onCloseDrawer = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(uiState.statusText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Open Menu") } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                ARContainer(
                    modifier = Modifier.fillMaxSize(),
                    onSessionCreate = { session ->
                        session.configure(session.config.apply {
                            depthMode = Config.DepthMode.AUTOMATIC
                            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        })
                        viewModel.onARSessionCreated(session)
                    }
                ) {
                    val planeManager = rememberARPlaneManager()
                    val trackableManager = rememberARTrackableManager<Int>()
                    val camera = rememberARCamera()

                    if (uiState.appState < AppState.ScenePlaced) {
                        planeManager.TrackPlanes(onUpdated = { planes -> if (uiState.appState == AppState.DetectingPlanes) viewModel.onPlaneDetectionChanged(planes.isNotEmpty()) })
                    }

                    uiState.tablePose?.let { tablePose ->
                        Table(
                            pose = tablePose,
                            onPlaneTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onEvent(UiEvent.OnPlaneTapped(it))
                            }
                        )

                        val cueBallState = uiState.cueBall
                        val objectBallState = uiState.objectBall

                        cueBallState?.let { Ball(pose = it.pose, color = Color.White, id = 0, trackableManager = trackableManager, onBallTapped = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.onEvent(UiEvent.OnBallTapped(0)) }, isSelected = uiState.selectedBall == 0) }
                        objectBallState?.let { Ball(pose = it.pose, color = Color.Yellow, id = 1, trackableManager = trackableManager, onBallTapped = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.onEvent(UiEvent.OnBallTapped(1)) }, isSelected = uiState.selectedBall == 1) }

                        if (cueBallState != null && objectBallState != null) {
                            ShotVisualization(uiState = uiState)
                        }

                        if(uiState.appState == AppState.ScenePlaced) {
                            SpatialControls(tablePose = tablePose, viewModel = viewModel)
                        }
                    }

                    trackableManager.onTrackableDrag(
                        onDragStart = { trackable, _ -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onEvent(UiEvent.OnDragStart(trackable.id)) },
                        onDrag = { _, hitResult -> viewModel.onEvent(UiEvent.OnDrag(hitResult.pose)) },
                        onDragEnd = { _, _ -> viewModel.onEvent(UiEvent.OnDragEnd) }
                    )
                }

                if (uiState.appState == AppState.ReadyToPlace) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { viewModel.onEvent(UiEvent.OnTablePlaceTapped) }) { Text("Place Table Here") }
                    }
                }

                AnimatedVisibility(visible = uiState.warningMessage != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                    Text(text = uiState.warningMessage ?: "", color = WarningRed, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.medium).padding(24.dp))
                }

                if (uiState.appState == AppState.ScenePlaced && uiState.shotType == ShotType.CUT) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                        SpinControl(onSpinChanged = { viewModel.onEvent(UiEvent.SetSpin(it)) })
                    }
                }
            }
        }
    }
}

@Composable
fun ARContainer(
    modifier: Modifier,
    onSessionCreate: (ERROR) -> onARSessionCreated,
    content: @Composable () -> onTrackableDrag
) {
    TODO("Not yet implemented")
}
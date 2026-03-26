package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHostScope
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit

@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
    hasTableModel: Boolean = false,
    content: AzNavHostScope.() -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val b1Y = Color(0xFFFFEB3B); val b2B = Color(0xFF2196F3); val b3R = Color(0xFFF44336)
    val b4P = Color(0xFF9C27B0); val b5O = Color(0xFFFF9800); val b6G = Color(0xFF4CAF50)
    val b7M = Color(0xFFE91E63); val b8K = Color(0xFF212121); val b9Y = Color(0xFFFFF59D)
    val b10B = Color(0xFF64B5F6); val b11R = Color(0xFFE57373); val b12P = Color(0xFFBA68C8)
    val b13O = Color(0xFFFFB74D); val b14G = Color(0xFF81C784); val b15M = Color(0xFFF06292)

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier,
        currentDestination = currentDestination,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        if (uiState.areHelpersVisible) {
            onscreen(alignment = Alignment.TopStart) {
                Text(
                    text = "Tap the icon for more",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }

        if (uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_CUE) {
            onscreen(alignment = Alignment.BottomCenter) {
                Text(
                    text = "Tap the cue ball",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        } else if (uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_TARGET) {
            onscreen(alignment = Alignment.BottomCenter) {
                Text(
                    text = "Tap the target ball",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }

        background(weight = 0) {
            val cueBallPos = uiState.onPlaneBall?.center
            if (cueBallPos != null && !uiState.spinPaths.isNullOrEmpty() && !uiState.isMasseModeActive) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.spinPaths.forEach { (color, points) ->
                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(cueBallPos.x + points[0].x, cueBallPos.y + points[0].y)
                                points.forEach { lineTo(cueBallPos.x + it.x, cueBallPos.y + it.y) }
                            }
                            drawPath(path = path, color = color.copy(alpha = uiState.spinPathsAlpha), style = Stroke(width = 4f))
                        }
                    }
                }
            }
        }

        azConfig(dockingSide = AzDockingSide.LEFT, packButtons = false, showFooter = true)
        azTheme(defaultShape = AzButtonShape.CIRCLE, activeColor = Color.White)
        azAdvanced(isLoading = false, helpEnabled = true, onDismissHelp = {})

        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItem(id = "shake", text = "Shake", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.Shake) })
            azRailItem(id = "exit", text = "Exit", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ExitToSplash) })
            content(); return@AzHostActivityLayout
        }

        azRailToggle(id = "help", isChecked = uiState.areHelpersVisible, toggleOnText = "Help", toggleOffText = "Help", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleHelp) })
        azMenuItem(id = "tutorial", text = "Tutorial", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.StartTutorial) })

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            val isArActive = uiState.cameraMode != CameraMode.OFF
            azRailToggle(
                id = "ar",
                isChecked = isArActive,
                toggleOnText = "AR", toggleOffText = "AR",
                fillColor = b3R, textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.CycleCameraMode) }
            )

            val inArSubMode = uiState.cameraMode == CameraMode.AR_SETUP || uiState.cameraMode == CameraMode.AR_ACTIVE
            if (inArSubMode) {
                azRailItem(id = "felt", text = "Felt", fillColor = b11R, textColor = Color.White, onClick = { 
                    onEvent(MainScreenEvent.ClearTableScan)
                    onEvent(MainScreenEvent.ToggleTableScanScreen) 
                })
                azRailItem(id = "cancel_ar", text = "Cancel", fillColor = b12P, textColor = Color.White, onClick = { 
                    onEvent(MainScreenEvent.CancelArSetup) 
                })
            }
        }
        azDivider()

        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(id = "spin", isChecked = uiState.isSpinControlVisible, toggleOnText = "Spin", toggleOffText = "Spin", fillColor = b4P, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleSpinControl) })
            azRailToggle(id = "masse", isChecked = uiState.isMasseModeActive, toggleOnText = "Massé", toggleOffText = "Massé", fillColor = b5O, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleMasseMode) })
        }

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(id = "bank", isChecked = uiState.isBankingMode, toggleOnText = "Aim", toggleOffText = "Bank", fillColor = b6G, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleBankingMode) })
            azRailItem(id = "add_obstacle", text = "Add", fillColor = b7M, textColor = Color.White, onClick = { onEvent(MainScreenEvent.AddObstacleBall) })
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailItem(id = "static", text = "Static", fillColor = b6G, textColor = Color.White, onClick = { onEvent(MainScreenEvent.LockBeginnerView) })
            azRailItem(id = "dynamic", text = "Dynamic", fillColor = b7M, textColor = Color.White, onClick = { onEvent(MainScreenEvent.UnlockBeginnerView) })
        } else {
            val resetLabel = when {
                uiState.obstacleBalls.isNotEmpty() -> "Clear"
                uiState.targetCvAnchor != null -> "Undo"
                uiState.preResetState != null -> "Undo"
                uiState.postResetState != null -> "Redo"
                else -> "Reset"
            }
            azRailItem(id = "reset", text = resetLabel, fillColor = b8K, textColor = Color.White, onClick = { onEvent(MainScreenEvent.Reset) })
        }
        azDivider()

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            if (hasTableModel) {
                azMenuItem(id = "rescan", text = "Recalibrate Felt", fillColor = b12P, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ClearTableScan); onEvent(MainScreenEvent.ToggleTableScanScreen) })
            } else {
                azMenuItem(id = "scan", text = "Felt Capture", fillColor = b11R, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleTableScanScreen) })
            }
        }

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "size", text = "Table Size", fillColor = b13O, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) })
            azMenuItem(id = "units", text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Metric" else "Imperial", fillColor = b14G, textColor = Color.Black, onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) })
            azDivider()
        }

        azMenuItem(id = "orientation", text = "Orientation", fillColor = b15M, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) })

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "advanced", text = "Advanced", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) })
        }

        azDivider()
        azMenuItem(id = "mode", text = "Mode: ${uiState.experienceMode?.name}", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) })

        content()
    }
}
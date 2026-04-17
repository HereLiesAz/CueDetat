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

private fun AzNavHostScope.azRailItemLowerCase(
    id: String,
    text: String,
    fillColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    azRailItem(id = id, text = text.lowercase(), fillColor = fillColor, textColor = textColor, onClick = onClick)
}

@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
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
        // [SECTION 1] Configuration (DSL) - MANDATORY TOP POSITION
        azConfig(dockingSide = AzDockingSide.LEFT, packButtons = false, showFooter = true)
        azTheme(defaultShape = AzButtonShape.CIRCLE, activeColor = Color.White)
        azAdvanced(isLoading = false, helpEnabled = true, onDismissHelp = {})

        // [SECTION 2] Global Onscreen & Background layers (DSL)
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

        // [SECTION 3] Extra content from the caller (ProtractorScreen's Camera & Overlays)
        content()

        // [SECTION 4] Mode-specific Rail Items
        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItemLowerCase(id = "shake", text = "Shake", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.Shake) })
            azRailItemLowerCase(id = "exit", text = "Exit", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ExitToSplash) })
            return@AzHostActivityLayout // Hater Mode doesn't show standard nav
        }

        azRailToggle(id = "help", isChecked = uiState.areHelpersVisible, toggleOnText = "Help", toggleOffText = "Help", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleHelp) })
        azMenuItem(id = "tutorial", text = "Tutorial", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.StartTutorial()) })

        val inArSubMode = uiState.cameraMode == CameraMode.AR_SETUP || 
                          uiState.cameraMode == CameraMode.AR_ACTIVE || 
                          uiState.cameraMode == CameraMode.LITE_AR
        
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            val isArActive = uiState.cameraMode != CameraMode.OFF
            azRailToggle(
                id = "ar",
                isChecked = isArActive,
                toggleOnText = "off", toggleOffText = "AR",
                fillColor = b3R, textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.CycleCameraMode) }
            )

            if (inArSubMode) {
                azRailItemLowerCase(id = "felt", text = if (uiState.cameraMode == CameraMode.AR_ACTIVE) "re-scan" else "felt", fillColor = b11R, textColor = Color.White, onClick = {
                    onEvent(MainScreenEvent.ToggleTableScanScreen)
                })
                
                azRailToggle(
                    id = "target_type",
                    isChecked = uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES,
                    toggleOnText = "stripes", toggleOffText = "solids",
                    fillColor = b9Y, textColor = Color.Black,
                    onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
                )

                if (!uiState.myriadTrajectory.isNullOrEmpty()) {
                    azRailToggle(
                        id = "flow",
                        isChecked = uiState.isFlowPokeEnabled,
                        toggleOnText = "flow", toggleOffText = "flow",
                        fillColor = Color(0xFFE040FB), textColor = Color.White,
                        onClick = { onEvent(MainScreenEvent.ToggleFlowPoke) }
                    )
                }

                if (uiState.pitchMatrix != null || uiState.topDownBitmap != null) {
                    azRailToggle(
                        id = "top_down_view",
                        isChecked = uiState.isTopDownViewActive,
                        toggleOnText = "back", toggleOffText = "view",
                        fillColor = Color.White, textColor = Color.Black,
                        onClick = { 
                            if (uiState.isTopDownViewActive) onEvent(MainScreenEvent.ClearTopDownView)
                            else onEvent(MainScreenEvent.ToggleTopDownView)
                        }
                    )
                }

                azRailItemLowerCase(id = "cancel_ar", text = "Cancel", fillColor = b12P, textColor = Color.White, onClick = {
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
            azRailItemLowerCase(id = "add_obstacle", text = "Add", fillColor = b7M, textColor = Color.White, onClick = { onEvent(MainScreenEvent.AddObstacleBall) })
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "lock_view",
                isChecked = uiState.isBeginnerViewLocked,
                toggleOnText = "Dynamic", toggleOffText = "Static",
                fillColor = if (uiState.isBeginnerViewLocked) b6G else b7M,
                textColor = Color.White,
                onClick = {
                    if (uiState.isBeginnerViewLocked) onEvent(MainScreenEvent.UnlockBeginnerView)
                    else onEvent(MainScreenEvent.LockBeginnerView)
                }
            )
            
            if (inArSubMode) {
                azRailToggle(
                    id = "target_type",
                    isChecked = uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES,
                    toggleOnText = "stripes", toggleOffText = "solids",
                    fillColor = b9Y, textColor = Color.Black,
                    onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
                )

                if (!uiState.myriadTrajectory.isNullOrEmpty()) {
                    azRailToggle(
                        id = "flow",
                        isChecked = uiState.isFlowPokeEnabled,
                        toggleOnText = "flow", toggleOffText = "flow",
                        fillColor = Color(0xFFE040FB), textColor = Color.White,
                        onClick = { onEvent(MainScreenEvent.ToggleFlowPoke) }
                    )
                }

                if (uiState.pitchMatrix != null || uiState.topDownBitmap != null) {
                    azRailToggle(
                        id = "top_down_view",
                        isChecked = uiState.isTopDownViewActive,
                        toggleOnText = "back", toggleOffText = "view",
                        fillColor = Color.White, textColor = Color.Black,
                        onClick = { 
                            if (uiState.isTopDownViewActive) onEvent(MainScreenEvent.ClearTopDownView)
                            else onEvent(MainScreenEvent.ToggleTopDownView)
                        }
                    )
                }
            }
        } else {
            val resetLabel = when {
                uiState.obstacleBalls.isNotEmpty() -> "clear"
                uiState.targetCvAnchor != null -> "undo"
                uiState.preResetState != null -> "undo"
                uiState.postResetState != null -> "redo"
                else -> "reset"
            }
            azRailItemLowerCase(id = "reset", text = resetLabel, fillColor = b8K, textColor = Color.White, onClick = { onEvent(MainScreenEvent.Reset) })
        }
        azDivider()

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
    }
}

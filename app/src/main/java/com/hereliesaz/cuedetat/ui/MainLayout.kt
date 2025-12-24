// app/src/main/java/com/hereliesaz/cuedetat/ui/MainLayout.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.ui.composables.dialogs.AdvancedOptionsDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.ui.composables.overlays.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.overlays.TutorialOverlay
import com.hereliesaz.cuedetat.ui.composables.sliders.TableRotationSlider

@Composable
fun MainLayout(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val safeTopPadding = maxHeight * 0.1f

        // Main content, e.g., the rendering canvas
        // Canvas fills the whole screen, usually.
        content()

        // Wrapper for all UI elements to enforce top 10% safety margin
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safeTopPadding)
        ) {
            // UI Controls layered on top
            TopControls(
                areHelpersVisible = uiState.areHelpersVisible,
                experienceMode = uiState.experienceMode,
                isTableVisible = uiState.table.isVisible,
                tableSizeFeet = uiState.table.size.feet.toFloat(),
                isBeginnerViewLocked = uiState.isBeginnerViewLocked,
                targetBallDistance = uiState.targetBallDistance.toFloat(),
                distanceUnit = uiState.distanceUnit,
                onCycleTableSize = { onEvent(MainScreenEvent.CycleTableSize) },
                onMenuClick = { onEvent(MainScreenEvent.ToggleNavigationRail) }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Left-aligned controls
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // This column is intentionally left empty.
                    // The buttons have been moved to the navigation rail.
                }

                // Center-aligned controls
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TableRotationSlider(
                        isVisible = uiState.table.isVisible,
                        worldRotationDegrees = uiState.worldRotationDegrees,
                        onRotationChange = { onEvent(MainScreenEvent.TableRotationChanged(it)) }
                    )
                }

                // Right-aligned controls
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    if (uiState.isSpinControlVisible && uiState.spinControlCenter != null) {
                        SpinControl(
                            centerPosition = uiState.spinControlCenter!!,
                            selectedSpinOffset = uiState.selectedSpinOffset,
                            lingeringSpinOffset = uiState.lingeringSpinOffset,
                            spinPathAlpha = uiState.spinPathsAlpha,
                            onEvent = onEvent,
                        )
                    }
                }
            }

            ZoomControls(
                zoomSliderPosition = uiState.zoomSliderPosition,
                onZoomChange = { onEvent(MainScreenEvent.ZoomSliderChanged(it)) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.6f)
                    .padding(end = 12.dp)
                    .width(48.dp)
            )

            // Overlays and Dialogs
            KineticWarningOverlay(text = uiState.warningText)
            TutorialOverlay(uiState = uiState, onEvent = onEvent)
            AdvancedOptionsDialog(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
            LuminanceAdjustmentDialog(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
            )

            TableSizeSelectionDialog(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )

            // Expressive navigation rail
            // Note: AzNavRail typically attaches to the side. Padding it down by 10% ensures it's safe.
            AzNavRailMenu(uiState = uiState, onEvent = onEvent)
        }
    }
}

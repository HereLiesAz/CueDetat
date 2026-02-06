// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainLayout.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
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

/**
 * The high-level layout of the main screen.
 *
 * It acts as a container for:
 * 1. The main content (Camera/AR view) passed as a composable lambda.
 * 2. The HUD/UI layer overlaid on top (TopControls, NavRail, SpinControl, Sliders).
 * 3. Dialogs and Overlays.
 *
 * @param uiState Current application state.
 * @param onEvent Event dispatcher.
 * @param content The background content (usually the AR ProtractorScreen).
 */
@Composable
fun MainLayout(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content, e.g., the rendering canvas/camera.
        content()

        // --- HUD Layer ---

        // Top Control Bar (App Title, Status).
        TopControls(
            areHelpersVisible = uiState.areHelpersVisible,
            experienceMode = uiState.experienceMode,
            isTableVisible = uiState.table.isVisible,
            tableSizeFeet = uiState.table.size.feet,
            isBeginnerViewLocked = uiState.isBeginnerViewLocked,
            targetBallDistance = uiState.targetBallDistance,
            distanceUnit = uiState.distanceUnit,
            onCycleTableSize = { onEvent(MainScreenEvent.CycleTableSize) },
            onMenuClick = { onEvent(MainScreenEvent.ToggleNavigationRail) }
        )

        // Bottom Controls Container (Rotation Slider, Spin Control).
        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left-aligned controls (Empty - buttons moved to rail).
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                // Intentionally empty.
            }

            // Center-aligned controls (Table Rotation).
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

            // Right-aligned controls (Spin Control).
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

        // Zoom Slider (Vertical, Right Side).
        ZoomControls(
            zoomSliderPosition = uiState.zoomSliderPosition,
            onZoomChange = { onEvent(MainScreenEvent.ZoomSliderChanged(it)) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.6f)
                .padding(end = 12.dp)
                .width(48.dp)
        )

        // --- Overlays and Dialogs ---

        // Warning Text Overlay.
        KineticWarningOverlay(text = uiState.warningText)

        // Tutorial Overlay.
        TutorialOverlay(uiState = uiState, onEvent = onEvent)

        // Advanced Options Dialog.
        AdvancedOptionsDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
        )

        // Luminance Adjustment Dialog.
        LuminanceAdjustmentDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
        )

        // Table Size Selection Dialog.
        TableSizeSelectionDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
        )

        // Navigation Rail (Menu).
        AzNavRailMenu(uiState = uiState, onEvent = onEvent)
    }
}

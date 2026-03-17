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
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.ui.composables.dialogs.AdvancedOptionsDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.GlowStickDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.ui.composables.overlays.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.overlays.TutorialOverlay
import com.hereliesaz.cuedetat.ui.composables.sliders.TableRotationSlider

/**
 * The HUD layer of the main screen, rendered as a [NavHost] destination inside
 * the [AzHostActivityLayout] background layer managed by [ProtractorScreen].
 *
 * Contains the AR overlay content, top bar, sliders, spin control, and all dialogs/overlays.
 *
 * @param uiState Current application state.
 * @param onEvent Event dispatcher.
 * @param content The AR content (ProtractorOverlay).
 */
@Composable
fun MainLayout(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        TopControls(
            experienceMode = uiState.experienceMode,
            isTableVisible = uiState.table.isVisible,
            tableSizeFeet = uiState.table.size.feet,
            isBeginnerViewLocked = uiState.isBeginnerViewLocked,
            targetBallDistance = uiState.targetBallDistance,
            distanceUnit = uiState.distanceUnit,
            onCycleTableSize = { onEvent(MainScreenEvent.CycleTableSize) }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                // Intentionally empty.
            }

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

        GlowStickDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { onEvent(MainScreenEvent.ToggleGlowStickDialog) }
        )

        TableSizeSelectionDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
        )
    }
}

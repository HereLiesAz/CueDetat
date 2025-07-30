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
import com.hereliesaz.cuedetat.ui.composables.CuedetatButton
import com.hereliesaz.cuedetat.ui.composables.ExpressiveNavigationRail
import com.hereliesaz.cuedetat.ui.composables.MenuDrawerContent
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
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content, e.g., the rendering canvas
        content()

        // UI Controls layered on top
        TopControls(
            uiState = uiState,
            onEvent = onEvent,
            onMenuClick = { onEvent(MainScreenEvent.ToggleMenu) }
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
                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    CuedetatButton(
                        onClick = { onEvent(MainScreenEvent.AddObstacleBall) },
                        text = "Add Obstacle"
                    )
                }
            }

            // Center-aligned controls
            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TableRotationSlider(uiState = uiState, onEvent = onEvent)
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

                val buttonText = when {
                    uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "Unlock View"
                    else -> "Reset View"
                }
                val buttonEvent = when {
                    uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> MainScreenEvent.UnlockBeginnerView
                    else -> MainScreenEvent.Reset
                }
                CuedetatButton(onClick = { onEvent(buttonEvent) }, text = buttonText)
            }
        }

        ZoomControls(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.6f)
                .padding(end = 8.dp)
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

        // Collapsed Rail (toggled by top-left button)
        ExpressiveNavigationRail(uiState = uiState, onEvent = onEvent)

        // Expanded Menu (toggled by the "Menu" button on the rail)
        AnimatedVisibility(
            visible = uiState.isExpandedMenuVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onEvent(MainScreenEvent.ToggleExpandedMenu) }
            )
        }
        AnimatedVisibility(
            visible = uiState.isExpandedMenuVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut()
        ) {
            MenuDrawerContent(
                uiState = uiState,
                onEvent = onEvent,
                onCloseDrawer = { onEvent(MainScreenEvent.ToggleExpandedMenu) }
            )
        }
    }
}
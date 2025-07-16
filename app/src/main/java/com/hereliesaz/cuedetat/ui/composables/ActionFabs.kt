// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ActionFabs.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun ActionFabs(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!uiState.isBankingMode) {
            ToggleCueBallFab(
                isVisible = uiState.onPlaneBall != null,
                isTableVisible = uiState.table.isVisible,
                onEvent = onEvent
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        ToggleSpinControlFab(
            isVisible = uiState.isSpinControlVisible,
            onEvent = onEvent
        )
    }
}


@Composable
fun ResetFab(
    hasChanged: Boolean,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.Reset) },
        containerColor = if (hasChanged) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Text("Reset\nView")
    }
}

@Composable
fun AddObstacleBallFab(
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.AddObstacle) },
        modifier = modifier
    ) {
        Text("Add\nBall")
    }
}

@Composable
fun ToggleTableFab(
    isVisible: Boolean,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleTable) },
        containerColor = if (isVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Text(if (isVisible) "Hide\nTable" else "Show\nTable")
    }
}


@Composable
fun ToggleCueBallFab(
    isVisible: Boolean,
    isTableVisible: Boolean,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { if (!isTableVisible) onEvent(MainScreenEvent.ToggleOnPlaneBall) },
        containerColor = if (isVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Text("Cue Ball\nToggle")
    }
}

@Composable
fun ToggleSpinControlFab(
    isVisible: Boolean,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleSpinControl) },
        containerColor = if (isVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Text("Spin")
    }
}
// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ActionFabs.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun ToggleSpinControlFab(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleSpinControl) },
        modifier = modifier
            .navigationBarsPadding(),
        containerColor = if (uiState.isSpinControlVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = "Spin",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (uiState.isSpinControlVisible) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ResetFab(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.Reset) },
        modifier = modifier
            .padding(16.dp)
            .navigationBarsPadding(),
        containerColor = if (uiState.valuesChangedSinceReset) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = "Reset\nView",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (uiState.valuesChangedSinceReset) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun AddObstacleBallFab(
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.AddObstacleBall) },
        modifier = modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = "Add\nBall",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ToggleCueBallFab(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = !uiState.showTable
    val containerColor = when {
        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        uiState.onPlaneBall != null -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        uiState.onPlaneBall != null -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    FloatingActionButton(
        onClick = { if (isEnabled) onEvent(MainScreenEvent.ToggleOnPlaneBall) },
        modifier = modifier.navigationBarsPadding(),
        containerColor = containerColor
    ) {
        Text(
            text = "Cue Ball\nToggle",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
fun ToggleTableFab(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (uiState.showTable) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val textColor = if (uiState.showTable) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer

    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleTable) },
        modifier = modifier.navigationBarsPadding(),
        containerColor = containerColor
    ) {
        Text(
            text = if (uiState.showTable) "Hide\nTable" else "Show\nTable",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
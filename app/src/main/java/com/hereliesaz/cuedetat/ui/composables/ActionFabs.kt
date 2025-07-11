// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ActionFabs.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun LockColorFab(
    isColorLocked: Boolean,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.LockOrUnlockColor) },
        modifier = modifier.navigationBarsPadding(),
        containerColor = if (isColorLocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = if (isColorLocked) "Unlock\nColor" else "Lock\nColor",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (isColorLocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
        containerColor = if (uiState.isSpinControlVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Spin",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (uiState.isSpinControlVisible) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
        containerColor = if (uiState.valuesChangedSinceReset) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (uiState.areHelpersVisible) {
            Text(
                text = "Reset\nView",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.valuesChangedSinceReset) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_undo_24),
                contentDescription = "Reset View",
                tint = if (uiState.valuesChangedSinceReset) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Add\nBall",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ToggleCueBallFab(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleOnPlaneBall) },
        modifier = modifier.navigationBarsPadding(),
        containerColor = if (uiState.onPlaneBall != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (uiState.areHelpersVisible) {
            Text(
                text = "Cue Ball\nToggle",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.onPlaneBall != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_jump_shot),
                contentDescription = "Toggle Actual Cue Ball",
                tint = if (uiState.onPlaneBall != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ToggleTableFab(
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleTable) },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Icon(
            painter = painterResource(id = R.drawable.pool_table),
            contentDescription = "Toggle Table",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
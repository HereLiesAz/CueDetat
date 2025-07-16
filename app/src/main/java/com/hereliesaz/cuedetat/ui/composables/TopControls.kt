// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/TopControls.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TopControls(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onMenuClick)
        ) {
            if (uiState.areHelpersVisible) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "Menu",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }


        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (uiState.table.isVisible || uiState.isBankingMode) {
                Text(text = "Distance: ${uiState.targetBallDistance}", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
            }
        }

        if (uiState.table.isVisible) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onEvent(MainScreenEvent.CycleTableSize) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = uiState.table.size.feet, color = MaterialTheme.colorScheme.primary, fontSize = 24.sp)
            }
        } else {
            Box(modifier = Modifier.size(48.dp)) // Placeholder for alignment
        }
    }
}
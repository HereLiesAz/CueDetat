// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/TableControls.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TableControls(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.poolTable != null) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Table Size Button
            Button(
                onClick = { onEvent(MainScreenEvent.ChangeTableSize) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Size: ${uiState.poolTable.size.displayName}",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Table Rotation Slider
            Slider(
                value = uiState.tableRotation,
                onValueChange = { onEvent(MainScreenEvent.RotateTable(it)) },
                valueRange = 0f..360f,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .navigationBarsPadding() // Add padding for navigation bar
            )
        }
    }
}

package com.hereliesaz.cuedetatlite.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetatlite.ui.MainScreenEvent

@Composable
fun LuminanceDialog(
    currentLuminance: Float,
    onEvent: (MainScreenEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(MainScreenEvent.DismissLuminanceDialog) },
        title = { Text("Adjust Luminance") },
        text = {
            VerticalSlider(
                value = currentLuminance,
                onValueChange = { onEvent(MainScreenEvent.LuminanceChanged(it)) }
            )
        },
        confirmButton = {
            Button(onClick = { onEvent(MainScreenEvent.DismissLuminanceDialog) }) {
                Text("OK")
            }
        }
    )
}
package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.TableSize
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TableSizeSelectionDialog(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (false) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Table Size") },
            text = {
                Column {
                    TableSize.entries.forEach { size ->
                        TextButton(onClick = {
                            onEvent(MainScreenEvent.SetTableSize(size))
                            onDismiss()
                        }) {
                            Text(size.feet)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
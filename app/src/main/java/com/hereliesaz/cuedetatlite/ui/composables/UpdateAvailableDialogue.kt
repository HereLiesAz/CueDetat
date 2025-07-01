// app/src/main/java/com/hereliesaz/cuedetatlite/ui/composables/UpdateAvailableDialog.kt
package com.hereliesaz.cuedetatlite.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun UpdateAvailableDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Available") },
        text = { Text("A new version is available. Would you like to download it?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

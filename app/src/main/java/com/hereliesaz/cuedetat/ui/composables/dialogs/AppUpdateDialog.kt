// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AppUpdateDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * One-tap update popup for installs not made by Google Play. "Download" opens the newest
 * release APK in the browser, which downloads it and hands it to the system installer.
 */
@Composable
fun AppUpdateDialog(
    versionName: String,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available") },
        text = { Text("Version $versionName is ready. Download it now?") },
        confirmButton = {
            TextButton(onClick = {
                onInstall()
                onDismiss()
            }) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

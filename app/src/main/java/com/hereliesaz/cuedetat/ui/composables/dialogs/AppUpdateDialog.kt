// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AppUpdateDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * One-tap update popup for the FOSS build. Tapping "Download & install"
 * downloads the newest release APK and launches the system installer (Android
 * always shows its own confirm step — full silent install isn't possible for a
 * normally-installed app).
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
        text = { Text("Version $versionName is ready. Download and install it now?") },
        confirmButton = {
            TextButton(onClick = {
                onInstall()
                onDismiss()
            }) { Text("Download & install") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

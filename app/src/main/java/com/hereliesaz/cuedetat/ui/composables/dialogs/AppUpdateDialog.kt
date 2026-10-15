// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AppUpdateDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.update.UpdateSource

/**
 * Source-specific update notice. Play installs open the Play Store listing;
 * GitHub/sideloaded installs open the GitHub APK (or its release page).
 * Dismissing a notice never starts an installation.
 */
@Composable
fun AppUpdateDialog(
    versionName: String,
    source: UpdateSource,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fromPlay = source == UpdateSource.GOOGLE_PLAY
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (fromPlay) "Google Play update available" else "GitHub update available")
        },
        text = {
            Text(
                if (fromPlay) {
                    "A newer Google Play build ($versionName) is available. Open Google Play to update?"
                } else {
                    "Version $versionName is available on GitHub. Download it now?"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onInstall()
                onDismiss()
            }) { Text(if (fromPlay) "Open Google Play" else "Download") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

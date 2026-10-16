// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AppUpdateDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.R
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
            Text(stringResource(if (fromPlay) R.string.update_play_title else R.string.update_github_title))
        },
        text = {
            Text(
                if (fromPlay) {
                    stringResource(R.string.update_play_body, versionName)
                } else {
                    stringResource(R.string.update_github_body, versionName)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onInstall()
                onDismiss()
            }) { Text(stringResource(if (fromPlay) R.string.update_play_action else R.string.update_github_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
        },
    )
}

// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/calibration/CalibrationSubmissionDialog.kt

package com.hereliesaz.cuedetat.ui.composables.calibration

import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * A dialog prompting the user to submit their calculated calibration profile to the backend.
 *
 * Used to crowd-source device-specific lens profiles.
 *
 * @param onDismiss Callback when the user declines or dismisses the dialog.
 * @param onSubmit Callback when the user agrees to submit.
 */
@Composable
fun CalibrationSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    // Get device model name for the dialog text.
    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Contribute to the D.D.D.D.?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        text = {
            Text(
                "Submit your calibration data anonymously to the open source Dimensional Distortion Device Database? " +
                        "This helps improve lens correction for all users.\n\n" +
                        "The following data will be sent: Camera distortion coefficients and your device model ($deviceName). " +
                        "No personally identifiable information will be collected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No Thanks", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit) {
                Text("Submit", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

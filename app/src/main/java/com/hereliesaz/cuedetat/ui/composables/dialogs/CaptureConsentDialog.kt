package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.hereliesaz.cuedetat.R

/**
 * One-time consent for training capture ([com.hereliesaz.cuedetat.data.CaptureRecorder]).
 *
 * Shown until answered: it can't be dismissed by tapping outside, because the answer is the
 * consent. The first paragraph says plainly what is sent (the disclosure app stores require
 * before camera images leave the phone); the rest is the ask. Either answer can be changed later
 * from the rail's "Training data" item (AzNavRailMenu, id "training_data"), which the copy names;
 * the app has no Settings screen. The decline button is neutral.
 *
 * @param onAnswer true to turn capture on, false to leave it off
 */
@Composable
fun CaptureConsentDialog(onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.capture_consent_title)) },
        text = {
            Column {
                Text(stringResource(R.string.capture_consent_disclosure))
                Spacer(Modifier.height(12.dp))
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.capture_consent_ask_before))
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(stringResource(R.string.capture_consent_ask_emphasis))
                        }
                        append(stringResource(R.string.capture_consent_ask_after))
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.capture_consent_accept)) }
        },
        // Neutral decline: a consent prompt that gates camera-frame upload must not shame "no".
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.capture_consent_decline)) }
        },
    )
}

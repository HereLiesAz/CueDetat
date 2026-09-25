package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * One-time consent for training capture ([com.hereliesaz.cuedetat.data.CaptureRecorder]).
 *
 * Shown until answered: it can't be dismissed by tapping outside, because the answer is the
 * consent. The first paragraph says plainly what is sent (the disclosure app stores require
 * before camera images leave the phone); the rest is the ask. Either answer can be changed later
 * from the menu's "Training data" item.
 *
 * @param onAnswer true to turn capture on, false to leave it off
 */
@Composable
fun CaptureConsentDialog(onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Training data") },
        text = {
            Column {
                Text(
                    "While the camera is on, Cue D’état saves a camera frame every few seconds, " +
                        "with the phone's tilt and heading and what the app detected, and sends " +
                        "them over Wi-Fi to the developer to train the table and ball detection. " +
                        "Your location, accounts and contacts are not included."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    buildAnnotatedString {
                        append("You got the app for free. Helping make it better is the ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("least") }
                        append(" you can do. But if you must, you can enable your selfishness by disabling the option in Settings.")
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text("Keep helping") } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text("Be selfish") } },
    )
}

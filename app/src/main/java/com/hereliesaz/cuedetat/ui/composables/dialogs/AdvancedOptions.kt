// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AdvancedOptions.kt
package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Debug view of the computer vision. It once also held two Canny-threshold sliders and an
 * "auto-tune for bad light" button; the detection pipeline stopped using Canny and nothing read
 * those values, so they changed nothing and the button always ended in "too dark". They were
 * removed rather than left promising a result.
 */
@Composable
fun AdvancedOptionsDialog(
    state: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val cvMaskText = stringResource(R.string.advanced_cv_mask)
    val cvMaskState = stringResource(
        if (state.showCvMask) R.string.advanced_cv_mask_shown else R.string.advanced_cv_mask_hidden
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.advanced_title)) },
        text = {
            Column {
                // The whole row is the 48dp toggle target; the Switch itself only displays state.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = state.showCvMask,
                            role = Role.Switch,
                            onValueChange = { onEvent(MainScreenEvent.ToggleCvMask) }
                        )
                        .semantics {
                            contentDescription = cvMaskText
                            stateDescription = cvMaskState
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cvMaskText, modifier = Modifier.weight(1f))
                    Switch(checked = state.showCvMask, onCheckedChange = null)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
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

@Composable
fun AdvancedOptionsDialog(
    state: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val canny1Text = stringResource(R.string.advanced_canny_1, state.cannyThreshold1.toInt())
    val canny2Text = stringResource(R.string.advanced_canny_2, state.cannyThreshold2.toInt())
    val cvMaskText = stringResource(R.string.advanced_cv_mask)
    val cvMaskState = stringResource(
        if (state.showCvMask) R.string.advanced_cv_mask_shown else R.string.advanced_cv_mask_hidden
    )
    val autoTuneText = stringResource(
        if (state.isAutoCalibrating) R.string.advanced_auto_tuning else R.string.advanced_auto_tune
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.advanced_title)) },
        text = {
            Column {
                Text(canny1Text)
                Slider(
                    value = state.cannyThreshold1,
                    onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) },
                    valueRange = 0f..255f,
                    modifier = Modifier.semantics { contentDescription = canny1Text }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(canny2Text)
                Slider(
                    value = state.cannyThreshold2,
                    onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) },
                    valueRange = 0f..255f,
                    modifier = Modifier.semantics { contentDescription = canny2Text }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onEvent(MainScreenEvent.AutoCalibrateCv) },
                    enabled = !state.isAutoCalibrating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(autoTuneText)
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
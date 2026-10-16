// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AdvancedOptions.kt
package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.*
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
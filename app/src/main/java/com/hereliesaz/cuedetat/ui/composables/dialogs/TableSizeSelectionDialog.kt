package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TableSize

/**
 * A dialog for selecting the physical size of the pool table (e.g., 7ft, 8ft, 9ft).
 * This scaling factor is critical for accurate bank shot calculations.
 *
 * @param uiState Current application state.
 * @param onEvent Callback to set the table size.
 * @param onDismiss Callback to close the dialog.
 */
@Composable
fun TableSizeSelectionDialog(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (uiState.showTableSizeDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.table_size_dialog_title), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Iterate through all defined table sizes enum.
                    TableSize.entries.forEach { size ->
                        TextButton(
                            onClick = { onEvent(MainScreenEvent.SetTableSize(size)); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.table_size_option, size.feet))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

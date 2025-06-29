package com.hereliesaz.cuedetat.ui.spatial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.composables.ShotControls
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.state.ShotType
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState

private val shotType: ShotType

@Composable
fun SpatialControls(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit
) {
    if (uiState.tablePose != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(8.dp)) {
                    ShotControls(
                        selectedShotType = uiState.shotType,
                        onShotTypeSelect = { onEvent(UiEvent.SetShotType(it)) }
                    )

                    if (uiState.shotType == ShotType.CUT || uiState.shotType == ShotType.BANK) {
                        SpinControl(onSpinChanged = { onEvent(UiEvent.SetSpin(it)) })
                    }
                }
            }
        }
    }
}

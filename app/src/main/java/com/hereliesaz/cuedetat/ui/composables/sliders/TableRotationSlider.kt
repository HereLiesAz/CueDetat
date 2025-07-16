// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/sliders/TableRotationSlider.kt
package com.hereliesaz.cuedetat.ui.composables.sliders

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TableRotationSlider(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.table.isVisible) {
        Slider(
            value = uiState.table.rotationDegrees,
            onValueChange = { onEvent(MainScreenEvent.TableRotationChanged(it)) },
            modifier = modifier,
            valueRange = 0f..360f
        )
    }
}
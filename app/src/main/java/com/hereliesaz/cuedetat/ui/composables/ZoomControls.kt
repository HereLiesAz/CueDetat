// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ZoomControls.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.VerticalSlider
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.OverlayState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomControls(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_zoom_in_24),
                contentDescription = stringResource(id = R.string.zoom_icon),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.areHelpersVisible) {
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            val distanceText = if (uiState.distanceUnit == DistanceUnit.METRIC) {
                "${uiState.targetBallDistance.toInt()} cm"
            } else {
                val totalInches = (uiState.targetBallDistance / 2.54f).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                "$feet' $inches\""
            }
            Text(
                text = distanceText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        VerticalSlider(
            value = uiState.zoomSliderPosition,
            onValueChange = { onEvent(MainScreenEvent.ZoomSliderChanged(it)) },
            modifier = Modifier.weight(1f),
            valueRange = -50f..50f,
            thumb = { interactionSource ->
                SliderDefaults.Thumb(interactionSource = interactionSource)
            },
            track = { sliderState ->
                SliderDefaults.Track(sliderState = sliderState)
            },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
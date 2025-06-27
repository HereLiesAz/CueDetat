// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ZoomControls.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight // Keep this
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// import androidx.compose.foundation.layout.width // Not strictly needed for the Column here if VerticalSlider defines its own width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
// Remove: import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.VerticalSlider // <-- IMPORT THE CUSTOM ONE
import com.hereliesaz.cuedetat.view.state.OverlayState

// We need Color for the SliderDefaults if we were using it for ticks, but M3 SliderDefaults handles it.
// import androidx.compose.ui.graphics.Color // Not needed if SliderDefaults handles everything

@Composable
fun ZoomControls(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier // This modifier is applied to the Column
) {
    Column(
        modifier = modifier // The parent in MainScreen.kt gives this Column its height and alignment
            .padding(vertical = 16.dp), // Add some vertical padding for the icon and text
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.areHelpersVisible) {
            Text(
                text = "Zoom",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_zoom_in_24),
                contentDescription = stringResource(id = R.string.zoom_icon),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // Consistent spacer

        VerticalSlider( // Using the custom VerticalSlider
            value = uiState.zoomSliderPosition,
            onValueChange = { onEvent(MainScreenEvent.ZoomSliderChanged(it)) },
            valueRange = 0f..100f,
            modifier = Modifier
                .weight(1f) // Slider will take available vertical space in the Column
                .fillMaxHeight(), // Ensure it tries to fill the weighted space.
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A vertical implementation of the Material3 Slider.
 *
 * Wraps the standard [Slider] and applies the [vertical] modifier to rotate it.
 *
 * @param value Current value.
 * @param onValueChange Callback for value changes.
 * @param modifier Styling modifier.
 * @param enabled Whether slider is active.
 * @param valueRange Min/Max range.
 * @param steps Discrete steps.
 * @param onValueChangeFinished Callback on drag end.
 * @param interactionSource Interaction source.
 * @param colors Theme colors.
 * @param thumb Thumb composable.
 * @param track Track composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors(),
    thumb: @Composable (MutableInteractionSource) -> Unit,
    track: @Composable (SliderState) -> Unit
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.vertical(), // Apply rotation
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { thumb(interactionSource) },
        track = { track(it) }
    )
}

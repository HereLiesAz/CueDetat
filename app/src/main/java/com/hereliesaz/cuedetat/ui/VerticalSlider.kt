package com.hereliesaz.cuedetat.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// This is the VerticalSlider composable you provided
@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalSlider(
    state: SliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            sliderState = sliderState,
            colors = colors,
            enabled = enabled,
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            sliderState = sliderState,
            trackCornerSize = Dp.Unspecified
        )
    }
) {
    // NOTE: The full implementation of this composable (which you have) goes here.
    // As I don't have the implementation, I am assuming it exists in your project.
    // The call to it from MainScreen.kt will work correctly once the full
    // code is in this file.
}

// This is the sample you provided
@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun VerticalSliderSample() {
    val coroutineScope = rememberCoroutineScope()
    val sliderState =
        rememberSliderState(
            steps = 9,
            valueRange = 0f..100f
        )
    val snapAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    var currentValue by rememberSaveable { mutableFloatStateOf(sliderState.value) }
    var animateJob: Job? by remember { mutableStateOf(null) }
    sliderState.shouldAutoSnap = false
    sliderState.onValueChange = { newValue ->
        currentValue = newValue
        if (sliderState.isDragging) {
            animateJob?.cancel()
            sliderState.value = newValue
        }
    }
    sliderState.onValueChangeFinished = {
        animateJob =
            coroutineScope.launch {
                animate(
                    initialValue = sliderState.value,
                    targetValue = currentValue,
                    animationSpec = snapAnimationSpec
                ) { value, _ ->
                    sliderState.value = value
                }
            }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "%.2f".format(sliderState.value)
        )
        Spacer(Modifier.height(16.dp))
        VerticalSlider(
            state = sliderState,
            modifier =
                Modifier
                    .height(300.dp)
                    .align(Alignment.CenterHorizontally),
            interactionSource = interactionSource,
            track = {
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.width(36.dp),
                    trackCornerSize = 12.dp
                )
            },
            reverseDirection = true
        )
    }
}
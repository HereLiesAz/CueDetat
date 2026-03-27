// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.PocketId
import kotlinx.coroutines.delay

/**
 * Full-screen scan UI.
 *
 * Shows a live camera feed with a pocket progress overlay.
 * Six pocket indicators fill in (yellow solid) as each pocket is detected.
 * Done button unlocks when all 6 are found. Reset clears accumulated detections.
 *
 * GPS permission is requested here at scan-start time.
 */
@Composable
fun TableScanScreen(
    onEvent: (MainScreenEvent) -> Unit,
    uiState: CueDetatState,
    viewModel: TableScanViewModel = hiltViewModel()
) {
    // Animation state: tracks whether the user has tapped capture (circle expands).
    var isCapturing by remember { mutableStateOf(false) }

    // Captured felt color from the ViewModel (non-null once captureFeltAndComplete fires).
    val capturedHsv by viewModel.capturedFeltHsv.collectAsState()

    // Magnifying circle size: expands to 240dp on tap, snaps back to 120dp when idle.
    val circleSize by animateDpAsState(
        targetValue = if (isCapturing) 240.dp else 120.dp,
        animationSpec = tween(durationMillis = 400),
        label = "magnifyCircle"
    )

    // Alpha for the felt display circle: 0 → snap 1 → hold 1s → animate to 0.
    val feltAlpha = remember { Animatable(0f) }

    // When the captured color arrives, drive the display-then-fade animation.
    LaunchedEffect(capturedHsv) {
        if (capturedHsv != null) {
            isCapturing = false
            feltAlpha.snapTo(1f)
            delay(1000L)
            feltAlpha.animateTo(0f, animationSpec = tween(durationMillis = 1000))
        }
    }

    // Pass current state snapshot to ViewModel so it can do coordinate transforms.
    LaunchedEffect(uiState.inversePitchMatrix, uiState.hasInverseMatrix) {
        viewModel.updateStateSnapshot(
            uiState.inversePitchMatrix,
            uiState.hasInverseMatrix,
            uiState.viewWidth,
            uiState.viewHeight
        )
    }

    // Forward each scan event to the main event bus. Do NOT close the screen here —
    // closures cancel the collector and would drop the second emission.
    val scanResult = viewModel.scanResult
    LaunchedEffect(scanResult) {
        scanResult.collect { result -> onEvent(result) }
    }

    // Reset scan state each time this overlay appears, then wait for completion.
    // Combining reset + close-watch into one LaunchedEffect(Unit) prevents a race where
    // a stale scanComplete=true (left from the previous capture) would fire the close
    // effect immediately before resetScan() had a chance to clear it.
    // The 2-second delay matches the felt display animation (1s hold + 1s fade).
    LaunchedEffect(Unit) {
        viewModel.resetScan()
        viewModel.scanComplete.collect { complete ->
            if (complete) {
                delay(2000L)
                onEvent(MainScreenEvent.ToggleTableScanScreen)
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // State hoisted above conditionals (Compose rule: no @Composable calls in if/when blocks)
        val selectedIds by viewModel.selectedSampleIds.collectAsState()

        // Top Gallery: Felt Samples
        if (uiState.savedFeltSamples.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.savedFeltSamples) { sample ->
                    val color = Color.hsv(sample.hsv[0], sample.hsv[1], sample.hsv[2])
                    val isSelected = selectedIds.contains(sample.id)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                shape = CircleShape
                            )
                            .combinedClickable(
                                onClick = { viewModel.toggleSampleSelection(sample.id) },
                                onLongClick = { viewModel.toggleSampleSelection(sample.id) }
                            )
                    )
                }
            }
        }

        // Context Menu for Selected Samples
        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 240.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = { /* TODO Move */ }) {
                    Text("Move", color = Color.White)
                }
                TextButton(onClick = { viewModel.deleteSelectedSamples() }) {
                    Text("Delete", color = Color.Red.copy(alpha = 0.8f))
                }
            }
        }

        // Magnifying circle: visible while capture hasn't completed yet.
        // Expands from 120dp to 240dp when the user taps, giving tactile feedback.
        if (capturedHsv == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            ) {
                // Hide the crosshair while expanding — it looks odd when stretched.
                if (!isCapturing) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)
                    }
                }
            }
        }

        // Felt display circle: appears once capture is done, fades out after 1 second.
        capturedHsv?.let { hsv ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(feltAlpha.value)
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hsv[0], hsv[1], hsv[2]))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            )
        }

        // Bottom Controls: hidden once capture is in progress or complete.
        if (!isCapturing && capturedHsv == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Point at the felt and tap below",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Camera Shutter Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color.LightGray, CircleShape)
                        .clickable {
                            isCapturing = true
                            viewModel.captureFeltAndComplete()
                        }
                )
            }
        }
    }
}

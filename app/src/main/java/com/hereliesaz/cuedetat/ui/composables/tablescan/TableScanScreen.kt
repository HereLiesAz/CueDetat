// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    LaunchedEffect(Unit) {
        viewModel.resetScan()
        viewModel.scanComplete.collect { complete ->
            if (complete) {
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

        // Magnifying Circle in the center.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f))
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
        ) {
            // Visual indicator of the sampled color.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 1.dp.toPx())
                )
                // Small dot in the dead center.
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = center
                )
            }
        }

        // Bottom Controls: Capture Felt Button.
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
                    .clickable { viewModel.captureFeltAndComplete() }
            )
        }
    }
}

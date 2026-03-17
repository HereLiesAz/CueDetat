// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val scanProgress by viewModel.scanProgress.collectAsState()

    // GPS permission request — fired once when the composable first appears.
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — TableScanRepository.getCurrentLocation() handles null gracefully */ }
    LaunchedEffect(Unit) {
        locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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

    // Close screen only after completeScan has signalled that ALL events have been emitted.
    val scanComplete by viewModel.scanComplete.collectAsState()
    LaunchedEffect(scanComplete) {
        if (scanComplete) {
            onEvent(MainScreenEvent.ToggleTableScanScreen)
        }
    }

    val foundCount = scanProgress.count { it.value }
    val allFound = foundCount >= 6

    Box(modifier = Modifier.fillMaxSize()) {
        // Pocket progress overlay.
        // Positions match PocketId enum order: TL, TR, BL, BR, SL, SR.
        val pocketPositionFractions = remember {
            listOf(
                0.15f to 0.15f, // TL
                0.85f to 0.15f, // TR
                0.15f to 0.85f, // BL
                0.85f to 0.85f, // BR
                0.15f to 0.50f, // SL
                0.85f to 0.50f  // SR
            )
        }
        val pocketIds = remember { PocketId.values() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            pocketIds.forEachIndexed { i, id ->
                val (xFrac, yFrac) = pocketPositionFractions[i]
                val pos = Offset(size.width * xFrac, size.height * yFrac)
                val isFound = scanProgress[id] == true
                drawCircle(
                    color = Color.Yellow,
                    radius = 24.dp.toPx(),
                    center = pos,
                    style = if (isFound) Fill else Stroke(width = 3.dp.toPx()),
                    alpha = if (isFound) 0.9f else 0.5f
                )
            }
        }

        // Controls overlay.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$foundCount / 6 pockets found — pan across the table",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    viewModel.resetScan()
                }) { Text("Reset") }
                Button(
                    onClick = { onEvent(MainScreenEvent.ToggleTableScanScreen) },
                    enabled = allFound
                ) { Text("Done") }
            }
        }
    }
}

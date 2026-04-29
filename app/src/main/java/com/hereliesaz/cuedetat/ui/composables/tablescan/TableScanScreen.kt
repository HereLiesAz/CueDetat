// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.os.Build
import android.view.HapticFeedbackConstants
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
    var showGreenFlash by remember { mutableStateOf(false) }

    // Captured felt color from the ViewModel (non-null once captureFeltAndComplete fires).
    val capturedHsv by viewModel.capturedFeltHsv.collectAsState()
    val scanStep by viewModel.scanStep.collectAsState()
    val currentPocketTarget by viewModel.currentPocketTarget.collectAsState()

    val capturedCount = PocketId.entries.count { id ->
        currentPocketTarget != null &&
                PocketId.entries.indexOf(id) < PocketId.entries.indexOf(currentPocketTarget!!)
    }

    LaunchedEffect(capturedCount) {
        if (capturedCount > 0) {
            showGreenFlash = true
            delay(300L)
            showGreenFlash = false
        }
    }

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

    LaunchedEffect(uiState.inversePitchMatrix, uiState.hasInverseMatrix, uiState.viewOffset, uiState.worldRotationDegrees, uiState.zoomSliderPosition) {
        viewModel.updateStateSnapshot(
            uiState.inversePitchMatrix,
            uiState.hasInverseMatrix,
            uiState.viewWidth,
            uiState.viewHeight,
            uiState.viewOffset,
            uiState.worldRotationDegrees,
            uiState.zoomSliderPosition
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
                onEvent(MainScreenEvent.StartArTracking)
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // State hoisted above conditionals (Compose rule: no @Composable calls in if/when blocks)
        val selectedIds by viewModel.selectedSampleIds.collectAsState()
        val mlConfidence by viewModel.mlConfidence.collectAsState()
        val darknessConfidence by viewModel.darknessConfidence.collectAsState()
        val view = LocalView.current

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
            if (scanStep == ScanStep.FELT_CAPTURE) {
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
                        val crosshairColor = if (mlConfidence > 0.8f) Color.Green else Color.White
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = crosshairColor.copy(alpha = 0.3f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(color = crosshairColor, radius = 2.dp.toPx(), center = center)
                        }
                    }
                }
            } else if (scanStep == ScanStep.POCKET_GUIDE) {
                // Breathing pulse animation
                val infiniteTransition = rememberInfiniteTransition(label = "reticle")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.97f, targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(180.dp * pulseScale)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Inner crosshair dot
                        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = center)
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
        if (!isCapturing && (capturedHsv == null || scanStep == ScanStep.POCKET_GUIDE)) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (scanStep) {
                    ScanStep.FELT_CAPTURE -> {
                        Text(
                            text = "Point at the felt and tap below",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Existing shutter button (unchanged)
                        Box(modifier = Modifier.size(80.dp).padding(4.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 4.dp.toPx()))
                                drawArc(
                                    color = if (mlConfidence > 0.8f) Color.Green else Color.Yellow,
                                    startAngle = -90f, sweepAngle = 360f * mlConfidence,
                                    useCenter = false, style = Stroke(width = 4.dp.toPx())
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center).size(64.dp).clip(CircleShape)
                                    .background(Color.White).border(4.dp, Color.LightGray, CircleShape)
                                    .clickable { isCapturing = true; viewModel.captureFeltAndComplete() }
                            )
                        }
                    }

                    ScanStep.POCKET_GUIDE -> {
                        val pocketNames = mapOf(
                            PocketId.TL to "Top Left", PocketId.TR to "Top Right",
                            PocketId.BL to "Bottom Left", PocketId.BR to "Bottom Right",
                            PocketId.SL to "Left Side", PocketId.SR to "Right Side"
                        )
                        val targetName = pocketNames[currentPocketTarget] ?: "Pocket"

                        Text(
                            text = "Aim the $targetName pocket into the ring",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 6-pocket progress row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            PocketId.entries.forEachIndexed { index, _ ->
                                val isDone = index < (PocketId.entries.indexOf(currentPocketTarget ?: PocketId.SR))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isDone) Color.Green else Color.White.copy(alpha = 0.4f))
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            }
                        }

                        // Capture button — enabled only when darkness confidence > 0.5
                        val captureEnabled = darknessConfidence > 0.5f
                        Box(modifier = Modifier.size(80.dp).padding(4.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 4.dp.toPx()))
                                drawArc(
                                    color = if (darknessConfidence > 0.8f) Color.Green else Color.White,
                                    startAngle = -90f, sweepAngle = 360f * darknessConfidence,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center).size(64.dp).clip(CircleShape)
                                    .background(if (captureEnabled) Color.White else Color.Gray.copy(alpha = 0.5f))
                                    .border(4.dp, Color.LightGray, CircleShape)
                                    .clickable(enabled = captureEnabled) {
                                        val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            HapticFeedbackConstants.CONFIRM
                                        } else {
                                            HapticFeedbackConstants.LONG_PRESS
                                        }
                                        view.performHapticFeedback(haptic)
                                        viewModel.captureCurrentPocket()
                                    }
                            )
                        }
                    }

                    ScanStep.AUTO_READY -> {
                        // AUTO_READY is superseded by the wizard geometry validation —
                        // the wizard transitions directly to AR_ACTIVE. This branch is kept
                        // as a safety fallback only.
                        Text(
                            text = "Table geometry confirmed. Starting AR…",
                            color = Color.Green,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showGreenFlash,
            enter = fadeIn(animationSpec = tween(50)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Green.copy(alpha = 0.25f))
            )
        }
    }
}

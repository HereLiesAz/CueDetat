// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignScreen.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun QuickAlignScreen(
    uiState: OverlayState,
    analyzer: VisionAnalyzer,
    onEvent: (MainScreenEvent) -> Unit,
    viewModel: QuickAlignViewModel = hiltViewModel()
) {
    val tappedPoints by viewModel.tappedPoints.collectAsState()
    val currentTapIndex by viewModel.currentTapIndex.collectAsState()
    val instructions = viewModel.logicalPointsToTap.getOrNull(currentTapIndex)
        ?: "All points selected. Press Finish to align."

    val density = LocalDensity.current
    val textPaint = remember(density) {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 16.sp.toPx() }
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    viewModel.onScreenTapped(offset)
                }
            }
    ) {
        CameraBackground(modifier = Modifier.fillMaxSize(), analyzer = analyzer)

        Canvas(modifier = Modifier.fillMaxSize()) {
            tappedPoints.forEachIndexed { index, offset ->
                drawCircle(
                    color = MaterialTheme.colorScheme.primary,
                    radius = 8.dp.toPx(),
                    center = offset
                )
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        "${index + 1}",
                        offset.x,
                        offset.y - textPaint.fontMetrics.ascent / 2 - textPaint.fontMetrics.descent,
                        textPaint
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Instructions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Quick Align",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap: $instructions",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.onResetPoints() }) {
                    Text("Reset Points")
                }
                TextButton(
                    onClick = { /* TODO: Implement calculation and apply */ },
                    enabled = tappedPoints.size == 4
                ) {
                    Text("Finish")
                }
                TextButton(onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }) {
                    Text("Cancel")
                }
            }
        }
    }
}
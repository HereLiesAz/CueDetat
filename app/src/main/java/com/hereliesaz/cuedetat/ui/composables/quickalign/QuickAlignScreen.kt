// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignScreen.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.CuedetatButton
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.hypot

@Composable
fun QuickAlignScreen(
    onEvent: (MainScreenEvent) -> Unit,
    viewModel: QuickAlignViewModel = hiltViewModel(),
    analyzer: QuickAlignAnalyzer
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val pocketPositions by viewModel.pocketPositions.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.alignResult.collect { event ->
            onEvent(event)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentStep) {
            QuickAlignStep.SELECT_SIZE -> SizeSelectionStep(viewModel)
            QuickAlignStep.CAPTURE_PHOTO -> CaptureStep(analyzer)
            QuickAlignStep.ALIGN_TABLE -> capturedBitmap?.let { bmp ->
                AlignmentStep(
                    viewModel = viewModel,
                    bitmap = bmp,
                    pocketPositions = pocketPositions
                )
            }

            QuickAlignStep.FINISHED -> {} // Handled by event
        }

        // Common bottom controls
        if (currentStep != QuickAlignStep.CAPTURE_PHOTO) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.onCancel(); onEvent(MainScreenEvent.ToggleQuickAlignScreen) }) {
                    Text("Cancel")
                }
                if (currentStep == QuickAlignStep.ALIGN_TABLE) {
                    TextButton(onClick = { viewModel.onResetPoints() }) {
                        Text("Reset")
                    }
                    TextButton(onClick = { viewModel.onFinishAlign(); onEvent(MainScreenEvent.ToggleQuickAlignScreen) }) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlignmentStep(
    viewModel: QuickAlignViewModel,
    bitmap: android.graphics.Bitmap,
    pocketPositions: Map<DraggablePocket, Offset>
) {
    var draggedPocket by remember { mutableStateOf<DraggablePocket?>(null) }
    val touchRadius = 60f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        draggedPocket = pocketPositions
                            .minByOrNull { (_, pos) ->
                                hypot(
                                    startOffset.x - pos.x,
                                    startOffset.y - pos.y
                                )
                            }
                            ?.let { (pocket, pos) ->
                                if (hypot(
                                        startOffset.x - pos.x,
                                        startOffset.y - pos.y
                                    ) < touchRadius
                                ) pocket else null
                            }
                    },
                    onDragEnd = { draggedPocket = null },
                    onDragCancel = { draggedPocket = null },
                    onDrag = { change, _ ->
                        draggedPocket?.let {
                            viewModel.onPocketDrag(it, change.position)
                        }
                    }
                )
            }) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Table Photo for Alignment",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val tl = pocketPositions[DraggablePocket.TOP_LEFT] ?: return@Canvas
            val tr = pocketPositions[DraggablePocket.TOP_RIGHT] ?: return@Canvas
            val br = pocketPositions[DraggablePocket.BOTTOM_RIGHT] ?: return@Canvas
            val bl = pocketPositions[DraggablePocket.BOTTOM_LEFT] ?: return@Canvas
            pocketPositions[DraggablePocket.SIDE_LEFT] ?: return@Canvas
            pocketPositions[DraggablePocket.SIDE_RIGHT] ?: return@Canvas

            val railPath = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }
            drawPath(railPath, Color.Yellow, alpha = 0.7f, style = Stroke(width = 4.dp.toPx()))

            pocketPositions.forEach { (pocket, pos) ->
                val isCorner =
                    pocket != DraggablePocket.SIDE_LEFT && pocket != DraggablePocket.SIDE_RIGHT
                val radius = if (isCorner) 30f else 20f
                drawCircle(
                    Color.Yellow,
                    radius = radius,
                    center = pos,
                    style = Stroke(width = 4.dp.toPx()),
                    alpha = 0.9f
                )
                drawCircle(Color.Yellow, radius = 8f, center = pos, alpha = 0.9f)
            }
        }
        Instructions(text = "Drag the yellow pockets to match the photo.")
    }
}

@Composable
private fun SizeSelectionStep(viewModel: QuickAlignViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Instructions(text = "First, select your table size.")
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TableSize.entries.forEach { size ->
                TextButton(
                    onClick = { viewModel.onTableSizeSelected(size) },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(
                        text = "${size.feet}' Table",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureStep(analyzer: QuickAlignAnalyzer) {
    Box(Modifier.fillMaxSize()) {
        CameraBackground(modifier = Modifier.fillMaxSize(), analyzer = analyzer)
        Instructions(text = "Take a wide shot of the entire table.")
        CuedetatButton(
            onClick = { analyzer.captureFrame() },
            text = "Capture",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun Instructions(text: String) {
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
            text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
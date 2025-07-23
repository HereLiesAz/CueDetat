package com.hereliesaz.cuedetat.ui.composables.quickalign

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.view.ProtractorOverlay
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.TableSize

@Composable
fun QuickAlignScreen(
    uiState: OverlayState,
    analyzer: VisionAnalyzer,
    onEvent: (MainScreenEvent) -> Unit,
    viewModel: QuickAlignViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val photo by analyzer.currentFrameBitmap.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentStep) {
            QuickAlignStep.SELECT_SIZE -> {
                SizeSelectionStep(viewModel = viewModel, onEvent = onEvent)
            }

            QuickAlignStep.ALIGN_TABLE -> {
                photo?.let {
                    AlignmentStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        onEvent = onEvent,
                        photo = it
                    )
                } ?: CameraBackground(
                    modifier = Modifier.fillMaxSize(),
                    analyzer = analyzer
                ) // Show camera while waiting for photo
            }
        }
    }
}

@Composable
private fun SizeSelectionStep(
    viewModel: QuickAlignViewModel,
    onEvent: (MainScreenEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Instructions(text = "First, select your table size.")
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }) {
            Text("Cancel")
        }
    }
}

@Composable
private fun AlignmentStep(
    uiState: OverlayState,
    viewModel: QuickAlignViewModel,
    onEvent: (MainScreenEvent) -> Unit,
    photo: Bitmap
) {
    val alignState by viewModel.alignState.collectAsState()
    val selectedTableSize by viewModel.selectedTableSize.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp


    val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        viewModel.onZoom(zoomChange)
        viewModel.onPan(panChange)
        viewModel.onRotate(rotationChange)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = "Table Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        val alignUiState = remember(alignState, selectedTableSize, screenWidth, screenHeight) {
            uiState.copy(
                viewWidth = with(LocalDensity.current) { screenWidth.toPx().toInt() },
                viewHeight = with(LocalDensity.current) { screenHeight.toPx().toInt() },
                table = Table(size = selectedTableSize ?: TableSize.NINE_FT, isVisible = true),
                zoomSliderPosition = ZoomMapping.zoomToSlider(alignState.zoom),
                worldRotationDegrees = alignState.rotation,
                viewOffset = PointF(alignState.pan.x, alignState.pan.y),
                onPlaneBall = null, // No need for balls in alignment
                pitchAngle = 0f // Force flat view for alignment
            )
        }

        ProtractorOverlay(
            uiState = alignUiState,
            systemIsDark = true,
            isTestingCvMask = false,
            onEvent = {},
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Instructions(text = "Use pinch, pan, and twist gestures to align the virtual table's pockets with the pockets in the photo.")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.onResetView() }) {
                    Text("Reset View")
                }
                TextButton(onClick = {
                    viewModel.onFinishAlign(alignUiState.viewWidth, alignUiState.viewHeight)
                    onEvent(MainScreenEvent.ToggleQuickAlignScreen)
                }) {
                    Text("Finish")
                }
                TextButton(onClick = {
                    viewModel.onCancel()
                    onEvent(MainScreenEvent.ToggleQuickAlignScreen)
                }) {
                    Text("Cancel")
                }
            }
        }
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
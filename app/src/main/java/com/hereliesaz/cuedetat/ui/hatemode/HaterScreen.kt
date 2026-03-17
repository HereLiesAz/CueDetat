// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.TopControls

/**
 * The main screen for "Hater Mode".
 *
 * Simulates a Magic-8-Ball experience where a 20-sided die floats in liquid.
 * The physics canvas is placed in a [background] layer; [TopControls] is placed
 * in an [onscreen] block per the AzNavRail architecture contract.
 *
 * @param haterViewModel The ViewModel managing the physics state.
 * @param uiState Global app state (for shared UI elements like the menu).
 * @param onEvent Event dispatcher.
 */
@Composable
fun HaterScreen(
    haterViewModel: HaterViewModel,
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    val state by haterViewModel.haterState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    LaunchedEffect(Unit) {
        haterViewModel.onEvent(HaterEvent.EnterHaterMode)
    }

    AzNavRailMenu(
        uiState = uiState,
        onEvent = onEvent,
        navController = navController,
        currentDestination = currentRoute
    ) {
        // --- Background layer: physics / die canvas ---
        background(weight = 0) {
            val context = LocalContext.current
            val bitmap = remember(state.answerResId) {
                BitmapFactory.decodeResource(context.resources, state.answerResId)
                    ?.asImageBitmap()
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                haterViewModel.onEvent(HaterEvent.Dragging(dragAmount))
                            },
                            onDragEnd = { haterViewModel.onEvent(HaterEvent.DragEnd) }
                        )
                    }
            ) {
                haterViewModel.setupBoundaries(size.width, size.height)
                val centerX = size.width / 2
                val centerY = size.height / 2

                drawRect(color = Color.Black)

                val targetSize = minOf(size.width, size.height) * 0.55f

                // Glow: soft bioluminescent emanation — very subtle, deepens with scale
                if (state.dieScale > 0.01f) {
                    val dieCenter = Offset(centerX + state.diePosition.x, centerY + state.diePosition.y)
                    val glowBase  = targetSize * 0.5f * state.dieScale
                    for (i in 0..3) {
                        drawCircle(
                            color  = Color(0xFF3311BB).copy(alpha = (0.055f - i * 0.011f) * state.dieScale),
                            radius = glowBase * (1.1f + i * 0.4f),
                            center = dieCenter
                        )
                    }
                }

                // Die bitmap: alpha tied to dieScale so it materializes out of the dark liquid
                bitmap?.let { bmp ->
                    val alphaPaint = Paint().apply {
                        alpha = (state.dieScale.coerceIn(0f, 1f) * 255f).toInt()
                    }
                    drawIntoCanvas { canvas ->
                        canvas.save()
                        canvas.translate(centerX + state.diePosition.x, centerY + state.diePosition.y)
                        canvas.rotate(state.dieAngle)
                        val scale = (targetSize / maxOf(bmp.width, bmp.height)) * state.dieScale
                        canvas.scale(scale, scale)
                        canvas.nativeCanvas.drawBitmap(
                            bmp.asAndroidBitmap(),
                            -bmp.width / 2f,
                            -bmp.height / 2f,
                            alphaPaint
                        )
                        canvas.restore()
                    }
                }
            }
        }

        // --- Onscreen: top status bar ---
        onscreen(alignment = Alignment.TopEnd) {
            TopControls(
                experienceMode = uiState.experienceMode,
                isTableVisible = uiState.table.isVisible,
                tableSizeFeet = uiState.table.size.feet,
                isBeginnerViewLocked = uiState.isBeginnerViewLocked,
                targetBallDistance = uiState.targetBallDistance,
                distanceUnit = uiState.distanceUnit,
                onCycleTableSize = { onEvent(MainScreenEvent.CycleTableSize) }
            )
        }
    }
}

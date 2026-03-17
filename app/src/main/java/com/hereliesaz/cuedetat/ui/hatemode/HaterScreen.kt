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

                drawRect(color = androidx.compose.ui.graphics.Color.Black)

                val targetSize = minOf(size.width, size.height) * 0.55f

                bitmap?.let { bmp ->
                    val andBmp    = bmp.asAndroidBitmap()
                    val hw        = bmp.width  / 2f
                    val hh        = bmp.height / 2f
                    val baseScale = (targetSize / maxOf(bmp.width, bmp.height)) * state.dieScale
                    val cx        = centerX + state.diePosition.x
                    val cy        = centerY + state.diePosition.y

                    drawIntoCanvas { canvas ->
                        // --- Adjacent faces: the other sides of the die dimly visible through the liquid ---
                        // A d20 face is 60° from its neighbours; render them at slightly larger scale
                        // and ~11% opacity to simulate seeing through the dark fluid.
                        if (state.dieScale > 0.05f) {
                            val faceAlpha = (state.dieScale * 28f).toInt().coerceAtMost(28)
                            val facePaint = Paint().apply { alpha = faceAlpha }

                            canvas.save()
                            canvas.translate(cx, cy)
                            canvas.rotate(state.dieAngle + 60f)
                            canvas.scale(baseScale * 1.25f, baseScale * 1.25f)
                            canvas.nativeCanvas.drawBitmap(andBmp, -hw, -hh, facePaint)
                            canvas.restore()

                            canvas.save()
                            canvas.translate(cx, cy)
                            canvas.rotate(state.dieAngle - 60f)
                            canvas.scale(baseScale * 1.20f, baseScale * 1.20f)
                            canvas.nativeCanvas.drawBitmap(andBmp, -hw, -hh, facePaint)
                            canvas.restore()
                        }

                        // --- Main face ---
                        val mainPaint = Paint().apply {
                            alpha = (state.dieScale.coerceIn(0f, 1f) * 255f).toInt()
                        }
                        canvas.save()
                        canvas.translate(cx, cy)
                        canvas.rotate(state.dieAngle)
                        canvas.scale(baseScale, baseScale)
                        canvas.nativeCanvas.drawBitmap(andBmp, -hw, -hh, mainPaint)
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

// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.TopControls

private const val ROUTE_HATER = "hater"
private const val ROUTE_ALIGN = "align"

/**
 * The main screen for "Hater Mode".
 *
 * Simulates a Magic-8-Ball experience where a 20-sided die floats in liquid.
 * Uses [AzHostActivityLayout] (via [AzNavRailMenu]) as the top-level container with a
 * [NavHost] for routing compliance. "Align" navigates back immediately since it is not
 * applicable in this mode.
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

    val particleColor = remember { Color(0xAA013FE8) }

    AzNavRailMenu(
        uiState = uiState,
        onEvent = onEvent,
        navController = navController,
        currentDestination = currentRoute
    ) {
        NavHost(navController = navController, startDestination = ROUTE_HATER) {
            composable(ROUTE_HATER) {
                val context = LocalContext.current
                val bitmap = remember(state.answerResId) {
                    BitmapFactory.decodeResource(context.resources, state.answerResId)
                        ?.asImageBitmap()
                }

                Box(modifier = Modifier.fillMaxSize()) {
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

                        state.particles.forEach { particleOffset ->
                            drawCircle(
                                color = particleColor,
                                radius = 4.dp.toPx(),
                                center = Offset(centerX + particleOffset.x, centerY + particleOffset.y)
                            )
                        }

                        bitmap?.let {
                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(
                                    centerX + state.diePosition.x,
                                    centerY + state.diePosition.y
                                )
                                canvas.rotate(state.dieAngle)
                                val halfW = it.width / 2f
                                val halfH = it.height / 2f
                                val targetSize = minOf(size.width, size.height) * 0.55f
                                val scale = targetSize / maxOf(it.width, it.height)
                                canvas.scale(scale, scale)
                                canvas.nativeCanvas.drawBitmap(
                                    it.asAndroidBitmap(),
                                    -halfW, -halfH,
                                    null
                                )
                                canvas.restore()
                            }
                        }
                    }

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

            // "align" is not applicable in Hater mode — navigate back immediately.
            composable(ROUTE_ALIGN) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}

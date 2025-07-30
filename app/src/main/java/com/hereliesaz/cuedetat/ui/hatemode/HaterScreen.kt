// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.BlurMaskFilter
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.ExpressiveNavigationRail
import com.hereliesaz.cuedetat.ui.composables.MenuDrawerContent
import com.hereliesaz.cuedetat.ui.composables.TopControls

@Composable
fun HaterScreen(
    haterViewModel: HaterViewModel,
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    val state by haterViewModel.haterState.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        haterViewModel.onEvent(HaterEvent.EnterHaterMode)
    }

    val dieAnimationProgress by animateFloatAsState(
        targetValue = when (state.triangleState) {
            TriangleState.SUBMERGING -> 0f
            TriangleState.EMERGING -> 1f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 1000),
        label = "Die Animation"
    )

    val glowPaint = remember {
        Paint().asFrameworkPaint().apply {
            color = Color(0x993366FF).toArgb()
            maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val trianglePaint = remember { Paint().apply { color = Color(0xFF3366FF) } }
    val textPaint = remember {
        TextPaint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        down.consume()
                        drag(down.id) { change ->
                            haterViewModel.onEvent(HaterEvent.Dragging(change.position - change.previousPosition))
                            change.consume()
                        }
                        haterViewModel.onEvent(HaterEvent.DragEnd)
                    }
                }
        ) {
            haterViewModel.setupBoundaries(size.width, size.height)
            haterViewModel.updateDieAndText(state.answer, density.density)

            val centerX = size.width / 2
            val centerY = size.height / 2

            drawRect(color = Color.Black)

            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(centerX, centerY)

                val dieX = state.diePosition.x
                val dieY = state.diePosition.y
                val dieAngle = state.dieAngle

                canvas.translate(dieX, dieY)
                canvas.rotate(dieAngle)
                canvas.scale(dieAnimationProgress, dieAnimationProgress)

                val text = state.answer
                val layoutWidth = (size.width * 0.7f).toInt()
                textPaint.textSize = 22 * density.fontScale * density.density

                val staticLayout =
                    StaticLayout.Builder
                        .obtain(text, 0, text.length, textPaint, layoutWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build()

                val textHeight = staticLayout.height.toFloat()

                val padding = 30 * density.density
                val triangleHeight = textHeight + padding
                val sideLength = (triangleHeight / (kotlin.math.sqrt(3.0) / 2.0)).toFloat()
                val triangleWidth = sideLength

                val topY = -(2.0 / 3.0) * triangleHeight
                val bottomY = (1.0 / 3.0) * triangleHeight
                val halfWidth = triangleWidth / 2.0

                val trianglePath = Path().apply {
                    moveTo(0f, topY.toFloat())
                    lineTo(-halfWidth.toFloat(), bottomY.toFloat())
                    lineTo(halfWidth.toFloat(), bottomY.toFloat())
                    close()
                }

                textPaint.alpha = (255 * dieAnimationProgress).toInt()

                canvas.nativeCanvas.drawPath(trianglePath.asAndroidPath(), glowPaint)
                canvas.drawPath(path = trianglePath, paint = trianglePaint)

                canvas.save()
                canvas.translate(-staticLayout.width / 2f, -staticLayout.height / 2f)
                staticLayout.draw(canvas.nativeCanvas)
                canvas.restore()

                canvas.restore()
            }
        }

        TopControls(
            uiState = uiState,
            onEvent = onEvent,
            onMenuClick = { onEvent(MainScreenEvent.ToggleMenu) }
        )

        ExpressiveNavigationRail(uiState = uiState, onEvent = onEvent)

        AnimatedVisibility(
            visible = uiState.isExpandedMenuVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onEvent(MainScreenEvent.ToggleExpandedMenu) }
            )
        }
        AnimatedVisibility(
            visible = uiState.isExpandedMenuVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut()
        ) {
            MenuDrawerContent(
                uiState = uiState,
                onEvent = onEvent,
                onCloseDrawer = { onEvent(MainScreenEvent.ToggleExpandedMenu) }
            )
        }
    }
}
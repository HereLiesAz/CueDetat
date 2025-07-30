// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.chaffic.geometry.Circle

@Composable
fun HaterScreen(viewModel: HaterViewModel) {
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    LaunchedEffect(Unit) {
        viewModel.setScreenSize(screenWidthDp, screenHeightDp)
        viewModel.onEvent(HaterEvent.EnterHaterMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.onEvent(HaterEvent.ScreenTapped) })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { viewModel.onEvent(HaterEvent.DragEnd) }
                ) { change, dragAmount ->
                    change.consume()
                    viewModel.onEvent(HaterEvent.Dragging(dragAmount))
                }
            }
    ) {
        state.bodies.forEach { body ->
            val userData = body.userData
            if (userData is Int) {
                val size = (body.fixtureList[0].shape as Circle).radius * 2
                AnimatedVisibility(
                    visible = state.isAnswerVisible,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    Image(
                        painter = painterResource(id = userData),
                        contentDescription = "Hater Response",
                        modifier = Modifier
                            .offset(
                                x = (body.position.x - size / 2).dp,
                                y = (body.position.y - size / 2).dp
                            )
                            .size(size.dp)
                            .rotate(Math.toDegrees(body.angle).toFloat())
                    )
                }
            }
        }
    }
}
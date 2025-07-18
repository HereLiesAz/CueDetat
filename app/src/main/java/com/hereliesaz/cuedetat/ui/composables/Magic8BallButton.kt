// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/Magic8BallButton.kt

package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.theme.Magic8BallBlue

@Composable
fun Magic8BallButton(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Use default ripple or none
                onClick = onClick
            )
            .drawBehind {
                val trianglePath = Path().apply {
                    val width = size.toPx()
                    val height = size.toPx()
                    moveTo(width * 0.5f, height * 0.2f)
                    lineTo(width * 0.8f, height * 0.8f)
                    lineTo(width * 0.2f, height * 0.8f)
                    close()
                }

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val paint = Paint().asFrameworkPaint()
                    paint.color = Magic8BallBlue.toArgb()
                    paint.setShadowLayer(
                        20f, 0f, 0f, Magic8BallBlue
                            .copy(alpha = 0.8f)
                            .toArgb()
                    )
                    nativeCanvas.drawPath(trianglePath.asAndroidPath(), paint)
                }

                drawPath(
                    path = trianglePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Magic8BallBlue.copy(alpha = 0.6f),
                            Magic8BallBlue.copy(alpha = 0.2f)
                        )
                    )
                )

                drawPath(
                    path = trianglePath,
                    color = Magic8BallBlue,
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.padding(bottom = (size.value * 0.1f).dp)) {
            content()
        }
    }
}
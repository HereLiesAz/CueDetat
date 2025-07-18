// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/Magic8BallButton.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.theme.OracleBlue
import com.hereliesaz.cuedetat.ui.theme.OracleGlow

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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                val trianglePath = Path().apply {
                    val width = size.toPx()
                    val height = size.toPx()
                    // Inverted and larger triangle
                    moveTo(width * 0.5f, height * 0.95f)
                    lineTo(width * 0.95f, height * 0.1f)
                    lineTo(width * 0.05f, height * 0.1f)
                    close()
                }

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val paint = Paint().asFrameworkPaint()
                    // Set up the glow layer
                    paint.setShadowLayer(
                        30f, 0f, 0f,
                        OracleGlow
                            .copy(alpha = 0.7f)
                            .toArgb()
                    )
                    // Draw the solid blue triangle onto the glow layer
                    paint.color = OracleBlue.toArgb()
                    nativeCanvas.drawPath(trianglePath.asAndroidPath(), paint)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Nudge the content up slightly to center it within the inverted triangle
        Box(modifier = Modifier.padding(bottom = (size.value * 0.1f).dp)) {
            content()
        }
    }
}
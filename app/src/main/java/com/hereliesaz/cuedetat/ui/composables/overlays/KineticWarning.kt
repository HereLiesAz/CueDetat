package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class)
@Composable
fun KineticWarningOverlay(text: String?, modifier: Modifier = Modifier) {
    // Generate a random alignment each time a new warning appears
    val randomAlignment = remember(text) {
        if (text == null) Alignment.Center else
            BiasAlignment(
                horizontalBias = Random.nextDouble(-0.5, 0.5).toFloat(),
                verticalBias = Random.nextDouble(-0.7, 0.7).toFloat()
            )
    }

    AnimatedVisibility(
        visible = text != null,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        // This Box is now the root of the visible content. It does not fill the
        // screen and therefore cannot block touches meant for what's underneath.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = randomAlignment
        ) {
            if (text != null) {
                Column(
                    horizontalAlignment = Alignment.Start, // Align column content to the left
                    verticalArrangement = Arrangement.Center
                ) {
                    val words = text.split(" ")
                    val textMeasurer = rememberTextMeasurer()
                    val style = MaterialTheme.typography.displayLarge.copy(
                        textAlign = TextAlign.Start, // Align text to the left
                        color = WarningRed.copy(alpha = 0.85f)
                    )
                    val screenWidthPx = with(LocalDensity.current) {
                        (LocalResources.current.displayMetrics.widthPixels - 64.dp.toPx())
                    }

                    var currentLineWords = mutableListOf<String>()
                    words.forEach { word ->
                        val testLine = (currentLineWords + word).joinToString(" ")
                        val textLayoutResult = textMeasurer.measure(
                            text = AnnotatedString(testLine),
                            style = style.copy(fontSize = 100.sp)
                        )

                        if (textLayoutResult.size.width > screenWidthPx && currentLineWords.isNotEmpty()) {
                            KineticLine(
                                text = currentLineWords.joinToString(" "),
                                style = style,
                                screenWidthPx = screenWidthPx
                            )
                            currentLineWords = mutableListOf(word)
                        } else {
                            currentLineWords.add(word)
                        }
                    }
                    if (currentLineWords.isNotEmpty()) {
                        KineticLine(
                            text = currentLineWords.joinToString(" "),
                            style = style,
                            screenWidthPx = screenWidthPx
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KineticLine(text: String, style: TextStyle, screenWidthPx: Float) {
    var readyToDraw by remember { mutableStateOf(false) }
    var dynamicFontSize by remember { mutableStateOf(100.sp) }

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        style = style.copy(fontSize = dynamicFontSize),
        maxLines = 1,
        onTextLayout = { result: TextLayoutResult ->
            if (result.hasVisualOverflow && !readyToDraw) {
                val scaleFactor = screenWidthPx / result.size.width
                dynamicFontSize = (dynamicFontSize.value * scaleFactor * 0.95f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}
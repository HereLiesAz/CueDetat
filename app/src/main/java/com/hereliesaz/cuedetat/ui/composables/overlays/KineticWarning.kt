// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/KineticWarning.kt

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import kotlin.random.Random

/**
 * A dramatic, kinetic typography overlay for displaying warnings (e.g. "Impossible Shot").
 *
 * The text appears at random positions on the screen, split into lines that dynamically
 * resize to fill the width, creating a forceful visual impact.
 *
 * @param text The warning message to display. If null, the overlay is hidden.
 * @param modifier Styling modifier.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun KineticWarningOverlay(text: String?, modifier: Modifier = Modifier) {
    // Determine a random screen position for the text block each time the text changes.
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = randomAlignment
        ) {
            if (text != null) {
                val textMeasurer = rememberTextMeasurer()

                // Calculate base font size relative to screen width.
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val baseFontSize = (screenWidthDp / 8).coerceIn(32, 100)

                val style = MaterialTheme.typography.displayLarge.copy(
                    textAlign = TextAlign.Start,
                    color = WarningRed.copy(alpha = 0.85f)
                )

                // Note: padding(16.dp) on Box subtracts 32.dp total from available width
                val screenWidthPx = with(LocalDensity.current) {
                    (screenWidthDp.dp - 32.dp).toPx()
                }

                // Split text into lines within a remember block to prevent recalculating on recompose
                val lines = remember(text, screenWidthPx) {
                    val words = text.split(" ")
                    val resultLines = mutableListOf<String>()
                    var currentLineWords = mutableListOf<String>()

                    for (word in words) {
                        val testLine = (currentLineWords + listOf(word)).joinToString(" ")
                        val width = textMeasurer.measure(
                            text = AnnotatedString(testLine),
                            style = style.copy(fontSize = baseFontSize.sp),
                            maxLines = 1
                        ).size.width

                        // If it overflows and we have words, push the current line and start a new one
                        if (width > screenWidthPx && currentLineWords.isNotEmpty()) {
                            resultLines.add(currentLineWords.joinToString(" "))
                            currentLineWords = mutableListOf(word)
                        } else {
                            currentLineWords.add(word)
                        }
                    }
                    // Print remaining words.
                    if (currentLineWords.isNotEmpty()) {
                        resultLines.add(currentLineWords.joinToString(" "))
                    }

                    resultLines
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    lines.forEach { lineText ->
                        KineticLine(
                            text = lineText,
                            style = style,
                            screenWidthPx = screenWidthPx,
                            baseFontSize = baseFontSize.toFloat(),
                            textMeasurer = textMeasurer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a single line of text, dynamically scaling the font size down if it overflows.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun KineticLine(
    text: String,
    style: TextStyle,
    screenWidthPx: Float,
    baseFontSize: Float,
    textMeasurer: TextMeasurer
) {
    // Pre-calculate scaling to ensure no visual overflow or UI thrashing
    val dynamicFontSize = remember(text, screenWidthPx, baseFontSize) {
        val unconstrainedWidth = textMeasurer.measure(
            text = AnnotatedString(text),
            style = style.copy(fontSize = baseFontSize.sp),
            constraints = Constraints(), // Measure with infinite bounds to get true width
            maxLines = 1
        ).size.width

        if (unconstrainedWidth > screenWidthPx && unconstrainedWidth > 0) {
            val scaleFactor = screenWidthPx / unconstrainedWidth.toFloat()
            (baseFontSize * scaleFactor * 0.95f).sp // Scale down with a 5% safety margin
        } else {
            baseFontSize.sp
        }
    }

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        style = style.copy(fontSize = dynamicFontSize),
        maxLines = 1,
        softWrap = false
    )
}
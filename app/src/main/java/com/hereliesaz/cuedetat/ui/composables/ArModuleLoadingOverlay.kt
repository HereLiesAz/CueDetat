package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.ArModuleState
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Foreground overlay shown while the on-demand Expert-AR module is being
 * downloaded/loaded, or after a load failure. Renders nothing in the IDLE/READY
 * states, so it can be placed unconditionally in the AR overlay layer.
 *
 * While visible it draws a full-screen scrim that consumes all pointer input, so
 * the user can't interact with the scan/AR UI underneath while the module is
 * preparing or has failed — the only way forward is Retry or Cancel.
 *
 * Driven by [CueDetatState.arModuleState], which MainViewModel advances as it runs
 * `arController.ensureLoaded()` on first AR entry.
 */
@Composable
fun ArModuleLoadingOverlay(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val state = uiState.arModuleState
    if (state != ArModuleState.LOADING && state != ArModuleState.FAILED) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            // Swallow every pointer event so taps/drags don't reach the UI below.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ArModuleState.LOADING -> {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Preparing Expert AR…",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Downloading the AR table-scan module. This happens once.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            ArModuleState.FAILED -> {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Couldn’t load Expert AR",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "The AR module couldn’t be downloaded. Check your connection and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onEvent(MainScreenEvent.CancelArSetup) }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onEvent(MainScreenEvent.RetryArModuleLoad) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            ArModuleState.IDLE, ArModuleState.READY -> { /* unreachable: early-returned above */ }
        }
    }
}

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.data.MetaConnectionStatus
import com.hereliesaz.cuedetat.data.MetaWearableRepository

@Composable
fun MetaWearableBackground(
    modifier: Modifier = Modifier,
    metaWearableRepository: MetaWearableRepository
) {
    val videoFrame by metaWearableRepository.videoFrame.collectAsState()
    val status by metaWearableRepository.connectionStatus.collectAsState()
    val lastError by metaWearableRepository.lastError.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        val frame = videoFrame
        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Meta Glasses Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // No frame yet — give the user visible feedback instead of a blank screen
            // so the "glasses" toggle never appears to do nothing.
            val message = when (status) {
                MetaConnectionStatus.CONNECTING -> "Connecting to glasses…"
                MetaConnectionStatus.STREAMING -> "Waiting for video…"
                MetaConnectionStatus.NO_DEVICE ->
                    "No Meta glasses found.\nPair them in the Meta AI app, then tap glasses again."
                MetaConnectionStatus.ERROR ->
                    "Couldn't connect to glasses.${lastError?.let { "\n$it" } ?: ""}"
                MetaConnectionStatus.IDLE -> "Starting glasses…"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

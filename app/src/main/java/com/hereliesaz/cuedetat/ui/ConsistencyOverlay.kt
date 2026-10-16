package com.hereliesaz.cuedetat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.WearableState

@Composable
fun ConsistencyOverlay(
    wearableState: WearableState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = wearableState.isConnected,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.consistency_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            // Fluidity / Consistency Score: shown only once a stroke has been scored.
            val score = wearableState.consistencyScore
            if (score != null) Row(verticalAlignment = Alignment.CenterVertically) {
                val scorePercent = (score * 100).toInt()
                val scoreColor = when {
                    scorePercent > 80 -> Color.Green
                    scorePercent > 50 -> Color.Yellow
                    else -> Color.Red
                }
                Text(
                    text = stringResource(R.string.consistency_fluidity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = "$scorePercent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scoreColor
                )
            }

            // Shaking / HR Warning
            if (wearableState.isShaking) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null, // the adjacent text says it
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.consistency_erratic),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }

            // HR reading: null until the watch sends one, and then nothing is shown.
            val heartRate = wearableState.heartRate
            if (heartRate != null) {
                Text(
                    text = stringResource(R.string.consistency_heart_rate, heartRate.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

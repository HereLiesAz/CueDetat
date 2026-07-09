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
import androidx.compose.ui.res.painterResource
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
                text = "Consistency Trainer",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            // Fluidity / Consistency Score
            Row(verticalAlignment = Alignment.CenterVertically) {
                val scorePercent = (wearableState.consistencyScore * 100).toInt()
                val scoreColor = when {
                    scorePercent > 80 -> Color.Green
                    scorePercent > 50 -> Color.Yellow
                    else -> Color.Red
                }
                Text(
                    text = "Fluidity: ",
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
                        contentDescription = "Warning",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Erratic Motion Detected!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }

            // HR reading
            Text(
                text = "HR: ${wearableState.heartRate.toInt()} BPM",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

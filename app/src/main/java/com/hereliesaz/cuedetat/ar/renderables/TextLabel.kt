package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.xr.compose.ar.Billboard
import androidx.xr.core.Pose

@Composable
fun TextLabel(pose: Pose, text: String, color: Color = Color.White) {
    Billboard(pose = pose, pivot = floatArrayOf(0.5f, 1.0f)) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
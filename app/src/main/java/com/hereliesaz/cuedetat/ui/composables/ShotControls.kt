package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun ShotControls(
    power: Float,
    spin: Offset,
    onPowerChange: (Float) -> Unit,
    onSpinChange: (Offset) -> Unit,
    onExecuteShot: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Power: ${power.toInt()}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = power,
                onValueChange = onPowerChange,
                valueRange = 0f..100f
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Spin", style = MaterialTheme.typography.bodyLarge)
            SpinControl(
                spinOffset = spin,
                onSpinChanged = onSpinChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onExecuteShot) {
                Text("Shoot")
            }
        }
    }
}
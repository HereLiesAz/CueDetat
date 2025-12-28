package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to Use Cue D'état AR", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("Initial Setup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("1. Move your phone around to detect a flat surface.\n2. When prompted, tap the 'Place Table Here' button.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))

                Text("Placing & Moving Balls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("1. Tap on the green table surface to place the Cue Ball, then tap again to place the Object Ball.\n2. To move a ball, simply tap its new location on the table.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))

                Text("Shot Types", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("• Use the controls at the bottom to switch between Cut, Bank, and Kick shots.\n• In Bank/Kick mode, use the slider to rotate the table and buttons to select a rail.\n• In Cut mode, use the 2D pad to apply spin and see its effect.", style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!", style = MaterialTheme.typography.bodyLarge)
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}
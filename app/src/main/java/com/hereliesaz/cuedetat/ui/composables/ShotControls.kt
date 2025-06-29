package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ui.state.ShotType

@Composable
fun ShotControls(
    selectedShotType: ShotType,
    onShotTypeSelect: (ShotType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ShotTypeButton("Cut", selectedShotType == ShotType.CUT) { onShotTypeSelect(ShotType.CUT) }
        ShotTypeButton("Bank", selectedShotType == ShotType.BANK) { onShotTypeSelect(ShotType.BANK) }
        ShotTypeButton("Kick", selectedShotType == ShotType.KICK) { onShotTypeSelect(ShotType.KICK) }
        ShotTypeButton("Jump", selectedShotType == ShotType.JUMP) { onShotTypeSelect(ShotType.JUMP) }
        ShotTypeButton("Massé", selectedShotType == ShotType.MASSE) { onShotTypeSelect(ShotType.MASSE) }
    }
}

@Composable
private fun ShotTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text)
    }
}

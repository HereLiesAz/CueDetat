package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.Rail
import com.hereliesaz.cuedetat.ui.ShotType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotTypeSelector(selectedType: ShotType, onTypeSelected: (ShotType) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            ShotType.entries.forEach { type ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.shape,
                    onClick = { onTypeSelected(type) },
                    selected = type == selectedType
                ) {
                    Text(type.name)
                }
            }
        }
    }
}

@Composable
fun RailSelectionControls(selectedRail: Rail, onRailSelected: (Rail) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_rail_select), contentDescription = "Select Rail")
            Rail.entries.forEach { rail ->
                val isSelected = selectedRail == rail
                OutlinedButton(
                    onClick = { onRailSelected(rail) },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(rail.name.first().toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TopControls(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 64.dp)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.CenterStart
        ) {
            if (uiState.areHelpersVisible) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "Menu",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }
        }

        if(uiState.showTable || uiState.isBankingMode) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.clickable { onEvent(MainScreenEvent.CycleTableSize) }
            ) {
                Text(
                    text = "Table Size",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${uiState.tableSize.feet}'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}
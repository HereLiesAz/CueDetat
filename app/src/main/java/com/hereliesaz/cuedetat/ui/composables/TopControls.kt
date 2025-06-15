package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TopControls(
    uiState: OverlayState,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.Start,
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
                    color = AccentGold,
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
    }
}
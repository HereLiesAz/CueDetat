// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ExpressiveNavigationRail.kt
// Test comment
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent

@Composable
fun ExpressiveNavigationRail(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val isExpanded = uiState.isNavigationRailExpanded

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    NavigationRail(
        modifier = Modifier.width(railWidth),
        containerColor = if (isExpanded) MaterialTheme.colorScheme.surface else Color.Transparent,
        header = {
            IconButton(
                onClick = { onEvent(MainScreenEvent.ToggleNavigationRail) },
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_cue_detat),
                    contentDescription = "Cuedetat Logo",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    ) {
        if (isExpanded) {
            MenuDrawerContent(
                uiState = uiState,
                onEvent = onEvent,
                onCloseDrawer = { onEvent(MainScreenEvent.ToggleNavigationRail) }
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                CuedetatButton(
                    onClick = { onEvent(MainScreenEvent.ToggleHelp) },
                    text = "Help"
                )

                CuedetatButton(
                    onClick = { onEvent(MainScreenEvent.ToggleSpinControl) },
                    text = "Spin"
                )

                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    CuedetatButton(
                        onClick = { onEvent(MainScreenEvent.ToggleBankingMode) },
                        text = "Bank"
                    )
                }

                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    CuedetatButton(
                        onClick = { onEvent(MainScreenEvent.AddObstacleBall) },
                        text = "Add"
                    )
                }

                val (text, event) = when {
                    uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "Unlock" to MainScreenEvent.UnlockBeginnerView
                    uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> "Lock" to MainScreenEvent.LockBeginnerView
                    else -> "Reset" to MainScreenEvent.Reset
                }
                CuedetatButton(
                    onClick = { onEvent(event) },
                    text = text
                )

                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    CuedetatButton(
                        onClick = { onEvent(MainScreenEvent.ToggleCamera) },
                        text = "Cam"
                    )
                }
            }
        }
    }
}
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Help
                NavigationRailItem(
                    selected = false,
                    onClick = { onEvent(MainScreenEvent.ToggleHelp) },
                    icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Help, contentDescription = "Help") }
                )

                // Spin Control
                NavigationRailItem(
                    selected = uiState.isSpinControlVisible,
                    onClick = { onEvent(MainScreenEvent.ToggleSpinControl) },
                    icon = { Icon(imageVector = Icons.Default.Timeline, contentDescription = "Spin Control") }
                )

                // Banking Mode
                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    NavigationRailItem(
                        selected = uiState.isBankingMode,
                        onClick = { onEvent(MainScreenEvent.ToggleBankingMode) },
                        icon = { Icon(imageVector = Icons.Default.Wallet, contentDescription = "Banking Mode") }
                    )
                }

                // Add Obstacle
                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    NavigationRailItem(
                        selected = false,
                        onClick = { onEvent(MainScreenEvent.AddObstacleBall) },
                        icon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Obstacle") }
                    )
                }

                // Reset/Lock/Unlock View
                val (icon, event) = when {
                    uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> Icons.Default.LockOpen to MainScreenEvent.UnlockBeginnerView
                    uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> Icons.Default.Lock to MainScreenEvent.LockBeginnerView
                    else -> Icons.Default.Refresh to MainScreenEvent.Reset
                }
                NavigationRailItem(
                    selected = false,
                    onClick = { onEvent(event) },
                    icon = { Icon(imageVector = icon, contentDescription = "View Lock") }
                )

                // Camera
                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    NavigationRailItem(
                        selected = uiState.isCameraVisible,
                        onClick = { onEvent(MainScreenEvent.ToggleCamera) },
                        icon = { Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera") }
                    )
                }
            }
        }
    }
}
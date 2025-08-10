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
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Menu
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
import com.hereliesaz.cuedetat.domain.MainScreenEvent

@Composable
fun ExpressiveNavigationRail(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val railItems = remember {
        listOf(
            "Help" to MainScreenEvent.StartTutorial,
            "Spin" to MainScreenEvent.ToggleSpinControl,
            "Bank" to MainScreenEvent.ToggleBankingMode,
            "Menu" to MainScreenEvent.ToggleExpandedMenu
        )
    }

    val isExpanded = uiState.isNavigationRailExpanded

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 256.dp else 80.dp,
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
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            railItems.forEach { (label, event) ->
                NavigationRailItem(
                    selected = false,
                    onClick = { onEvent(event) },
                    icon = {
                        val icon = when (label) {
                            "Help" -> Icons.Default.Help
                            "Spin" -> Icons.Default.Timeline
                            "Bank" -> Icons.Default.Wallet
                            "Menu" -> Icons.Default.Menu
                            else -> Icons.Default.Help
                        }
                        Icon(imageVector = icon, contentDescription = label)
                    },
                    label = { if (isExpanded) Text(label) },
                    alwaysShowLabel = isExpanded
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
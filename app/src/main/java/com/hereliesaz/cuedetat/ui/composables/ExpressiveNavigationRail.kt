// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ExpressiveNavigationRail.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

@Composable
fun ExpressiveNavigationRail(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    // A curated list of items for the collapsed rail
    val railItems = remember {
        listOf(
            "Help" to MainScreenEvent.StartTutorial,
            "Spin" to MainScreenEvent.ToggleSpinControl,
            "Bank" to MainScreenEvent.ToggleBankingMode,
            "Menu" to MainScreenEvent.ToggleExpandedMenu
        )
    }

    val railBackgroundColor by animateColorAsState(
        targetValue = if (uiState.isMenuVisible) Color.Black.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "RailBackgroundColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = uiState.isMenuVisible,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            // Scrim to dismiss the menu
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onEvent(MainScreenEvent.ToggleMenu) }
            )
        }

        AnimatedVisibility(
            visible = uiState.isMenuVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            NavigationRail(
                containerColor = railBackgroundColor,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 80.dp)
            ) {
                railItems.forEach { (label, event) ->
                    NavigationRailItem(
                        selected = false, // Rail items are actions, not destinations
                        onClick = {
                            onEvent(event)
                            // Keep the rail open unless the user explicitly closes it,
                            // unless opening the full menu.
                            if (event is MainScreenEvent.ToggleExpandedMenu) {
                                onEvent(MainScreenEvent.ToggleMenu)
                            }
                        },
                        icon = {
                            CuedetatButton(
                                onClick = {
                                    onEvent(event)
                                    if (event is MainScreenEvent.ToggleExpandedMenu) {
                                        onEvent(MainScreenEvent.ToggleMenu)
                                    }
                                },
                                text = label,
                                size = 64.dp // Smaller buttons for the rail
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationRailItemDefaults.colors(indicatorColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun MenuDrawerContent(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))

        MenuItem(
            icon = ImageVector.vectorResource(R.drawable.ic_help_outline_24),
            text = stringResource(if (uiState.areHelpersVisible) R.string.hide_helpers else R.string.show_helpers),
            onClick = { onEvent(MainScreenEvent.ToggleHelp); onCloseDrawer() }
        )
        MenuItem(
            icon = Icons.Outlined.School,
            text = "Show Tutorial",
            onClick = { onEvent(MainScreenEvent.StartTutorial); onCloseDrawer() }
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline
        )

        val bankingModeToggleText =
            if (uiState.isBankingMode) "Ghost Ball Aiming" else "Calculate Bank"
        MenuItem(
            icon = Icons.Outlined.ViewInAr,
            text = bankingModeToggleText,
            onClick = { onEvent(MainScreenEvent.ToggleBankingMode); onCloseDrawer() }
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline
        )

        val systemIsCurrentlyDark = isSystemInDarkTheme()
        val (themeToggleText, themeToggleIcon) = when (uiState.isForceLightMode) {
            true -> "Embrace the Darkness" to Icons.Outlined.Nightlight
            false -> "Use System Theme" to Icons.Outlined.BrightnessMedium
            null -> if (systemIsCurrentlyDark) "Walk toward the Light" to Icons.Outlined.LightMode else "Embrace the Darkness" to Icons.Outlined.Nightlight
        }
        MenuItem(
            icon = themeToggleIcon,
            text = themeToggleText,
            onClick = { onEvent(MainScreenEvent.ToggleForceTheme); onCloseDrawer() }
        )
        MenuItem(
            icon = Icons.Outlined.BrightnessMedium,
            text = "Luminance",
            onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog); onCloseDrawer() }
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline
        )

        MenuItem(
            icon = Icons.Outlined.Brush,
            text = "About Me",
            onClick = { onEvent(MainScreenEvent.ViewArt); onCloseDrawer() })
        MenuItem(
            icon = Icons.Outlined.MonetizationOn,
            text = "Chalk Your Tip",
            onClick = { onEvent(MainScreenEvent.ShowDonationOptions); onCloseDrawer() })
        MenuItem(
            icon = Icons.Outlined.SystemUpdate,
            text = "Check for Updates",
            onClick = { onEvent(MainScreenEvent.CheckForUpdate); onCloseDrawer() })
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, contentDescription = text, modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/sliders/LockViewButton.kt

package com.hereliesaz.cuedetat.ui.composables.sliders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Lock View: freezes every sensor-driven transform of the table (SensorLock) while leaving manual
 * moves and sliders live. Sits at the bottom-right beside the rotation swiper.
 *
 * @param isLocked Current CueDetatState.isViewLocked.
 * @param onToggle Called on tap (MainScreenEvent.ToggleViewLock).
 */
@Composable
fun LockViewButton(
    isLocked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedIconToggleButton(
        checked = isLocked,
        onCheckedChange = { onToggle() },
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = if (isLocked) "Unlock view" else "Lock view",
            tint = if (isLocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        )
    }
}

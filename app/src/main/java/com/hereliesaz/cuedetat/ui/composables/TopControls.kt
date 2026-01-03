package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.view.state.DistanceUnit

@Composable
fun TopControls(
    areHelpersVisible: Boolean,
    experienceMode: ExperienceMode?,
    isTableVisible: Boolean,
    tableSizeFeet: Float,
    isBeginnerViewLocked: Boolean,
    targetBallDistance: Float,
    distanceUnit: DistanceUnit,
    onCycleTableSize: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stub implementation
}

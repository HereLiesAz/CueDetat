// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/TopControls.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.view.state.DistanceUnit

/**
 * The top bar UI containing Contextual Status Info (table size, distance).
 *
 * @param experienceMode Current experience mode (affects visibility of controls).
 * @param isTableVisible Whether the table is visible (affects table size display).
 * @param tableSizeFeet Current table size in feet.
 * @param isBeginnerViewLocked Whether the camera view is locked (Beginner mode).
 * @param targetBallDistance Distance to the target ball.
 * @param distanceUnit Unit of measurement (Metric/Imperial).
 * @param onCycleTableSize Callback to cycle table size.
 * @param modifier Styling modifier.
 */
@Composable
fun TopControls(
    experienceMode: ExperienceMode?,
    isTableVisible: Boolean,
    tableSizeFeet: Int,
    isBeginnerViewLocked: Boolean,
    targetBallDistance: Float,
    distanceUnit: DistanceUnit,
    onCycleTableSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding() // Avoid overlapping system status bar.
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        // --- Right Side: Contextual Status (Table Size, Distance) ---
        if (experienceMode != ExperienceMode.HATER) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Table Size Indicator (Clickable to change).
                if (isTableVisible) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clickable(
                                onClickLabel = "Change Table Size",
                                role = Role.Button,
                                onClick = onCycleTableSize
                            )
                            .semantics {
                                contentDescription = "Current table size: ${tableSizeFeet} feet. Double tap to cycle."
                            }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Table Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${tableSizeFeet}'",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Distance Indicator.
                if (experienceMode != ExperienceMode.BEGINNER || !isBeginnerViewLocked) {
                    Column(horizontalAlignment = Alignment.End) {
                        val distanceText = if (targetBallDistance > 0) {
                            if (distanceUnit == DistanceUnit.IMPERIAL) {
                                val feet = (targetBallDistance / 12).toInt()
                                val inches = (targetBallDistance % 12).toInt()
                                "$feet ft $inches in"
                            } else {
                                val cm = (targetBallDistance * 2.54).toInt()
                                "$cm cm"
                            }
                        } else {
                            "--"
                        }

                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

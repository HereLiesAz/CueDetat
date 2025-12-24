package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.view.state.DistanceUnit

/**
 * Renders the top control bar, showing app name or menu button and status information.
 *
 * PERFORMANCE OPTIMIZATION:
 * This composable uses primitive parameters instead of the monolithic `CueDetatState`.
 * Jetpack Compose can skip recomposition if the primitive parameters haven't changed.
 * Passing the entire state object would cause this to recompose on *any* state change,
 * even if it didn't affect the top controls (e.g., sensor updates, CV data).
 */
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .semantics {
                    role = Role.Button
                    contentDescription = "Open Menu"
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        if (dragAmount.y > 2.0f) {
                            onMenuClick()
                            change.consume()
                        }
                    }
                }
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.CenterStart
        ) {
            if (areHelpersVisible && experienceMode != ExperienceMode.HATER) {
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start
                    )
                    // Note: R.string.tagline must be defined in strings.xml
                    Text(
                        text = stringResource(id = R.string.app_name), // Fallback if tagline missing
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = null, // Handled by parent semantics
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }
        }

        if (experienceMode != ExperienceMode.HATER) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isTableVisible) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .semantics {
                                role = Role.Button
                                contentDescription = "Change Table Size"
                            }
                            .clickable { onEvent(MainScreenEvent.CycleTableSize) }
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

package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.ar.Plane
import androidx.xr.compose.material3.Box
import androidx.xr.compose.material3.rememberMaterial
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.ar.ARConstants

private val BED_COLOR = Color(0.0f, 0.4f, 0.15f, 1.0f)
private val RAIL_COLOR = Color(0.35f, 0.2f, 0.05f, 1.0f)

@Composable
fun Table(pose: Pose, onPlaneTap: (Pose) -> Unit) {
    val bedMaterial = rememberMaterial(color = BED_COLOR)
    val railMaterial = rememberMaterial(color = RAIL_COLOR)

    // A plane that sits on the floor, aligned with the table bed. This is what the user taps to place balls.
    Plane(
        pose = pose,
        size = Pair(ARConstants.TABLE_WIDTH.dp, ARConstants.TABLE_DEPTH.dp),
        material = rememberMaterial(color = Color.Transparent),
        onTap = { onPlaneTap(it.pose) }
    )

    // The visible table bed, slightly above the tap plane
    Box(
        pose = pose.copy(translation = pose.translation + floatArrayOf(0f, 0.005f, 0f)),
        size = Triple(ARConstants.TABLE_WIDTH.dp, 0.01.dp, ARConstants.TABLE_DEPTH.dp),
        material = bedMaterial
    )

    val halfDepth = ARConstants.TABLE_DEPTH / 2f
    val halfWidth = ARConstants.TABLE_WIDTH / 2f
    val railYPos = ARConstants.RAIL_HEIGHT / 2f + 0.005f

    // Rails
    Box(
        pose = pose.copy(translation = pose.translation + floatArrayOf(0f, railYPos, -halfDepth - (ARConstants.RAIL_WIDTH / 2.0f))),
        size = Triple((ARConstants.TABLE_WIDTH + 2 * ARConstants.RAIL_WIDTH).dp, ARConstants.RAIL_HEIGHT.dp, ARConstants.RAIL_WIDTH.dp),
        material = railMaterial
    )
    Box(
        pose = pose.copy(translation = pose.translation + floatArrayOf(0f, railYPos, halfDepth + (ARConstants.RAIL_WIDTH / 2.0f))),
        size = Triple((ARConstants.TABLE_WIDTH + 2 * ARConstants.RAIL_WIDTH).dp, ARConstants.RAIL_HEIGHT.dp, ARConstants.RAIL_WIDTH.dp),
        material = railMaterial
    )
    Box(
        pose = pose.copy(translation = pose.translation + floatArrayOf(-halfWidth - (ARConstants.RAIL_WIDTH / 2.0f), railYPos, 0f)),
        size = Triple(ARConstants.RAIL_WIDTH.dp, ARConstants.RAIL_HEIGHT.dp, ARConstants.TABLE_DEPTH.dp),
        material = railMaterial
    )
    Box(
        pose = pose.copy(translation = pose.translation + floatArrayOf(halfWidth + (ARConstants.RAIL_WIDTH / 2.0f), railYPos, 0f)),
        size = Triple(ARConstants.RAIL_WIDTH.dp, ARConstants.RAIL_HEIGHT.dp, ARConstants.TABLE_DEPTH.dp),
        material = railMaterial
    )
}
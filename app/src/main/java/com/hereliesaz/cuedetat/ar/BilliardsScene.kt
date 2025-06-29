package com.hereliesaz.cuedetat.ar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.Tappable
import androidx.xr.compose.TrackedPlane
import com.google.ar.core.HitResult
import com.hereliesaz.cuedetat.ar.entity.RenderableEntity
import com.hereliesaz.cuedetat.ar.renderables.Ball
import com.hereliesaz.cuedetat.ar.renderables.TableNode
import com.hereliesaz.cuedetat.ui.state.UiState

@Composable
fun BilliardsScene(
    uiState: UiState,
    onTap: (HitResult) -> Unit
) {
    val context = LocalContext.current

    // Detect horizontal planes and make them tappable.
    TrackedPlane(
        onTap = onTap,
        modifier = Tappable()
    )

    // Render the table if its state exists
    uiState.table?.let { tableState ->
        RenderableEntity(pose = tableState.pose) {
            remember { TableNode(context) }
        }
    }

    // Render the cue ball if its state exists
    uiState.cueBall?.let { ballState ->
        RenderableEntity(pose = ballState.pose) {
            remember { Ball(context, color = floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f)) }
        }
    }

    // Render the object ball if its state exists
    uiState.objectBall?.let { ballState ->
        RenderableEntity(pose = ballState.pose) {
            remember { Ball(context, color = floatArrayOf(0.8f, 0.2f, 0.2f, 1.0f)) }
        }
    }
}
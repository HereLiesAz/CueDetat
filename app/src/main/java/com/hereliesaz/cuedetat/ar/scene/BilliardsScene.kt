package com.hereliesaz.cuedetat.ar.scene

import androidx.compose.runtime.Composable
import androidx.xr.compose.spatial.Subspace
import com.hereliesaz.cuedetat.ui.state.UiState

@Composable
fun BilliardsScene(uiState: UiState) {
    Subspace {
        // The table will be placed at the anchor point
        uiState.table?.let { tableState ->
            Table(tableState)
        }

        // The cue ball and object ball will be placed relative to the table
        uiState.cueBall?.let { cueBallState ->
            Ball(cueBallState)
        }
        uiState.objectBall?.let { objectBallState ->
            Ball(objectBallState)
        }
    }
}
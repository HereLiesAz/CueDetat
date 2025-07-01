// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/domain/UpdateStateUseCase.kt
package com.hereliesaz.cuedetatlite.domain

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.model.Perspective
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class UpdateStateUseCase(private val warningManager: WarningManager) {

    operator fun invoke(
        overlayState: OverlayState,
        perspective: Perspective,
        tableModel: TableModel?
    ): OverlayState {
        val (pitchMatrix, railPitchMatrix, inverseMatrix, hasInverse) = perspective

        val screenState = overlayState.screenState.copy(tableModel = tableModel)

        val updatedScreenState = if (screenState.isBankingMode && overlayState.bankingAimTarget != null) {
            val bankingPath = tableModel?.calculateBankingPath(
                screenState.protractorUnit.cueBall.logicalPosition,
                overlayState.bankingAimTarget
            ) ?: emptyList()
            screenState.copy(bankingPath = bankingPath)
        } else {
            screenState
        }

        val finalScreenState = updatedScreenState.copy(
            isImpossibleShot = isImpossibleShot(updatedScreenState),
            warningText = warningManager.getWarning(updatedScreenState)
        )

        return overlayState.copy(
            screenState = finalScreenState,
            pitchMatrix = pitchMatrix,
            railPitchMatrix = railPitchMatrix,
            inversePitchMatrix = inverseMatrix,
            hasInverseMatrix = hasInverse
        )
    }

    private fun isImpossibleShot(screenState: ScreenState): Boolean {
        val cuePos = screenState.protractorUnit.cueBall.logicalPosition
        val targetPos = screenState.protractorUnit.targetBall.logicalPosition
        val dx = targetPos.x - cuePos.x
        val dy = targetPos.y - cuePos.y
        val angle = atan2(dy, dx)
        val ghostCueX = targetPos.x + (screenState.protractorUnit.cueBall.radius + screenState.protractorUnit.targetBall.radius) * cos(angle)
        val ghostCueY = targetPos.y + (screenState.protractorUnit.cueBall.radius + screenState.protractorUnit.targetBall.radius) * sin(angle)

        return screenState.actualCueBall?.let {
            val actualDx = ghostCueX - it.logicalPosition.x
            val actualDy = ghostCueY - it.logicalPosition.y
            (dx * actualDx + dy * actualDy) < 0
        } ?: false
    }
}

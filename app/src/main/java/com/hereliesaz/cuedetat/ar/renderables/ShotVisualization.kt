package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ar.MathUtils.normalize
import com.hereliesaz.cuedetat.ar.MathUtils.toF3
import com.hereliesaz.cuedetat.ar.PhysicsUtil
import com.hereliesaz.cuedetat.ui.MainUiState
import com.hereliesaz.cuedetat.ui.Rail
import com.hereliesaz.cuedetat.ui.ShotType
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.ui.theme.RustedEmber
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

@Composable
fun ShotVisualization(
    uiState: MainUiState,
    cuePose: Pose,
    objPose: Pose
) {
    val cueF3 = cuePose.translation.toF3()
    val objF3 = objPose.translation.toF3()

    when (uiState.shotType) {
        ShotType.CUT -> CutShotLines(cueF3, objF3, uiState)
        ShotType.BANK -> BankShotLines(cueF3, objF3, uiState)
        ShotType.KICK -> KickShotLines(cueF3, objF3, uiState)
        ShotType.JUMP -> JumpShotLines(cueF3, objF3, uiState)
        ShotType.MASSE -> MasseShotLines(cueF3, objF3, uiState)
    }

    if (uiState.warningMessage != null) {
        TextLabel(pose = Pose(objF3 + Float3(y = 0.2f)), text = uiState.warningMessage, color = WarningRed)
    }
}

@Composable
private fun CutShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val squirtAngle = -uiState.spinOffset.x * 2.5f
    val squirtRotation = Quaternion.fromAxisAngle(Float3(y = 1f), squirtAngle)
    val initialAimVector = normalize(objF3 - cueF3)
    val deflectedAimVector = squirtRotation * initialAimVector

    val directionToObj = normalize(cueF3 - objF3)
    val ghostBallLocalPos = objF3 + directionToObj * ARConstants.BALL_DIAMETER
    Ball(pose = Pose(ghostBallLocalPos), color = Color.White, opacity = 0.5f)

    Line(start = cueF3, end = cueF3 + deflectedAimVector * 3f, color = AccentGold, thickness = 0.003f)

    val throwAngle = -uiState.spinOffset.x * 3.0f
    val throwRotation = Quaternion.fromAxisAngle(Float3(y = 1f), throwAngle)
    val initialObjTrajectory = normalize(objF3 - ghostBallLocalPos)
    val thrownObjTrajectory = throwRotation * initialObjTrajectory
    Line(start = objF3, end = objF3 + thrownObjTrajectory * 3f, color = RustedEmber)

    // Recommended spin indicator
    val cutAngle = com.hereliesaz.cuedetat.ar.MathUtils.angleBetween(initialAimVector, normalize(ghostBallLocalPos - objF3))
    val requiredSpinX = (cutAngle / 90f) * 0.5f
    SpinIndicator(cueBallPose = Pose(cueF3), spinOffset = androidx.compose.ui.geometry.Offset(x = requiredSpinX, y = 0f), color = Color.Cyan)

    // User's applied spin indicator
    if (uiState.spinOffset != androidx.compose.ui.geometry.Offset.Zero) {
        SpinIndicator(cueBallPose = Pose(cueF3), spinOffset = uiState.spinOffset, color = WarningRed)
    }
}

@Composable
private fun BankShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val (railAimPoint, _) = PhysicsUtil.calculateBank(objF3, cueF3, uiState.selectedRail) ?: return
    Line
    // Jump shot visualizes jumping OVER the object ball
    if (objF3 == null) return
    JumpShotLines(cueF3, objF3, uiState)
}
com.hereliesaz.cuedetat.ui.ShotType.MASSE -> {
    if (objF3 == null) return
    MasseShotLines(cueF3, objF3, uiState)
}
}

if (uiState.warningMessage != null) {
    val textPos = objF3 ?: cueF3
    TextLabel(pose = Pose(textPos + dev.romainguy.kotlin.math.Float3(y = 0.2f)), text = uiState.warningMessage, color = WarningRed)
}
}

@Composable
private fun CutShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val ghostBallPos = PhysicsUtil.calculateGhostBallPosition(cueF3, objF3)
    Ball(pose = Pose(ghostBallPos), color = Color.White, opacity = 0.5f)

    val initialAimVector = normalize(ghostBallPos - cueF3)
    val deflectedAimVector = PhysicsUtil.calculateSquirtedVector(initialAimVector, uiState.spinOffset)
    Line(start = cueF3, end = cueF3 + deflectedAimVector * 3f, color = AccentGold, thickness = 0.003f)

    val initialObjTrajectory = normalize(objF3 - ghostBallPos)
    val thrownObjTrajectory = PhysicsUtil.calculateThrownVector(initialObjTrajectory, uiState.spinOffset)
    Line(start = objF3, end = objF3 + thrownObjTrajectory * 3f, color = RustedEmber)

    val requiredSpinX = PhysicsUtil.angleBetween(initialAimVector, deflectedAimVector) / 2.5f // Simplified
    SpinIndicator(cueBallPose = Pose(cueF3), spinOffset = uiState.spinOffset, color = WarningRed)
    SpinIndicator(cueBallPose = Pose(cueF3), spinOffset = androidx.compose.ui.geometry.Offset(x = requiredSpinX, y = 0f), color = Color.Cyan)
}

@Composable
private fun BankShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val (railAimPoint, _) = PhysicsUtil.calculateBank(objF3, cueF3, uiState.selectedRail) ?: return
    Line(start = cueF3, end = railAimPoint, color = AccentGold, thickness = 0.003f)
    Line(start = railAimPoint, end = objF3, color = AccentGold, thickness = 0.003f)
    val trajectory = normalize(objF3 - railAimPoint) * 3f
    Line(start = objF3, end = objF3 + trajectory, color = RustedEmber)
    TextLabel(pose = Pose(railAimPoint + dev.romainguy.kotlin.math.Float3(y = 0.05f)), text = PhysicsUtil.getDiamondText(railAimPoint, uiState.selectedRail))
}

@Composable
private fun KickShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val (railAimPoint, mirroredTargetPos) = PhysicsUtil.calculateBank(objF3, cueF3, uiState.selectedRail) ?: return
    Ball(pose = Pose(mirroredTargetPos), color = Color.Yellow.copy(alpha = 0.4f))
    Line(start = cueF3, end = mirroredTargetPos, color = AccentGold.copy(alpha = 0.4f), thickness = 0.001f)
    Line(start = cueF3, end = railAimPoint, color = AccentGold, thickness = 0.003f)
    Line(start = railAimPoint, end = objF3, color = RustedEmber, thickness = 0.003f)
    TextLabel(pose = Pose(railAimPoint + dev.romainguy.kotlin.math.Float3(y = 0.05f)), text = PhysicsUtil.getDiamondText(railAimPoint, uiState.selectedRail))
}

@Composable
private fun JumpShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val arcPoints = PhysicsUtil.calculateJumpShotArc(cueF3, objF3, uiState.cueElevation)
    ArcLine(points = arcPoints, color = AccentGold)
}

@Composable
private fun MasseShotLines(cueF3: Float3, objF3: Float3, uiState: MainUiState) {
    val curvePoints = PhysicsUtil.calculateMasseCurve(cueF3, objF3, uiState.spinOffset, uiState.cueElevation)
    ArcLine(points = curvePoints, color = AccentGold)
}
package com.hereliesaz.cuedetat.ar.renderables

import com.google.ar.core.Pose
import com.hereliesaz.cuedetat.ar.ArConstants
import com.hereliesaz.cuedetat.ar.MathUtils
import com.hereliesaz.cuedetat.ar.normalize
import com.hereliesaz.cuedetat.ar.toF3
import dev.romainguy.kotlin.math.Float3

data class ShotData(
    val ghostBallPose: Pose? = null,
    val pocketingLinePoints: List<Float3> = emptyList(),
    val tangentLinePoints: List<Float3> = emptyList(),
    val cueBallPathPoints: List<Float3> = emptyList()
)

object ShotVisualization {

    fun calculateCutShot(cueBallPose: Pose, objectBallPose: Pose): ShotData {
        val cuePos = cueBallPose.translation.toF3()
        val objPos = objectBallPose.translation.toF3()

        val shotVector = normalize(objPos - cuePos)
        val ghostBallPos = objPos - shotVector * ArConstants.BALL_DIAMETER
        val ghostBallPose = Pose(ghostBallPos.toFloatArray(), cueBallPose.rotation)

        // Pocketing Line (defaulting to a straight line through nearest pocket)
        val pocket = ArConstants.POCKETS.minByOrNull { MathUtils.dot(it - objPos, it - objPos) } ?: objPos
        val pocketingLinePoints = listOf(objPos, pocket)

        // Tangent Line
        val tangentVector = normalize(Float3(-shotVector.z, 0f, shotVector.x))
        val tangentStart = objPos
        val tangentEnd = tangentStart + tangentVector * 1.0f
        val tangentLinePoints = listOf(tangentStart, tangentEnd)

        // Cue Ball Path
        val cuePathStart = ghostBallPos + shotVector * ArConstants.BALL_DIAMETER
        val cuePathEnd = cuePathStart + tangentVector * 1.0f
        val cueBallPathPoints = listOf(cuePathStart, cuePathEnd)

        return ShotData(
            ghostBallPose = ghostBallPose,
            pocketingLinePoints = pocketingLinePoints,
            tangentLinePoints = tangentLinePoints,
            cueBallPathPoints = cueBallPathPoints
        )
    }

    // Placeholder for other shot calculations
    fun calculateBankShot(cueBallPose: Pose, objectBallPose: Pose): ShotData {
        // Implement bank shot logic here
        return ShotData()
    }
}

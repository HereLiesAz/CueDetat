package com.hereliesaz.cuedetat.protractor.calculator

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.protractor.ProtractorState
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.pow // Import pow

data class ProjectedCoords(
    val targetProjected: PointF,
    val cueProjected: PointF,
    val targetScreenRadius: Float,
    val cueScreenRadius: Float
)

data class AimingLineLogicalCoords(
    val startX: Float, val startY: Float,
    val cueX: Float, val cueY: Float,
    val endX: Float, val endY: Float,
    val normDirX: Float, val normDirY: Float
)

data class DeflectionLineParams(
    val tBPMagnitude: Float,
    val unitVecX: Float,
    val unitVecY: Float,
    val drawLength: Float
)

class ProtractorGeometryCalculator(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {

    fun mapPoint(logicalPoint: PointF, matrixToUse: Matrix): PointF {
        val pointArray = floatArrayOf(logicalPoint.x, logicalPoint.y)
        matrixToUse.mapPoints(pointArray)
        return PointF(pointArray[0], pointArray[1])
    }

    // Using Doubles for intermediate pow calculations for precision, then casting to Float
    fun distance(p1: PointF, p2: PointF): Float {
        val dx = (p1.x - p2.x).toDouble()
        val dy = (p1.y - p2.y).toDouble()
        return sqrt(dx.pow(2) + dy.pow(2)).toFloat()
    }
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = (x1 - x2).toDouble()
        val dy = (y1 - y2).toDouble()
        return sqrt(dx.pow(2) + dy.pow(2)).toFloat()
    }


    fun calculateProjectedScreenData(state: ProtractorState): ProjectedCoords {
        val targetProjected = mapPoint(state.targetCircleCenter, state.mPitchMatrix)
        val cueProjected = mapPoint(state.cueCircleCenter, state.mPitchMatrix)

        val tL = mapPoint(PointF(state.targetCircleCenter.x - state.currentLogicalRadius, state.targetCircleCenter.y), state.mPitchMatrix)
        val tR = mapPoint(PointF(state.targetCircleCenter.x + state.currentLogicalRadius, state.targetCircleCenter.y), state.mPitchMatrix)
        val tT = mapPoint(PointF(state.targetCircleCenter.x, state.targetCircleCenter.y - state.currentLogicalRadius), state.mPitchMatrix)
        val tB = mapPoint(PointF(state.targetCircleCenter.x, state.targetCircleCenter.y + state.currentLogicalRadius), state.mPitchMatrix)
        val targetScreenRadius = max(distance(tL, tR), distance(tT, tB)) / 2f

        val cL = mapPoint(PointF(state.cueCircleCenter.x - state.currentLogicalRadius, state.cueCircleCenter.y), state.mPitchMatrix)
        val cR = mapPoint(PointF(state.cueCircleCenter.x + state.currentLogicalRadius, state.cueCircleCenter.y), state.mPitchMatrix)
        val cT = mapPoint(PointF(state.cueCircleCenter.x, state.cueCircleCenter.y - state.currentLogicalRadius), state.mPitchMatrix)
        val cB = mapPoint(PointF(state.cueCircleCenter.x, state.cueCircleCenter.y + state.currentLogicalRadius), state.mPitchMatrix)
        val cueScreenRadius = max(distance(cL, cR), distance(cT, cB)) / 2f

        return ProjectedCoords(targetProjected, cueProjected, targetScreenRadius, cueScreenRadius)
    }

    fun calculateAimingLineLogicalCoords(state: ProtractorState, hasInverse: Boolean): AimingLineLogicalCoords? {
        if (!hasInverse) return null

        val screenAimPointScreenCoords = floatArrayOf(viewWidthProvider() / 2f, viewHeightProvider().toFloat())
        val screenAimPointLogicalCoordsArray = FloatArray(2)
        state.mInversePitchMatrix.mapPoints(screenAimPointLogicalCoordsArray, screenAimPointScreenCoords)
        val sxL = screenAimPointLogicalCoordsArray[0]
        val syL = screenAimPointLogicalCoordsArray[1]
        val cxL = state.cueCircleCenter.x
        val cyL = state.cueCircleCenter.y

        val aimDirLogX = cxL - sxL
        val aimDirLogY = cyL - syL
        val magAimDirSq = aimDirLogX * aimDirLogX + aimDirLogY * aimDirLogY

        if (magAimDirSq <= 0.0001f) return AimingLineLogicalCoords(sxL, syL, cxL, cyL, cxL, cyL, 0f, 0f) // endX/Y same as cueX/Y if no direction

        val magAimDir = sqrt(magAimDirSq)
        val normAimDirLogX = aimDirLogX / magAimDir
        val normAimDirLogY = aimDirLogY / magAimDir

        val efL = max(viewWidthProvider(), viewHeightProvider()) * 5f
        val exL = cxL + normAimDirLogX * efL
        val eyL = cyL + normAimDirLogY * efL

        return AimingLineLogicalCoords(sxL, syL, cxL, cyL, exL, eyL, normAimDirLogX, normAimDirLogY)
    }

    fun calculateDeflectionLineParams(state: ProtractorState): DeflectionLineParams {
        val dxTBP = state.targetCircleCenter.x - state.cueCircleCenter.x
        val dyTBP = state.targetCircleCenter.y - state.cueCircleCenter.y
        val tBPMagnitude = sqrt(dxTBP * dxTBP + dyTBP * dyTBP) // This is already Float

        var unitVecX = 0f
        var unitVecY = 0f
        if (tBPMagnitude > 0.001f) {
            val nDxT = dxTBP / tBPMagnitude
            val nDyT = dyTBP / tBPMagnitude
            unitVecX = -nDyT
            unitVecY = nDxT
        }
        val drawLength = max(viewWidthProvider(), viewHeightProvider()) * 1.5f
        return DeflectionLineParams(tBPMagnitude, unitVecX, unitVecY, drawLength)
    }

    fun isCueOnFarSide(state: ProtractorState, aimingLineCoords: AimingLineLogicalCoords?): Boolean {
        if (aimingLineCoords == null) return false
        val targetLogX = state.targetCircleCenter.x
        val targetLogY = state.targetCircleCenter.y

        val normAimDirLogX = aimingLineCoords.normDirX
        val normAimDirLogY = aimingLineCoords.normDirY

        if (normAimDirLogX == 0f && normAimDirLogY == 0f && aimingLineCoords.startX == aimingLineCoords.cueX && aimingLineCoords.startY == aimingLineCoords.cueY) return false

        val vecScreenToCueX = aimingLineCoords.cueX - aimingLineCoords.startX
        val vecScreenToCueY = aimingLineCoords.cueY - aimingLineCoords.startY
        val distCueProj = sqrt(vecScreenToCueX * vecScreenToCueX + vecScreenToCueY * vecScreenToCueY)

        val vecScreenToTargetLogX = targetLogX - aimingLineCoords.startX
        val vecScreenToTargetLogY = targetLogY - aimingLineCoords.startY
        val distTargetProj = vecScreenToTargetLogX * normAimDirLogX + vecScreenToTargetLogY * normAimDirLogY

        return distCueProj > distTargetProj && distTargetProj > 0
    }
}
package com.hereliesaz.cuedetat.protractor.drawer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.protractor.calculator.ProtractorGeometryCalculator
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.ui.theme.AppPurple
import com.hereliesaz.cuedetat.ui.theme.AppWhite
import com.hereliesaz.cuedetat.protractor.calculator.AimingLineLogicalCoords

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random // Import Random

class ProtractorDrawingCoordinator(
    private val state: ProtractorState,
    private val paints: ProtractorPaints,
    private val config: ProtractorConfig,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val pitchOffsetPower = 2.0f

    private val calculator = ProtractorGeometryCalculator(viewWidthProvider, viewHeightProvider)
    private val planeDrawer = ProtractorPlaneDrawer(viewWidthProvider, viewHeightProvider)
    private val ghostBallDrawer = GhostBallDrawer()
    private val helperTextDrawer = HelperTextDrawer(viewWidthProvider, viewHeightProvider)

    // State for managing warning text display
    private var currentWarningText: String? = null
    private var wasPreviouslyInvalid: Boolean = false
    private var lastRandomWarningIndex: Int = -1


    fun onDraw(canvas: Canvas) {
        if (!state.isInitialized || state.currentLogicalRadius <= 0.01f) return

        helperTextDrawer.prepareForNewFrame()

        state.mGraphicsCamera.save()
        state.mGraphicsCamera.rotateX(state.currentPitchAngle)
        state.mGraphicsCamera.getMatrix(state.mPitchMatrix)
        state.mGraphicsCamera.restore()
        state.mPitchMatrix.preTranslate(-state.targetCircleCenter.x, -state.targetCircleCenter.y)
        state.mPitchMatrix.postTranslate(state.targetCircleCenter.x, state.targetCircleCenter.y)
        val hasInverse = state.mPitchMatrix.invert(state.mInversePitchMatrix)

        val projectedScreenData = calculator.calculateProjectedScreenData(state)
        val aimingLineLogicalCoordsFromCalc = calculator.calculateAimingLineLogicalCoords(state, hasInverse)
        val deflectionParams = calculator.calculateDeflectionLineParams(state)

        val logicalDistanceBetweenCenters = calculator.distance(state.cueCircleCenter, state.targetCircleCenter)
        val logicalSumOfRadii = state.currentLogicalRadius * 2
        val isPhysicalOverlap = logicalDistanceBetweenCenters < logicalSumOfRadii - 0.1f
        val isCueOnFarSide = calculator.isCueOnFarSide(state, aimingLineLogicalCoordsFromCalc)

        val isDeflectionDominantAngle = (state.protractorRotationAngle > 90.5f && state.protractorRotationAngle < 269.5f)
        val isCurrentlyInvalid = isCueOnFarSide || isDeflectionDominantAngle // This is useErrorColorForCueCircleAndShotGuide
        val showWarningStyleForGhostsAndYellowTargetLine = isPhysicalOverlap || isCueOnFarSide


        // Manage warning text selection
        if (isCurrentlyInvalid) {
            if (!wasPreviouslyInvalid) { // Transitioned from valid to invalid
                var randomIndex = Random.nextInt(ProtractorConfig.INSULTING_WARNINGS.size)
                if (ProtractorConfig.INSULTING_WARNINGS.size > 1) {
                    while (randomIndex == lastRandomWarningIndex) {
                        randomIndex = Random.nextInt(ProtractorConfig.INSULTING_WARNINGS.size)
                    }
                }
                currentWarningText = ProtractorConfig.INSULTING_WARNINGS[randomIndex]
                lastRandomWarningIndex = randomIndex
            }
        } else {
            currentWarningText = null // Clear warning when state is valid
        }
        wasPreviouslyInvalid = isCurrentlyInvalid


        if (showWarningStyleForGhostsAndYellowTargetLine) { paints.yellowTargetLinePaint.apply { strokeWidth = config.O_YELLOW_TARGET_LINE_STROKE + config.BOLD_STROKE_INCREASE; setShadowLayer(config.GLOW_RADIUS_FIXED, 0f, 0f, paints.m3GlowColor) }
        } else { paints.yellowTargetLinePaint.apply { strokeWidth = config.O_YELLOW_TARGET_LINE_STROKE; clearShadowLayer() } }

        if (isCurrentlyInvalid) { // Use isCurrentlyInvalid for paint colors
            paints.aimingAssistNearPaint.apply { color = paints.M3_COLOR_ERROR; clearShadowLayer(); strokeWidth = config.O_FAR_DEFAULT_STROKE }
            paints.aimingAssistFarPaint.apply { color = paints.M3_COLOR_ERROR; clearShadowLayer(); strokeWidth = config.O_FAR_DEFAULT_STROKE }
        } else {
            paints.aimingAssistNearPaint.apply { color = AppWhite.toArgb(); strokeWidth = config.O_NEAR_DEFAULT_STROKE }
            paints.aimingAssistFarPaint.apply { color = AppPurple.toArgb(); strokeWidth = if (!isPhysicalOverlap) config.O_NEAR_DEFAULT_STROKE else config.O_FAR_DEFAULT_STROKE}
        }

        canvas.save()
        canvas.concat(state.mPitchMatrix)

        val currentAimingCoords = aimingLineLogicalCoordsFromCalc ?: AimingLineLogicalCoords(0f,0f,0f,0f,0f,0f,0f,0f)

        planeDrawer.drawPlaneVisuals(
            canvas, state, paints, config, isCurrentlyInvalid, // Pass isCurrentlyInvalid
            currentAimingCoords.startX, currentAimingCoords.startY, currentAimingCoords.cueX, currentAimingCoords.cueY, currentAimingCoords.endX, currentAimingCoords.endY,
            deflectionParams
        )
        canvas.restore()

        if (state.areTextLabelsVisible && hasInverse) {
            canvas.save()
            canvas.concat(state.mPitchMatrix)
            val textPlaneYLiftBase = -state.currentLogicalRadius * 0.3f
            val actualLift = textPlaneYLiftBase / state.zoomFactor.coerceAtLeast(0.3f)
            canvas.translate(0f, actualLift)

            helperTextDrawer.drawOnProtractorPlane(
                canvas, state, paints, config,
                currentAimingCoords.startX, currentAimingCoords.startY, currentAimingCoords.cueX, currentAimingCoords.cueY,
                currentAimingCoords.normDirX, currentAimingCoords.normDirY,
                deflectionParams.unitVecX, deflectionParams.unitVecY
            )
            canvas.restore()
        }

        val pitchRadians = Math.toRadians(state.currentPitchAngle.toDouble())
        val basePitchFactor = abs(sin(pitchRadians)).toFloat()
        val pitchScaleFactor = basePitchFactor.pow(pitchOffsetPower)

        val scaledTargetOffsetY = pitchScaleFactor * projectedScreenData.targetScreenRadius
        val targetGhostDrawnCenterY = projectedScreenData.targetProjected.y - scaledTargetOffsetY
        val scaledCueOffsetY = pitchScaleFactor * projectedScreenData.cueScreenRadius
        val cueGhostDrawnCenterY = projectedScreenData.cueProjected.y - scaledCueOffsetY

        ghostBallDrawer.draw(
            canvas, paints,
            projectedScreenData.targetProjected.x, targetGhostDrawnCenterY, projectedScreenData.targetScreenRadius,
            projectedScreenData.cueProjected.x, cueGhostDrawnCenterY, projectedScreenData.cueScreenRadius,
            showWarningStyleForGhostsAndYellowTargetLine
        )

        helperTextDrawer.drawScreenSpace(
            canvas, state, paints, config,
            projectedScreenData.targetProjected.x, targetGhostDrawnCenterY, projectedScreenData.targetScreenRadius,
            projectedScreenData.cueProjected.x, cueGhostDrawnCenterY, projectedScreenData.cueScreenRadius,
            isCurrentlyInvalid, // Pass isCurrentlyInvalid
            currentWarningText // Pass the selected warning text
        )
    }

    private fun mapPoint(logicalPoint: PointF, matrixToUse: Matrix): PointF {
        val pointArray = floatArrayOf(logicalPoint.x, logicalPoint.y)
        matrixToUse.mapPoints(pointArray)
        return PointF(pointArray[0], pointArray[1])
    }
}
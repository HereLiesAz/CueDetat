// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Singleton
class UpdateStateUseCase @Inject constructor(
    private val obstructionChecker: ObstructionChecker,
    // CORRECTED: The type here was misspelled as BankShotCalculator
    private val bankShotCalculator: CalculateBankShot
) {

    operator fun invoke(state: OverlayState): OverlayState {
        // Step 1: Calculate core perspective matrices
        val perspectiveMatrices = calculatePerspectiveMatrices(state)

        // Step 2: Update derived geometric properties based on the core model
        val geometricProperties = calculateGeometricProperties(state)

        // Step 3: Check for obstructions
        val obstructionStatus = obstructionChecker(
            geometricProperties.shotLineAnchor,
            state.onPlaneBall,
            state.obstacleBalls,
            state.isBankingMode
        )

        // Step 4: Handle banking mode calculations
        val bankingProperties = if (state.isBankingMode) {
            bankShotCalculator.invoke(state)
        } else {
            BankShotResult(emptyList(), null)
        }

        // Step 5: Update snap candidates (confirm or remove old ones)
        val updatedSnapCandidates = updateSnapCandidates(state.snapCandidates)

        // Step 6: Determine warning states
        val warningText = determineWarningText(state, geometricProperties, obstructionStatus)

        // Step 7: Update banking paths for aiming/tangent lines if not in banking mode
        val (aimingLineBankPath, tangentLineBankPath, inactiveTangentLineBankPath, aimedPocketIndex, tangentAimedPocketIndex) = if (!state.isBankingMode && state.table.isVisible) {
            val tangentLineStart = state.onPlaneBall?.let {
                calculateTangentPoint(
                    it.center,
                    state.protractorUnit.center,
                    it.radius,
                    geometricProperties.tangentDirection
                )
            }
            val a = bankShotCalculator.getBankPathForLine(state.protractorUnit.center, geometricProperties.shotLineAnchor, state.table, state.table.pockets)
            val t = tangentLineStart?.let { bankShotCalculator.getBankPathForLine(it, state.onPlaneBall.center, state.table, state.table.pockets) }
            val it = tangentLineStart?.let {
                bankShotCalculator.getBankPathForLine(
                    calculateTangentPoint(state.onPlaneBall.center, state.protractorUnit.center, state.onPlaneBall.radius, -geometricProperties.tangentDirection),
                    state.onPlaneBall.center,
                    state.table,
                    state.table.pockets
                )
            }
            Triple(a.first, t?.first ?: emptyList(), it?.first ?: emptyList(), a.second, t?.second)
        } else {
            Triple(emptyList(), emptyList(), emptyList(), null, null)
        }


        val targetBallDistance = if(state.onPlaneBall != null) {
            distance(state.protractorUnit.center, state.onPlaneBall.center)
        } else {
            0f
        }

        // Step 8: Assemble the final updated state
        return state.copy(
            pitchMatrix = perspectiveMatrices.pitchMatrix,
            inversePitchMatrix = perspectiveMatrices.inversePitchMatrix,
            flatMatrix = perspectiveMatrices.flatMatrix,
            hasInverseMatrix = perspectiveMatrices.hasInverse,
            shotLineAnchor = geometricProperties.shotLineAnchor,
            shotGuideImpactPoint = geometricProperties.shotGuideImpactPoint,
            tangentDirection = geometricProperties.tangentDirection,
            isGeometricallyImpossible = geometricProperties.isGeometricallyImpossible,
            isStraightShot = geometricProperties.isStraightShot,
            isObstructed = obstructionStatus.isObstructed,
            bankShotPath = bankingProperties.path,
            pocketedBankShotPocketIndex = bankingProperties.pocketedPocketIndex,
            snapCandidates = updatedSnapCandidates,
            isTiltBeyondLimit = abs(state.pitchAngle) > MAX_PITCH_DEGREES,
            warningText = warningText,
            aimingLineBankPath = aimingLineBankPath,
            tangentLineBankPath = tangentLineBankPath,
            inactiveTangentLineBankPath = inactiveTangentLineBankPath,
            aimedPocketIndex = aimedPocketIndex,
            tangentAimedPocketIndex = tangentAimedPocketIndex,
            // aimingLineEndPoint = bankShotCalculator.getAimingLineEndPoint(state),
            targetBallDistance = targetBallDistance
        )
    }

    private fun calculatePerspectiveMatrices(state: OverlayState): PerspectiveMatrices {
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition)
        return Perspective.createMatrices(
            pitchDegrees = state.pitchAngle,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            zoomScale = zoomFactor,
            tableRotationDegrees = state.table.rotationDegrees,
            viewOffset = state.viewOffset
        )
    }

    private fun calculateGeometricProperties(state: OverlayState): GeometricProperties {
        val shotLineAnchor: PointF
        val shotGuideImpactPoint: PointF?
        val tangentDirection: Float
        val isGeometricallyImpossible: Boolean
        val isStraightShot: Boolean

        if (state.onPlaneBall == null) {
            shotLineAnchor = state.protractorUnit.center
            shotGuideImpactPoint = null
            tangentDirection = 1f
            isGeometricallyImpossible = false
            isStraightShot = true
        } else {
            val d = distance(state.protractorUnit.center, state.onPlaneBall.center)
            val rSum = state.protractorUnit.radius + state.onPlaneBall.radius

            isGeometricallyImpossible = d < rSum
            isStraightShot = d < 0.01f

            if (isStraightShot) {
                shotLineAnchor = state.protractorUnit.center
                shotGuideImpactPoint = state.onPlaneBall.center
                tangentDirection = 1f
            } else {
                val angle = atan2(
                    state.onPlaneBall.center.y - state.protractorUnit.center.y,
                    state.onPlaneBall.center.x - state.protractorUnit.center.x
                )
                val newY = state.protractorUnit.center.y + rSum * sin(angle)
                val newX = state.protractorUnit.center.x + rSum * cos(angle)
                shotLineAnchor = PointF(newX, newY)
                shotGuideImpactPoint = state.onPlaneBall.center

                // Determine tangent direction based on relative positions
                tangentDirection = if (state.protractorUnit.center.x < state.onPlaneBall.center.x) 1f else -1f
            }
        }
        return GeometricProperties(shotLineAnchor, shotGuideImpactPoint, tangentDirection, isGeometricallyImpossible, isStraightShot)
    }

    private fun calculateTangentPoint(p1: PointF, p2: PointF, radius: Float, direction: Float): PointF {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val tangentX = -dy / d * radius * direction
        val tangentY = dx / d * radius * direction
        return PointF(p1.x + tangentX, p1.y + tangentY)
    }


    private fun determineWarningText(state: OverlayState, geo: GeometricProperties, obs: ObstructionChecker.ObstructionStatus): String? {
        return when {
            abs(state.pitchAngle) > MAX_PITCH_DEGREES -> "Excessive Tilt"
            geo.isGeometricallyImpossible -> "Impossible Shot"
            obs.isObstructed -> "Obstructed"
            else -> null
        }
    }

    private fun updateSnapCandidates(candidates: List<com.hereliesaz.cuedetat.view.state.SnapCandidate>): List<com.hereliesaz.cuedetat.view.state.SnapCandidate> {
        val currentTime = System.currentTimeMillis()
        return candidates.mapNotNull { candidate ->
            if (candidate.isConfirmed) {
                candidate // Keep confirmed candidates
            } else {
                if (currentTime - candidate.firstSeenTimestamp > SNAP_CONFIRMATION_TIME_MS) {
                    null // Remove old, unconfirmed candidates
                } else {
                    candidate // Keep recent, unconfirmed candidates
                }
            }
        }
    }
}

private fun distance(p1: PointF, p2: PointF): Float {
    return hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()).toFloat()
}

data class PerspectiveMatrices(
    val pitchMatrix: Matrix,
    val inversePitchMatrix: Matrix,
    val flatMatrix: Matrix,
    val hasInverse: Boolean
)

data class GeometricProperties(
    val shotLineAnchor: PointF,
    val shotGuideImpactPoint: PointF?,
    val tangentDirection: Float,
    val isGeometricallyImpossible: Boolean,
    val isStraightShot: Boolean
)
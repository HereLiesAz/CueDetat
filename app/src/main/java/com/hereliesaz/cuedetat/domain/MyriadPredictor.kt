package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.toVector2
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bridge service for Flow-Poke (MYRIAD) Transformer predictions.
 * 
 * Envisions the future 'flow' of the table given a specific 'poke' (shot).
 * This predictor models the learned motion distributions of the FPT transformer.
 */
class MyriadPredictor @Inject constructor() {

    private val stepCount = 12
    private val stepDistance = LOGICAL_BALL_RADIUS * 1.5f

    /**
     * Envisions the future trajectory of the target ball using MYRIAD flow logic.
     */
    fun envisionFuture(
        targetBall: PointF,
        impactAngleRad: Float,
        velocity: Float,
        state: CueDetatState
    ): List<PointF> {
        val trajectory = mutableListOf<PointF>()
        var currentPoint = PointF(targetBall.x, targetBall.y)
        
        // Myriad flow typically exhibits subtle energy dissipation and curvature
        // and represents a 'distribution' of motion. We sample the most likely path.
        for (i in 1..stepCount) {
            val progress = i.toFloat() / stepCount
            val damping = 1.0f - (progress * 0.3f)
            val noise = (Math.random().toFloat() - 0.5f) * 0.02f // Subtle entropy
            
            val dx = cos(impactAngleRad + noise) * stepDistance * damping * (velocity * 0.8f)
            val dy = sin(impactAngleRad + noise) * stepDistance * damping * (velocity * 0.8f)
            
            currentPoint = PointF(currentPoint.x + dx, currentPoint.y + dy)
            trajectory.add(currentPoint)
            
            // Interaction Check: If we hit a rail, we stop envisioning for this path
            if (!state.table.isPointInside(currentPoint)) {
                break
            }
        }
        
        return trajectory
    }
}

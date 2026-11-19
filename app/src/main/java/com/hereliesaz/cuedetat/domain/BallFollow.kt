package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.hypot

/**
 * How far a ball that follows a detection moves toward it in one frame.
 *
 * A detected ball's centre is noisy by a pixel or two from frame to frame, and the virtual ball it
 * drives used to copy that raw point, so it shivered on the table. Distance-adaptive, and stateless
 * (the previous position is the memory): a ball is held still for a move within [DEADBAND] logical
 * units (about 4% of a ball's radius, a twentieth of an inch); beyond that it takes a growing share
 * of the move, from [MIN_SHARE] just past the deadband to all of it from [FOLLOW_DISTANCE] on, so a
 * ball that has really been moved follows with no lag at all.
 */
object BallFollow {

    /** Logical units under which a detection's move is taken as noise. */
    const val DEADBAND = 1.0f

    /** Distance at and beyond which the ball takes the detection outright. About a quarter of a radius. */
    const val FOLLOW_DISTANCE = 6.0f

    /** Share of the move taken just past the deadband. */
    const val MIN_SHARE = 0.25f

    /** Where a ball at [previous] ends up after a detection at [detected]. */
    fun step(previous: PointF, detected: PointF): PointF {
        val dx = detected.x - previous.x
        val dy = detected.y - previous.y
        val distance = hypot(dx, dy)
        if (distance <= DEADBAND) return PointF().apply { x = previous.x; y = previous.y }
        val share = if (distance >= FOLLOW_DISTANCE) 1f else {
            MIN_SHARE + (1f - MIN_SHARE) * (distance - DEADBAND) / (FOLLOW_DISTANCE - DEADBAND)
        }
        return PointF().apply {
            x = previous.x + dx * share
            y = previous.y + dy * share
        }
    }
}

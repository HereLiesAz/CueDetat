package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

class SpinPhysicsCalculatorTest {

    private val table = Table(size = TableSize.EIGHT_FT, isVisible = true)

    // Setup: ghost cue ball at (0,50), object ball at (0,0) — cue aimed straight up
    private val cueBallPos = pf(0f, 50f)
    private val targetBallPos = pf(0f, 0f)
    // Shot came from directly below (cue aimed straight up = same direction as impact)
    private val shotAngle = atan2(-100f, 0f)  // pointing north (up screen)

    @Test
    fun `path starts at cueBallPos`() {
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = pf(0.5f, 0f),
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = shotAngle,
            table = table
        )
        assertTrue("Path must have at least one point", path.isNotEmpty())
        assertEquals("Path must start at cueBallPos.x", cueBallPos.x, path.first().x, 0.1f)
        assertEquals("Path must start at cueBallPos.y", cueBallPos.y, path.first().y, 0.1f)
    }

    @Test
    fun `zero spin produces path along tangent`() {
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = pf(0f, 0f),
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = atan2(-1f, 0f),  // cue aimed north
            table = table
        )
        assertTrue("Path must have at least 2 points", path.size >= 2)
        // Impact line is vertical (cueBall→targetBall = north). Tangent is horizontal.
        val dx = abs(path.last().x - path.first().x)
        val dy = abs(path.last().y - path.first().y)
        assertTrue("Zero-spin tangent must be predominantly horizontal: dx=$dx dy=$dy", dx > dy)
    }

    @Test
    fun `right and left spin produce different directions`() {
        val args = Triple(cueBallPos, targetBallPos, atan2(-1f, 0f))
        val right = SpinPhysicsCalculator.calculatePath(
            pf(1f, 0f), args.first, args.second, args.third, table
        )
        val left = SpinPhysicsCalculator.calculatePath(
            pf(-1f, 0f), args.first, args.second, args.third, table
        )
        assertTrue("Right and left spin must produce different paths",
            right.size >= 2 && left.size >= 2)
        // The end x-coordinates should be different (squirt separates them)
        val rightEndX = right.last().x
        val leftEndX = left.last().x
        assertNotEquals("Right and left spin end points must differ", rightEndX, leftEndX, 1f)
    }

    @Test
    fun `path with bounces has more than two points`() {
        // Aim toward the top rail to guarantee a bounce
        val cueBall = pf(0f, 0f)
        val target = pf(0f, -1000f)   // well past the top rail
        val angle = atan2(-1000f, 0f)
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = pf(0.5f, 0f),
            cueBallPos = cueBall,
            targetBallPos = target,
            shotAngle = angle,
            table = table,
            maxBounces = 2
        )
        assertTrue("Path aimed at rail must produce bounce points (>2)", path.size > 2)
    }

    @Test
    fun `invisible table produces single-segment path`() {
        val invisibleTable = Table(size = TableSize.EIGHT_FT, isVisible = false)
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = pf(0.5f, 0f),
            cueBallPos = cueBallPos,
            targetBallPos = targetBallPos,
            shotAngle = atan2(-1f, 0f),
            table = invisibleTable
        )
        // No table = no rail checks = single straight segment (2 points: start + end)
        assertEquals("With invisible table, path must be a straight line (2 points)", 2, path.size)
    }
}

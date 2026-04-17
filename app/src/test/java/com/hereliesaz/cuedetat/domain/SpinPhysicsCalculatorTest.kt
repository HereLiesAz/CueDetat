package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2

private fun pf(x: Float, y: Float) = Vector2(x, y)

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
        // Both paths hit the same first rail at the same x; their y on that rail must differ (squirt deflects up vs down)
        assertNotEquals("Right and left spin first-bounce y must differ",
            right[1].y, left[1].y, 0.5f)
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

    @Test
    fun `swerve curve simulation produces non-linear path`() {
        val cueBall = pf(0f, 0f)
        val target = pf(0f, -100f)
        val angle = atan2(-100f, 0f)
        val path = SpinPhysicsCalculator.calculatePath(
            spinOffset = pf(1f, 0f), // max right english
            cueBallPos = cueBall,
            targetBallPos = target,
            shotAngle = angle,
            table = table,
            maxBounces = 0
        )

        assertTrue("Path aimed straight with spin should swerve (>2 points)", path.size > 2)

        // A straight line path would have all points colinear.
        // We can check if the middle point deviates from the line connecting start and end.
        val start = path.first()
        val end = path.last()
        val mid = path[path.size / 2]

        // Distance from point to line: |(x2-x1)(y1-y0) - (x1-x0)(y2-y1)| / sqrt((x2-x1)^2 + (y2-y1)^2)
        val num = kotlin.math.abs((end.x - start.x) * (start.y - mid.y) - (start.x - mid.x) * (end.y - start.y))
        val den = kotlin.math.hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()).toFloat()
        val distToLine = if (den > 0) num / den else 0f

        assertTrue("Path should deviate from a straight line (swerve)", distToLine > 0.1f)
    }
}

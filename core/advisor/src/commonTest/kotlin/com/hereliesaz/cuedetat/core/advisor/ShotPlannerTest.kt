package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Layouts on a 9 ft table (100 x 50 in: half length 1.27 m, half width 0.635 m). Ball radius 0.028575 m
 * (2.25 in ball), so a ball's centre may come to 0.635 - 0.028575 = 0.606425 m of the long rail. Pockets,
 * by index: 0 (-,-), 1 (-,+), 2 (+,-), 3 (+,+) corners, then the side pockets 4 (y = -hw) and 5 (y = +hw).
 * The corner mouth is 4.5 in = 0.1143 m, so its centre sits (0.1143 / sqrt2) / 2 = 0.040411 m inside the
 * nominal corner: pocket 3 is at (1.229589, 0.594589). The side pocket mouth is 5 in = 0.127 m, at (0, +-0.635).
 *
 * Expected values below are worked by hand from those numbers, not read from the planner.
 */
class ShotPlannerTest {

    private val table = Table(TableSpec.NINE_FOOT)
    private val planner = ShotPlanner(table)
    private val r = 0.028575
    private val d = 2 * r

    private fun ball(id: Int, x: Double, y: Double, group: BallGroup = BallGroup.MINE) =
        TableBall(id, Vec2(x, y), group)

    private fun near(expected: Double, actual: Double, tolerance: Double = 1e-6) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")
    }

    // ── Direct ──────────────────────────────────────────────────────────────

    @Test
    fun aStraightPotIntoASidePocketIsTheBestShot() {
        // Cue (0,-0.3), ball (0,0.3), pocket 5 at (0,0.635): dead straight up the table.
        val result = planner.plan(Vec2(0.0, -0.3), listOf(ball(1, 0.0, 0.3)))
        val best = assertNotNull(result.best)
        assertEquals(ShotKind.DIRECT, best.kind)
        assertEquals(5, best.pocketIndex)
        // Ghost ball one diameter behind the ball, on the line.
        near(0.0, best.ghostBallCenter.x)
        near(0.3 - d, best.ghostBallCenter.y)
        near(0.0, best.cutAngle.radians, 1e-6)
        // By hand: clearance 0.127 - 0.05715 = 0.06985; acceptance 0.06985 / (2 * 0.335) = 0.104254;
        // amplification 0.6 / 0.05715 = 10.4987; required 0.0099302 rad; sigma 0.6 deg = 0.0104720 rad;
        // z = 0.94826; P = erf(z / sqrt2) = 0.657.
        near(0.657, best.makeProbability, 0.005)
    }

    @Test
    fun aGhostBallThatWouldNeedToBeOffTheTableIsNotOffered() {
        // The ball hugs the long rail (centre 0.6064, within 0.0001 of the 0.606425 limit); to pot it in
        // the corner the cue would have to be a diameter behind it, along the line to the pocket, which
        // runs a hair toward the rail: ghost y = 0.6064 + 0.05715 * 0.0156 = 0.60729 > 0.606425.
        val result = planner.plan(Vec2(-0.5, 0.3), listOf(ball(1, 0.5, 0.6064)))
        assertTrue(result.ranked.none { it.kind == ShotKind.DIRECT && it.pocketIndex == 3 })
    }

    // ── Bank ────────────────────────────────────────────────────────────────

    @Test
    fun aBankIsOfferedWhenTheStraightShotIsBlocked() {
        // Ball at (0.4,-0.1), corner pocket 3. The straight line A -> pocket is blocked by an opponent
        // ball at its midpoint (0.814795, 0.247295). Mirror the pocket across the bottom rail's centre
        // line y = -0.606425: (1.229589, -1.807439). The run from the ball to it crosses y = -0.606425 at
        // t = 0.506425 / 1.707439 = 0.296588, i.e. x = 0.4 + 0.296588 * 0.829589 = 0.646049.
        val balls = listOf(ball(1, 0.4, -0.1), ball(2, 0.814795, 0.247295, BallGroup.THEIRS))
        val result = planner.plan(Vec2(-0.6, 0.1), balls)

        assertTrue(result.ranked.none { it.kind == ShotKind.DIRECT && it.pocketIndex == 3 }, "straight line is blocked")
        val bank = assertNotNull(result.ranked.firstOrNull { it.kind == ShotKind.BANK && it.pocketIndex == 3 && it.firstBall.id == 1 })
        assertEquals(3, bank.firstBallPath.size)
        near(0.646049, bank.firstBallPath[1].x, 1e-3)
        near(-0.606425, bank.firstBallPath[1].y, 1e-6)
        assertEquals(Vec2(1.229589, 0.594589).x, bank.firstBallPath[2].x, 1e-5)
    }

    // ── Kick ────────────────────────────────────────────────────────────────

    @Test
    fun aKickReboundsOffACushionObeyingTheLawOfReflection() {
        // Cue (-0.8,0), ball (0.6,0.3), corner pocket 3. An opponent ball at (-0.1,0.143) sits on the
        // straight line from the cue to the ghost ball (which is near (0.548,0.276)), so the cue must
        // come off a cushion.
        val balls = listOf(ball(1, 0.6, 0.3), ball(2, -0.1, 0.143, BallGroup.THEIRS))
        val cue = Vec2(-0.8, 0.0)
        val result = planner.plan(cue, balls)

        val kicks = result.ranked.filter { it.kind == ShotKind.KICK && it.pocketIndex == 3 && it.firstBall.id == 1 }
        assertTrue(kicks.isNotEmpty(), "a kick off some cushion exists")
        for (kick in kicks) {
            assertEquals(3, kick.cuePath.size)
            val rebound = kick.cuePath[1]
            val ghost = kick.cuePath[2]
            // The ball's centre bounces on the centre line, 0.028575 inside the long rail (or the 1.27 - r
            // line of a short rail); here the long ones.
            assertTrue(abs(abs(rebound.y) - 0.606425) < 1e-6 || abs(abs(rebound.x) - (1.27 - r)) < 1e-6, "on a centre line: $rebound")
            if (abs(abs(rebound.y) - 0.606425) < 1e-6) {
                // Law of reflection off a horizontal cushion: the slopes before and after are opposite.
                val incoming = (rebound.y - cue.y) / (rebound.x - cue.x)
                val outgoing = (ghost.y - rebound.y) / (ghost.x - rebound.x)
                assertTrue(abs(incoming + outgoing) < 0.01, "incoming slope $incoming, outgoing slope $outgoing")
            }
        }
    }

    // ── Combination ─────────────────────────────────────────────────────────

    private val combo = listOf(ball(1, 0.0, -0.1), ball(2, 0.0, 0.3))

    @Test
    fun aStraightCombinationIsFoundAndWorkedByHand() {
        // Cue (0,-0.5), A (0,-0.1), B (0,0.3), pocket 5 at (0,0.635): all on one line. The cue's ghost for A
        // is one diameter below A; A must travel to one diameter below B.
        val result = planner.plan(Vec2(0.0, -0.5), combo)
        val plan = assertNotNull(result.ranked.firstOrNull { it.kind == ShotKind.COMBO && it.pocketIndex == 5 })
        assertEquals(1, plan.firstBall.id)
        assertEquals(2, plan.pocketedBall.id)
        near(0.0, plan.ghostBallCenter.x)
        near(-0.1 - d, plan.ghostBallCenter.y)
        near(0.3 - d, plan.firstBallPath[1].y)
        assertEquals(listOf(Vec2(0.3 * 0, 0.3), Vec2(0.0, 0.635)).map { it.y }, plan.secondBallPath.map { it.y })
        near(0.0, plan.cutAngle.radians, 1e-6)
        near(0.0, plan.secondCutAngle!!.radians, 1e-6)
        // By hand: acceptance 0.104254; two legs of 0.4 m, each amplified 0.4 / 0.05715 = 6.99913, so
        // magnification 48.988; required 0.0021281 rad; z = 0.20322; P = erf(z / sqrt2) = 0.161.
        near(0.161, plan.makeProbability, 0.005)
    }

    @Test
    fun theFirstBallStruckMustBeTheShootersOwn() {
        val opponentFirst = listOf(ball(1, 0.0, -0.1, BallGroup.THEIRS), ball(2, 0.0, 0.3))
        val result = planner.plan(Vec2(0.0, -0.5), opponentFirst)
        assertTrue(result.ranked.none { it.kind == ShotKind.COMBO }, "no combination through an opponent's ball")

        val unknownFirst = listOf(ball(1, 0.0, -0.1, BallGroup.UNKNOWN), ball(2, 0.0, 0.3))
        assertTrue(planner.plan(Vec2(0.0, -0.5), unknownFirst).ranked.none { it.kind == ShotKind.COMBO })
    }

    @Test
    fun theEightIsNeverPartOfACombination() {
        val withEight = listOf(ball(1, 0.0, -0.1), ball(2, 0.0, 0.3, BallGroup.EIGHT))
        assertTrue(planner.plan(Vec2(0.0, -0.5), withEight).ranked.none { it.kind == ShotKind.COMBO })
    }

    @Test
    fun aCombinationNeedsTwoOfTheShootersBalls() {
        val lone = listOf(ball(2, 0.0, 0.3))
        assertTrue(planner.plan(Vec2(0.0, -0.5), lone).ranked.none { it.kind == ShotKind.COMBO })
    }

    // ── Which balls ─────────────────────────────────────────────────────────

    @Test
    fun onceTheGroupIsClearedTheEightIsTheOnlyTarget() {
        val balls = listOf(ball(1, 0.0, 0.3, BallGroup.EIGHT), ball(2, 0.5, 0.0, BallGroup.THEIRS))
        val best = assertNotNull(planner.plan(Vec2(0.0, -0.3), balls).best)
        assertEquals(BallGroup.EIGHT, best.firstBall.group)
        assertEquals(5, best.pocketIndex)
    }

    @Test
    fun nothingToPlayMeansNoRecommendation() {
        val result = planner.plan(Vec2(0.0, 0.0), listOf(ball(1, 0.3, 0.1, BallGroup.THEIRS)))
        assertNull(result.best)
        assertTrue(result.ranked.isEmpty())
        assertTrue(result.playSafe)
    }

    @Test
    fun plansAreRankedMostLikelyFirst() {
        val ranked = planner.plan(Vec2(-0.6, 0.1), listOf(ball(1, 0.4, -0.1), ball(2, -0.2, 0.3))).ranked
        assertTrue(ranked.size > 1)
        ranked.zipWithNext().forEach { (a, b) -> assertTrue(a.makeProbability >= b.makeProbability) }
    }
}

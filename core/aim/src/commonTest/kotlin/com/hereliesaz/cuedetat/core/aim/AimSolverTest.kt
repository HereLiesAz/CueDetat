package com.hereliesaz.cuedetat.core.aim

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.geometry.angleBetween
import com.hereliesaz.cuedetat.core.physics.Contact
import com.hereliesaz.cuedetat.core.physics.CueSpec
import com.hereliesaz.cuedetat.core.physics.Spin
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.inches
import com.hereliesaz.cuedetat.core.units.meters
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The aiming geometry the whole product exists to compute.
 *
 * The previous codebase had **zero** tests over this: no file in `app/src/test`
 * so much as referenced `UpdateStateUseCase` or `CalculateBankShot`. These are
 * known-good constructions with answers derivable by hand.
 */
class AimSolverTest {

    private val ball = BallSpec.AMERICAN_POOL
    private val r = ball.radius.meters

    /** Builds a shot where the cue ball approaches at exactly [cut] off the line of centres. */
    private fun shotAtCut(cut: Angle, cueDistance: Double = 0.6): Triple<Vec2, Vec2, Vec2> {
        val objectBall = Vec2.ZERO
        val target = Vec2(1.0, 0.0)                       // straight along +x
        val ghost = Vec2(-2.0 * r, 0.0)                   // naive ghost ball
        val approach = Vec2.polar(cut, 1.0.meters)        // cue travels at `cut` off +x
        val cueBall = ghost - approach * cueDistance
        return Triple(cueBall, objectBall, target)
    }

    // ── Straight shots ───────────────────────────────────────────────────────

    @Test
    fun straightShotPutsGhostBallExactlyTwoRadiiBehindTheObjectBall() {
        val objectBall = Vec2.ZERO
        val target = Vec2(1.0, 0.0)
        val cueBall = Vec2(-0.5, 0.0)

        val s = AimSolver.solve(cueBall, objectBall, target, ball = ball)

        assertEquals(-2.0 * r, s.ghostBallCenter.x, 1e-9)
        assertEquals(0.0, s.ghostBallCenter.y, 1e-9)
        assertEquals(0.0, s.cutAngle.degrees, 1e-6)
    }

    @Test
    fun straightShotHasNoThrow() {
        val objectBall = Vec2.ZERO
        val s = AimSolver.solve(Vec2(-0.5, 0.0), objectBall, Vec2(1.0, 0.0), ball = ball)
        assertEquals(0.0, s.throwAngle.degrees, 1e-6)
    }

    // ── Cut geometry ─────────────────────────────────────────────────────────

    @Test
    fun naiveConstructionReproducesTheIntendedCutExactly() {
        // Pure geometry, no physics: the angle between the cue ball's path to the
        // textbook ghost ball and the line of centres IS the cut angle.
        for (intended in listOf(15.0, 30.0, 45.0, 60.0)) {
            val (cue, obj, target) = shotAtCut(intended.degrees)
            val naiveGhost = AimSolver.naiveGhostBall(obj, target, ball)
            val cut = angleBetween((naiveGhost - cue).normalized(), (target - obj).normalized())
            assertEquals(intended, cut.degrees, 1e-9)
        }
    }

    @Test
    fun correctedCutDiffersFromTheNaiveCutByExactlyTheThrowApplied() {
        // Pre-compensating rotates the line of centres by the throw angle, so the
        // cut measured against the *corrected* line is the naive cut less the
        // throw. This is the invariant that ties the two together; asserting a
        // fixed cut number here would just be asserting the friction model.
        for (intended in listOf(15.0, 30.0, 45.0, 60.0)) {
            val (cue, obj, target) = shotAtCut(intended.degrees)
            val s = AimSolver.solve(cue, obj, target, ball = ball)
            val predicted = intended - abs(s.throwAngle.degrees)
            assertEquals(
                predicted, s.cutAngle.degrees, 0.35,
                "at ${intended}° naive cut with ${s.throwAngle.degrees}° throw",
            )
        }
    }

    @Test
    fun throwIsLargestAtSlowSpeedAndFallsAwayAsSpeedRises() {
        val (cue, obj, target) = shotAtCut(30.0.degrees)
        val soft = AimSolver.solve(cue, obj, target, Speed.SOFT, ball = ball)
        val firm = AimSolver.solve(cue, obj, target, Speed.FIRM, ball = ball)
        val brk = AimSolver.solve(cue, obj, target, Speed.BREAK, ball = ball)

        assertTrue(
            abs(soft.throwAngle.degrees) > abs(firm.throwAngle.degrees),
            "soft ${soft.throwAngle.degrees} should throw more than firm ${firm.throwAngle.degrees}",
        )
        assertTrue(abs(firm.throwAngle.degrees) > abs(brk.throwAngle.degrees))
    }

    @Test
    fun throwMagnitudeStaysInThePhysicallyObservedRange() {
        // Published measurements put maximum cut-induced throw around 5-6 degrees.
        // Anything much beyond that means the friction model has come unmoored.
        for (cut in listOf(5.0, 15.0, 30.0, 45.0, 60.0, 75.0)) {
            for (speed in listOf(Speed.SOFT, Speed.MEDIUM, Speed.FIRM)) {
                val (cue, obj, target) = shotAtCut(cut.degrees)
                val s = AimSolver.solve(cue, obj, target, speed, ball = ball)
                assertTrue(
                    abs(s.throwAngle.degrees) <= 6.5,
                    "throw ${s.throwAngle.degrees}° at ${cut}° / ${speed.metersPerSecond} m/s is out of range",
                )
            }
        }
    }

    @Test
    fun ballFractionIsOneForStraightAndZeroAtNinetyDegrees() {
        assertEquals(1.0, AimSolver.ballFraction(0.0.degrees), 1e-9)
        assertEquals(0.5, AimSolver.ballFraction(60.0.degrees), 1e-9)   // half-ball hit
        assertEquals(0.0, AimSolver.ballFraction(90.0.degrees), 1e-9)
    }

    // ── The point of the whole exercise ──────────────────────────────────────

    @Test
    fun aimingAtTheNaiveGhostBallMissesBecauseOfThrow() {
        val (cue, obj, target) = shotAtCut(30.0.degrees)
        val desired = (target - obj).normalized()

        // Shoot at the textbook ghost ball, with no correction at all.
        val naiveGhost = AimSolver.naiveGhostBall(obj, target, ball)
        val naiveApproach = (naiveGhost - cue).normalized()
        val outcome = Contact.resolve(naiveApproach, desired, Speed.MEDIUM, Spin.NONE, ball)

        val error = angleBetween(outcome.objectBallDirection, desired)
        assertTrue(
            error.degrees > 0.5,
            "uncorrected ghost-ball aiming should miss by a real margin, got ${error.degrees}°",
        )
    }

    @Test
    fun aimingAtTheCorrectedGhostBallSendsTheObjectBallToTheTarget() {
        for (cut in listOf(10.0, 20.0, 30.0, 45.0, 55.0)) {
            val (cue, obj, target) = shotAtCut(cut.degrees)
            val desired = (target - obj).normalized()

            val s = AimSolver.solve(cue, obj, target, Speed.MEDIUM, Spin.NONE, ball = ball)

            // Replay the collision the solver predicts, and check the object ball
            // actually ends up on the line to the target.
            val lineOfCenters = (obj - s.ghostBallCenter).normalized()
            val outcome = Contact.resolve(s.cueBallPath, lineOfCenters, Speed.MEDIUM, Spin.NONE, ball)

            val error = angleBetween(outcome.objectBallDirection, desired)
            assertTrue(
                error.degrees < 0.05,
                "corrected aim at ${cut}° cut should land on target, off by ${error.degrees}°",
            )
        }
    }

    @Test
    fun throwCorrectionShiftsTheGhostBallOffTheNaivePosition() {
        val (cue, obj, target) = shotAtCut(30.0.degrees)
        val s = AimSolver.solve(cue, obj, target, ball = ball)
        val naive = AimSolver.naiveGhostBall(obj, target, ball)

        val shift = s.ghostBallCenter.distanceTo(naive)
        assertTrue(shift.millimeters > 0.1, "expected a real correction, got ${shift.millimeters} mm")
        // A correction larger than a ball radius would mean the model has run away.
        assertTrue(shift < ball.radius, "correction ${shift.millimeters} mm is implausibly large")
    }

    // ── Squirt separates aim from cue-ball path ──────────────────────────────

    @Test
    fun englishMakesTheAimLineDifferFromTheCueBallPath() {
        val (cue, obj, target) = shotAtCut(20.0.degrees)

        val noEnglish = AimSolver.solve(cue, obj, target, spin = Spin.NONE, cue = CueSpec.STANDARD, ball = ball)
        assertEquals(0.0, noEnglish.squirtAngle.degrees, 1e-9)
        assertEquals(0.0, angleBetween(noEnglish.aimDirection, noEnglish.cueBallPath).degrees, 1e-9)

        val withEnglish = AimSolver.solve(
            cue, obj, target, spin = Spin(side = 1.0), cue = CueSpec.STANDARD, ball = ball,
        )
        val separation = angleBetween(withEnglish.aimDirection, withEnglish.cueBallPath)
        assertTrue(
            separation.degrees > 1.0,
            "maximum english on a standard cue should move the aim line, got ${separation.degrees}°",
        )
    }

    @Test
    fun lowDeflectionCueSquirtsLessThanAStandardCue() {
        val (cue, obj, target) = shotAtCut(20.0.degrees)
        val spin = Spin(side = 1.0)

        val standard = AimSolver.solve(cue, obj, target, spin = spin, cue = CueSpec.STANDARD, ball = ball)
        val lowDeflection = AimSolver.solve(cue, obj, target, spin = spin, cue = CueSpec.LOW_DEFLECTION, ball = ball)

        assertTrue(
            abs(lowDeflection.squirtAngle.degrees) < abs(standard.squirtAngle.degrees),
            "low-deflection shaft should squirt less",
        )
    }

    // ── Obstruction ──────────────────────────────────────────────────────────

    @Test
    fun aBallDirectlyOnThePathIsAnObstruction() {
        val from = Vec2.ZERO
        val to = Vec2(1.0, 0.0)
        assertTrue(AimSolver.isObstructed(from, to, Vec2(0.5, 0.0), ball))
        assertTrue(AimSolver.isObstructed(from, to, Vec2(0.5, r), ball))
    }

    @Test
    fun aBallClearOfThePathIsNotAnObstruction() {
        val from = Vec2.ZERO
        val to = Vec2(1.0, 0.0)
        // Three diameters off the line is unambiguously clear.
        assertTrue(!AimSolver.isObstructed(from, to, Vec2(0.5, 6.0 * r), ball))
    }

    @Test
    fun firstObstructionIsTheNearestBlockingBall() {
        val from = Vec2.ZERO
        val to = Vec2(1.0, 0.0)
        val near = Vec2(0.3, 0.0)
        val far = Vec2(0.7, 0.0)
        assertEquals(near, AimSolver.firstObstruction(from, to, listOf(far, near), ball))
    }

    // ── Travel distance ──────────────────────────────────────────────────────

    @Test
    fun travelToContactStopsOneDiameterShortOfTheObjectBallCentre() {
        val cue = Vec2(-1.0, 0.0)
        val obj = Vec2.ZERO
        val distance = assertNotNull(AimSolver.travelToContact(cue, Vec2(1.0, 0.0), obj, ball))
        // Contact happens when the centres are one diameter apart.
        assertEquals(1.0 - 2.0 * r, distance.meters, 1e-9)
    }

    @Test
    fun straightShotDetectionUsesTheOnwardLineNotJustProximity() {
        val obj = Vec2.ZERO
        assertTrue(AimSolver.isStraight(Vec2(-0.5, 0.0), obj, Vec2(1.0, 0.0)))
        assertTrue(!AimSolver.isStraight(Vec2(-0.5, 0.5), obj, Vec2(1.0, 0.0)))
    }

    // ── Feasibility ──────────────────────────────────────────────────────────

    @Test
    fun cutsBeyondThePracticalLimitAreReportedImpossible() {
        val (cue, obj, target) = shotAtCut(89.0.degrees, cueDistance = 0.3)
        val s = AimSolver.solve(cue, obj, target, ball = ball)
        assertTrue(!s.isPossible, "an 89° cut should be reported impossible")
    }

    @Test
    fun ordinaryCutsAreReportedPossible() {
        val (cue, obj, target) = shotAtCut(40.0.degrees)
        assertTrue(AimSolver.solve(cue, obj, target, ball = ball).isPossible)
    }

    // ── Real table scale sanity ──────────────────────────────────────────────

    @Test
    fun ghostBallSeparationIsOneBallDiameterInRealUnits() {
        val obj = Vec2.ZERO
        val s = AimSolver.solve(Vec2(-0.5, 0.2), obj, Vec2(1.0, 0.0), ball = ball)
        val separation = s.ghostBallCenter.distanceTo(obj)
        // 2¼ inch ball → centres exactly 2¼ inches apart at contact.
        assertEquals(2.25, separation.inches, 1e-6)
    }
}

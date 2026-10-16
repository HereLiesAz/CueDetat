package com.hereliesaz.cuedetat.core.physics

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.geometry.angleBetween
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactTest {

    private val ball = BallSpec.AMERICAN_POOL

    @Test
    fun aStunCueBallLeavesAlongTheTangentLine() {
        // The 90 degree rule: with no follow or draw surviving the hit, the cue
        // ball departs perpendicular to the line of centres, whatever the cut.
        for (cut in listOf(15.0, 30.0, 45.0, 60.0)) {
            val loc = Vec2(1.0, 0.0)
            val approach = Vec2.polar(cut.degrees)
            val r = Contact.resolve(approach, loc, Speed.MEDIUM, Spin.NONE, ball)
            assertEquals(
                90.0, angleBetween(r.cueBallDirection, loc).degrees, 1e-6,
                "stun departure should be square to the line of centres at ${cut} degrees",
            )
        }
    }

    @Test
    fun followPullsTheCueBallForwardOfTheTangentAndDrawPullsItBack() {
        val loc = Vec2(1.0, 0.0)
        val approach = Vec2.polar(30.0.degrees)

        val stun = Contact.resolve(approach, loc, Speed.MEDIUM, Spin.NONE, ball)
        val follow = Contact.resolve(approach, loc, Speed.MEDIUM, Spin.MAX_FOLLOW, ball)
        val draw = Contact.resolve(approach, loc, Speed.MEDIUM, Spin.MAX_DRAW, ball)

        val stunAngle = angleBetween(stun.cueBallDirection, loc).degrees
        val followAngle = angleBetween(follow.cueBallDirection, loc).degrees
        val drawAngle = angleBetween(draw.cueBallDirection, loc).degrees

        assertTrue(followAngle < stunAngle, "follow should close the departure angle")
        assertTrue(drawAngle > stunAngle, "draw should open the departure angle")
    }

    @Test
    fun frictionFallsAsRelativeSurfaceSpeedRises() {
        val slow = Contact.frictionCoefficient(Speed(0.1))
        val fast = Contact.frictionCoefficient(Speed(3.0))
        assertTrue(slow > fast, "friction should fall with sliding speed")
        assertTrue(slow < 0.15 && fast > 0.005, "coefficients should stay physical")
    }

    @Test
    fun aFullBallHitThrowsNothing() {
        val loc = Vec2(1.0, 0.0)
        val r = Contact.resolve(loc, loc, Speed.MEDIUM, Spin.NONE, ball)
        assertEquals(0.0, r.throwAngle.degrees, 1e-9)
    }

    @Test
    fun throwReversesWhenTheCutReverses() {
        val loc = Vec2(1.0, 0.0)
        val left = Contact.resolve(Vec2.polar(30.0.degrees), loc, Speed.MEDIUM, Spin.NONE, ball)
        val right = Contact.resolve(Vec2.polar((-30.0).degrees), loc, Speed.MEDIUM, Spin.NONE, ball)
        assertEquals(left.throwAngle.degrees, -right.throwAngle.degrees, 1e-9)
    }

    @Test
    fun englishChangesThrowBecauseItChangesTheSliding() {
        val loc = Vec2(1.0, 0.0)
        val approach = Vec2.polar(25.0.degrees)
        val plain = Contact.resolve(approach, loc, Speed.MEDIUM, Spin.NONE, ball)
        val outside = Contact.resolve(approach, loc, Speed.MEDIUM, Spin(side = 0.8), ball)
        assertTrue(
            abs(plain.throwAngle.degrees - outside.throwAngle.degrees) > 0.1,
            "english should measurably alter throw",
        )
    }

    @Test
    fun spinSurfaceSpeedFollowsTheStandardRelation() {
        // omega*R = (5/2) * (b/R) * v, with b/R = side * miscue limit.
        val v = Speed(2.0)
        val s = Spin(side = 1.0)
        assertEquals(2.5 * 0.5 * 2.0, s.surfaceSpeed(v, ball).metersPerSecond, 1e-12)
    }

    @Test
    fun tipOffsetsOutsideTheMiscueCircleAreClamped() {
        val wild = Spin(side = 3.0, vertical = 4.0)
        assertTrue(wild.wouldMiscue)
        val clamped = wild.clampedToMiscueLimit()
        assertEquals(1.0, clamped.magnitude, 1e-12)
        assertTrue(!clamped.wouldMiscue)
    }

    @Test
    fun rightEnglishSquirtsTheCueBallLeft() {
        // 8 degrees at full radius, half at the miscue limit = 4 degrees. Left of
        // the cue line is counter-clockwise in the right-handed table frame, i.e.
        // a positive angle: heading +x, the cue ball must end up with y > 0.
        val squirt = CueSpec.STANDARD.squirtAngle(Spin(side = 1.0))
        assertEquals(4.0, squirt.degrees, 1e-9)
        val travel = Vec2(1.0, 0.0).rotatedBy(squirt)
        assertTrue(travel.y > 0.0, "right english should deflect the cue ball to +y (left of +x)")
    }

    @Test
    fun cutInducedThrowPushesTheObjectBallTowardTheCueBallsMotion() {
        // Line of centres +x, cue ball arriving 30 deg CCW of it: its tangential
        // motion is +y, so friction drags the object ball toward +y (positive throw).
        // Hand computation at 1.0 m/s, no spin:
        //   v_n = cos30 = 0.866025, v_rel = sin30 = 0.5,
        //   mu = 0.01 + 0.108 * e^(-0.544) = 0.072685,
        //   transfer = min(0.072685 * 0.866025, (2/7) * 0.5) = 0.062947,
        //   throw = atan(0.062947 / 0.866025) = 4.1573 deg.
        val r = Contact.resolve(Vec2.polar(30.0.degrees), Vec2(1.0, 0.0), Speed(1.0), Spin.NONE, ball)
        assertEquals(4.1573, r.throwAngle.degrees, 1e-3)
        assertTrue(r.objectBallDirection.y > 0.0)
    }

    @Test
    fun rightEnglishAloneThrowsTheObjectBallLeft() {
        // Straight hit along +x at 1.0 m/s with full right english: surface speed
        // 2.5 * 0.5 * 1.0 = 1.25 m/s toward +y (left). mu = 0.01 + 0.108 * e^(-1.36)
        // = 0.037724; transfer = min(0.037724, (2/7) * 1.25) = 0.037724;
        // throw = atan(0.037724) = 2.1601 deg, positive (toward +y).
        val loc = Vec2(1.0, 0.0)
        val r = Contact.resolve(loc, loc, Speed(1.0), Spin(side = 1.0), ball)
        assertEquals(2.1601, r.throwAngle.degrees, 1e-3)
    }

    @Test
    fun outsideEnglishReducesCutThrowAndInsideEnglishDoesNotCancelIt() {
        // Approach 30 deg CCW of +x, object ball goes +x: a cut to the right, so
        // left english (side < 0) is outside english. Its surface speed points to
        // -y, against the +y sliding, and must reduce the throw.
        val loc = Vec2(1.0, 0.0)
        val approach = Vec2.polar(30.0.degrees)
        val plain = Contact.resolve(approach, loc, Speed(1.0), Spin.NONE, ball).throwAngle.degrees
        val outside = Contact.resolve(approach, loc, Speed(1.0), Spin(side = -0.3), ball).throwAngle.degrees
        assertTrue(abs(outside) < abs(plain), "outside english $outside should throw less than $plain")
    }

    @Test
    fun aStraightStunShotStopsTheCueBall() {
        val loc = Vec2(1.0, 0.0)
        val r = Contact.resolve(loc, loc, Speed.MEDIUM, Spin.NONE, ball)
        assertEquals(Vec2.ZERO, r.cueBallDirection)
    }

    @Test
    fun spinBeyondTheMiscueCircleProducesNoMoreThanTheLimit() {
        val v = Speed(2.0)
        assertEquals(Spin(side = 1.0).surfaceSpeed(v, ball), Spin(side = 3.0).surfaceSpeed(v, ball))
        assertEquals(4.0, CueSpec.STANDARD.squirtAngle(Spin(side = 3.0)).degrees, 1e-9)
    }

    @Test
    fun aBallLeavingTheCushionIsNotReflected() {
        val away = Vec2(1.0, 1.0).normalized()
        val out = Cushion.rebound(away, Vec2(0.0, 1.0), Speed.MEDIUM, Spin.NONE)
        assertEquals(away, out.direction)
        assertEquals(Speed.MEDIUM, out.speed)
    }

    @Test
    fun aCushionNeverSpeedsTheBallUp() {
        // Nearly parallel to the rail, where 0.92 * ~1 + rail throw could exceed 1.
        val glancing = Vec2(1.0, -1e-3).normalized()
        for (side in listOf(-1.0, 1.0)) {
            val out = Cushion.rebound(glancing, Vec2(0.0, 1.0), Speed.MEDIUM, Spin(side = side))
            assertTrue(out.speed.metersPerSecond <= Speed.MEDIUM.metersPerSecond + 1e-12, "side $side gained speed")
        }
    }

    @Test
    fun aCushionReboundLosesSpeedButKeepsDirectionSensible() {
        val incoming = Vec2(1.0, -1.0).normalized()
        val normal = Vec2(0.0, 1.0)
        val out = Cushion.rebound(incoming, normal, Speed.MEDIUM, Spin.NONE)

        assertTrue(out.speed < Speed.MEDIUM, "a cushion must remove energy")
        assertTrue(out.direction.y > 0.0, "the ball must come off the cushion")
        assertTrue(out.direction.x > 0.0, "and keep travelling the same way along it")
    }

    @Test
    fun runningEnglishAndReverseEnglishSendTheBallDifferentWays() {
        val incoming = Vec2(1.0, -1.0).normalized()
        val normal = Vec2(0.0, 1.0)
        val running = Cushion.rebound(incoming, normal, Speed.MEDIUM, Spin(side = 0.8))
        val reverse = Cushion.rebound(incoming, normal, Speed.MEDIUM, Spin(side = -0.8))
        assertTrue(
            angleBetween(running.direction, reverse.direction).degrees > 1.0,
            "english must change the rebound angle",
        )
    }

    @Test
    fun theCushionKeepsOnlyPartOfTheSideSpin() {
        val out = Cushion.rebound(Vec2(1.0, -1.0).normalized(), Vec2(0.0, 1.0), Speed.MEDIUM, Spin(side = 1.0))
        assertTrue(abs(out.spin.side) < 1.0 && abs(out.spin.side) > 0.0)
    }
}

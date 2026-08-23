package com.hereliesaz.cuedetat.core.geometry

import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.inches
import com.hereliesaz.cuedetat.core.units.meters
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TableTest {

    private val table = Table(TableSpec.NINE_FOOT)
    private val ball = TableSpec.NINE_FOOT.ball
    private val r = ball.radius

    // ── Dimensions ───────────────────────────────────────────────────────────

    @Test
    fun nineFootTableIsOneHundredByFiftyInches() {
        assertEquals(100.0, TableSpec.NINE_FOOT.playLength.inches, 1e-9)
        assertEquals(50.0, TableSpec.NINE_FOOT.playWidth.inches, 1e-9)
        // And in the unit the app actually computes in.
        assertEquals(2.54, TableSpec.NINE_FOOT.playLength.meters, 1e-9)
    }

    @Test
    fun aStandardBallIsTwoAndAQuarterInches() {
        assertEquals(2.25, BallSpec.AMERICAN_POOL.diameter.inches, 1e-9)
    }

    @Test
    fun aTableNarrowerThanItIsLongIsRejected() {
        var threw = false
        try {
            TableSpec(40.0.inches, 80.0.inches, label = "impossible")
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "playWidth exceeding playLength should be rejected")
    }

    // ── Pockets ──────────────────────────────────────────────────────────────

    @Test
    fun thereAreFourCornersAndTwoSides() {
        assertEquals(6, table.pockets.size)
        assertEquals(4, table.pockets.count { it.type == PocketType.CORNER })
        assertEquals(2, table.pockets.count { it.type == PocketType.SIDE })
    }

    @Test
    fun sidePocketsSitAtTheMidpointOfTheLongRails() {
        val sides = table.pockets.filter { it.type == PocketType.SIDE }
        for (p in sides) {
            assertEquals(0.0, p.mouthCenter.x, 1e-9)
            assertEquals(table.halfWidth.meters, abs(p.mouthCenter.y), 1e-9)
        }
    }

    @Test
    fun cornerPocketMouthsAreSetBackFromTheNominalCorner() {
        // The jaw tips lie on the rails, so the mouth centre sits inside the
        // nominal corner point. A model that put pockets *at* the corners — as
        // the previous one did — has the rails running through the pockets.
        val corner = table.pockets.first {
            it.type == PocketType.CORNER && it.mouthCenter.x > 0 && it.mouthCenter.y > 0
        }
        assertTrue(corner.mouthCenter.x < table.halfLength.meters)
        assertTrue(corner.mouthCenter.y < table.halfWidth.meters)
    }

    // ── Pocket acceptance: the behaviour a point-pocket model cannot express ──

    @Test
    fun aBallRolledStraightAtACornerPocketDrops() {
        val corner = table.pockets.first {
            it.type == PocketType.CORNER && it.mouthCenter.x > 0 && it.mouthCenter.y > 0
        }
        val approach = corner.facing
        val start = corner.mouthCenter - approach * 0.3
        assertNotNull(table.pocketAlong(start, approach, r), "a straight approach should pocket")
    }

    @Test
    fun aBallRollingAlongTheRailCannotDropInTheSidePocket() {
        // The side pocket faces square across the rail, so a ball travelling
        // along that rail arrives at 90° to the pocket axis and the mouth
        // presents nothing at all. This is the shot every player knows is
        // impossible, and a point-pocket model — which the previous code used —
        // cannot express it: the ball passes directly over the pocket centre.
        val side = table.pockets.first { it.type == PocketType.SIDE && it.mouthCenter.y > 0 }
        val alongRail = Vec2(1.0, 0.0)
        assertEquals(0.0, side.effectiveWidth(alongRail).meters, 1e-12)
        assertTrue(!side.accepts(side.mouthCenter, alongRail, r))
    }

    @Test
    fun aBallRollingAlongTheRailStillDropsInACornerPocket() {
        // The contrast that proves the model is doing geometry rather than
        // applying a blanket penalty: a corner pocket faces diagonally, so the
        // same rail-hugging ball meets it at 45° and a 4.5 inch mouth still
        // presents over 3 inches — comfortably more than a 2.25 inch ball.
        val corner = table.pockets.first {
            it.type == PocketType.CORNER && it.mouthCenter.x > 0 && it.mouthCenter.y > 0
        }
        val alongRail = Vec2(1.0, 0.0)
        assertTrue(corner.effectiveWidth(alongRail) > ball.diameter)
        assertTrue(corner.accepts(corner.mouthCenter, alongRail, r))
    }

    @Test
    fun effectivePocketWidthShrinksWithTheCosineOfTheApproachAngle() {
        val side = table.pockets.first { it.type == PocketType.SIDE && it.mouthCenter.y > 0 }
        val straight = side.effectiveWidth(side.facing)
        val at60 = side.effectiveWidth(side.facing.rotatedBy(60.0.degrees))

        assertEquals(side.mouthWidth.meters, straight.meters, 1e-9)
        assertEquals(side.mouthWidth.meters * 0.5, at60.meters, 1e-9) // cos 60 = 0.5
    }

    @Test
    fun aBallMovingAwayFromAPocketIsNeverAccepted() {
        val side = table.pockets.first { it.type == PocketType.SIDE && it.mouthCenter.y > 0 }
        assertEquals(0.0, side.effectiveWidth(-side.facing).meters, 1e-12)
        assertTrue(!side.accepts(side.mouthCenter, -side.facing, r))
    }

    @Test
    fun aBallTooFarOffCentreClipsTheJawEvenApproachingStraight() {
        val side = table.pockets.first { it.type == PocketType.SIDE && it.mouthCenter.y > 0 }
        val clearance = side.mouthWidth.meters / 2.0 - r.meters
        val lateral = side.facing.perpendicular.normalized()

        assertTrue(side.accepts(side.mouthCenter + lateral * (clearance * 0.9), side.facing, r))
        assertTrue(!side.accepts(side.mouthCenter + lateral * (clearance * 1.1), side.facing, r))
    }

    // ── Cushions ─────────────────────────────────────────────────────────────

    @Test
    fun longRailsAreSplitByTheirSidePocket() {
        // Two long rails at two segments each, plus two unbroken short rails.
        assertEquals(6, table.cushions.size)
    }

    @Test
    fun cushionCentreLineIsInsetByOneBallRadius() {
        val topRail = table.cushions.first {
            it.segment.start.y > 0 && it.inwardNormal.y < 0
        }
        val line = topRail.centerLine(r)
        assertEquals(topRail.segment.start.y - r.meters, line.start.y, 1e-12)
    }

    @Test
    fun aBallHeadingAtARailFindsThatRail() {
        // Start clear of the side-pocket gap so the ray meets cushion, not mouth.
        val x = table.halfLength.meters / 2.0
        val hit = table.firstCushionHit(Vec2(x, 0.0), Vec2(0.0, 1.0), r)
        assertNotNull(hit)
        // Contact stops one ball radius short of the cushion nose.
        assertEquals(table.halfWidth.meters - r.meters, hit.contactCenter.y, 1e-9)
    }

    @Test
    fun aBallAimedThroughTheSidePocketGapMeetsNoCushion() {
        // Straight up the middle passes through the mouth, so there is no rail
        // there to bounce off. The old rectangle model had a continuous edge
        // running across every pocket, so this ball would have banked.
        assertNull(table.firstCushionHit(Vec2.ZERO, Vec2(0.0, 1.0), r))
    }

    @Test
    fun aBallSentAtASidePocketIsPocketedRatherThanRailed() {
        val side = table.pockets.first { it.type == PocketType.SIDE && it.mouthCenter.y > 0 }
        val start = Vec2(0.0, 0.0)
        val pocket = table.pocketAlong(start, side.facing, r)
        assertNotNull(pocket, "a ball sent straight at the side pocket should drop")
        assertEquals(PocketType.SIDE, pocket.type)
    }

    @Test
    fun theSidePocketGapIsWideEnoughForABallToPassButTheRailsResumeEitherSide() {
        val longRailSegments = table.cushions.filter { it.inwardNormal.y < 0 }
        assertEquals(2, longRailSegments.size)
        val gapEdges = longRailSegments.flatMap { listOf(it.segment.start.x, it.segment.end.x) }
            .filter { abs(it) < table.halfLength.meters / 2.0 }
        // The two inner edges are the side-pocket jaws, one either side of centre.
        assertEquals(2, gapEdges.size)
        assertTrue(gapEdges.any { it < 0.0 } && gapEdges.any { it > 0.0 })
    }

    // ── Legal positions ──────────────────────────────────────────────────────

    @Test
    fun aBallTouchingTheCushionIsStillLegalButOverlappingIsNot() {
        val y = table.halfWidth.meters - r.meters
        assertTrue(table.isLegalBallPosition(Vec2(0.0, y - 1e-6)))
        assertTrue(!table.isLegalBallPosition(Vec2(0.0, y + 1e-3)))
    }

    // ── Diamonds ─────────────────────────────────────────────────────────────

    @Test
    fun diamondsRunZeroToEightAlongTheLongRail() {
        assertEquals(0.0, table.diamondsAlongLength(Vec2(-table.halfLength.meters, 0.0)), 1e-9)
        assertEquals(4.0, table.diamondsAlongLength(Vec2.ZERO), 1e-9)
        assertEquals(8.0, table.diamondsAlongLength(Vec2(table.halfLength.meters, 0.0)), 1e-9)
    }

    @Test
    fun diamondsRunZeroToFourAcrossTheShortRail() {
        assertEquals(2.0, table.diamondsAcrossWidth(Vec2.ZERO), 1e-9)
        assertEquals(4.0, table.diamondsAcrossWidth(Vec2(0.0, table.halfWidth.meters)), 1e-9)
    }
}

class PrimitiveGeometryTest {

    @Test
    fun reflectionPreservesTheAngleOfIncidence() {
        val normal = Vec2(0.0, 1.0)
        val incoming = Vec2(1.0, -1.0).normalized()
        val out = reflect(incoming, normal)

        assertEquals(incidenceAngle(incoming, normal).degrees, 45.0, 1e-9)
        assertEquals(out.x, incoming.x, 1e-12)
        assertEquals(out.y, -incoming.y, 1e-12)
    }

    @Test
    fun reflectionIsIndependentOfNormalSign() {
        val incoming = Vec2(1.0, -2.0).normalized()
        val a = reflect(incoming, Vec2(0.0, 1.0))
        val b = reflect(incoming, Vec2(0.0, -1.0))
        assertEquals(a.x, b.x, 1e-12)
        assertEquals(a.y, b.y, 1e-12)
    }

    @Test
    fun rayHitsSegmentAtTheExpectedPointAndDistance() {
        val hit = rayIntersectSegment(
            from = Vec2.ZERO,
            direction = Vec2(1.0, 0.0),
            segment = Segment(Vec2(2.0, -1.0), Vec2(2.0, 1.0)),
        )
        assertNotNull(hit)
        assertEquals(2.0, hit.point.x, 1e-12)
        assertEquals(0.0, hit.point.y, 1e-12)
        assertEquals(2.0, hit.distance.meters, 1e-12)
    }

    @Test
    fun rayParallelToSegmentDoesNotIntersect() {
        assertNull(
            rayIntersectSegment(Vec2.ZERO, Vec2(1.0, 0.0), Segment(Vec2(0.0, 1.0), Vec2(5.0, 1.0)))
        )
    }

    @Test
    fun rayPointingAwayFromSegmentDoesNotIntersect() {
        assertNull(
            rayIntersectSegment(Vec2.ZERO, Vec2(-1.0, 0.0), Segment(Vec2(2.0, -1.0), Vec2(2.0, 1.0)))
        )
    }

    @Test
    fun rayCircleIntersectionReturnsTheNearerSurfaceHit() {
        val hit = rayIntersectCircle(Vec2.ZERO, Vec2(1.0, 0.0), Vec2(5.0, 0.0), 1.0.meters)
        assertNotNull(hit)
        assertEquals(4.0, hit.distance.meters, 1e-12)
    }

    @Test
    fun rayMissingACircleReturnsNull() {
        assertNull(rayIntersectCircle(Vec2.ZERO, Vec2(1.0, 0.0), Vec2(5.0, 3.0), 1.0.meters))
    }

    @Test
    fun angleBetweenIsStableAtZeroAndPi() {
        assertEquals(0.0, angleBetween(Vec2(1.0, 0.0), Vec2(1.0, 0.0)).degrees, 1e-9)
        assertEquals(180.0, angleBetween(Vec2(1.0, 0.0), Vec2(-1.0, 0.0)).degrees, 1e-9)
        assertEquals(90.0, angleBetween(Vec2(1.0, 0.0), Vec2(0.0, 1.0)).degrees, 1e-9)
    }

    @Test
    fun closestPointOnSegmentClampsToTheEndpoints() {
        val seg = Segment(Vec2.ZERO, Vec2(1.0, 0.0))
        assertEquals(0.0, seg.closestPointTo(Vec2(-5.0, 3.0)).x, 1e-12)
        assertEquals(1.0, seg.closestPointTo(Vec2(9.0, 3.0)).x, 1e-12)
        assertEquals(0.5, seg.closestPointTo(Vec2(0.5, 3.0)).x, 1e-12)
    }

    @Test
    fun rotationByAFullTurnIsIdentity() {
        val v = Vec2(0.3, -0.7)
        val rotated = v.rotatedBy(360.0.degrees)
        assertTrue(abs(rotated.x - v.x) < 1e-12 && abs(rotated.y - v.y) < 1e-12)
    }
}

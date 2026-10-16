package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.geometry.Pocket
import com.hereliesaz.cuedetat.core.geometry.PocketType
import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.inches
import com.hereliesaz.cuedetat.core.units.meters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShotAdvisorTest {

    private val table = Table(TableSpec.NINE_FOOT)
    private val advisor = ShotAdvisor(table)

    /** A 4.5 in pocket facing +x, entered dead on its axis. */
    private val pocket = Pocket(Vec2(1.0, 0.0), 4.5.inches, Vec2(1.0, 0.0), PocketType.CORNER)

    @Test
    fun makeProbabilityMatchesAHandComputation() {
        // Effective width 0.1143 m, ball 0.05715 m => clearance 0.05715 m.
        // acceptance = 0.05715 / (2 * 1.0) = 0.028575 rad.
        // amplification = 0.5715 / 0.05715 = 10; straight => no thin-cut penalty.
        // required = 0.0028575 rad; sigma = 0.6 deg = 0.0104720 rad; z = 0.272871.
        // P = erf(z / sqrt 2) = erf(0.192949) = 0.215048.
        val p = advisor.makeProbability(pocket, Vec2(1.0, 0.0), 1.0.meters, 0.5715.meters, 0.0.degrees)
        assertEquals(0.215048, p, 1e-5)
    }

    @Test
    fun aSixtyDegreeCutHalvesTheRequiredPrecision() {
        // Thin-cut penalty 1/cos60 = 2 => z = 0.136436; erf(0.096475) = 0.108523.
        val p = advisor.makeProbability(pocket, Vec2(1.0, 0.0), 1.0.meters, 0.5715.meters, 60.0.degrees)
        assertEquals(0.108523, p, 1e-5)
    }

    @Test
    fun aPocketTooNarrowForTheBallGivesZero() {
        val tiny = pocket.copy(mouthWidth = 2.0.inches)
        assertEquals(0.0, advisor.makeProbability(tiny, Vec2(1.0, 0.0), 1.0.meters, 0.5.meters, 0.0.degrees))
    }

    @Test
    fun noTargetsMeansPlaySafe() {
        val rec = advisor.recommend(Vec2(-0.5, 0.0), emptyList())
        assertNull(rec.best)
        assertTrue(rec.ranked.isEmpty())
        assertTrue(rec.playSafe)
    }

    @Test
    fun aStraightShortPotIsRankedAndABlockerRemovesIt() {
        val corner = table.pockets.first { it.type == PocketType.CORNER }
        val mouth = corner.mouthCenter
        val axis = corner.facing.normalized()
        val objectBall = mouth - axis * 0.3
        val cueBall = mouth - axis * 0.8

        val open = advisor.recommend(cueBall, listOf(objectBall))
        assertNotNull(open.ranked.firstOrNull { it.pocket == corner }, "straight pot should be a candidate")

        val blocker = mouth - axis * 0.15
        val blocked = advisor.recommend(cueBall, listOf(objectBall), others = listOf(blocker))
        assertNull(blocked.ranked.firstOrNull { it.pocket == corner }, "blocked line must not be offered")
    }

    @Test
    fun rankingIsByDescendingProbability() {
        val rec = advisor.recommend(Vec2(-0.6, 0.1), listOf(Vec2(0.2, -0.1), Vec2(0.5, 0.3)))
        val ps = rec.ranked.map { it.makeProbability }
        assertEquals(ps.sortedDescending(), ps)
    }
}

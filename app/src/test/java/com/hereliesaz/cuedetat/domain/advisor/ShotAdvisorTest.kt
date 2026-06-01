package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ShotAdvisorTest {

    private val advisor = ShotAdvisor()
    private val R = 25f
    private val DIAG = 1000f
    private fun p(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

    @Test fun `straight pot scores higher than a cut`() {
        val target = p(0f, 0f)
        val pocket = p(0f, 300f)
        val straight = advisor.recommend(
            AdvisorInput(
                cue = p(0f, -300f), targetBalls = listOf(target),
                allBalls = listOf(p(0f, -300f), target), pockets = listOf(pocket),
                ballRadius = R, tableDiagonal = DIAG,
            )
        )
        val cut = advisor.recommend(
            AdvisorInput(
                cue = p(200f, -300f), targetBalls = listOf(target),
                allBalls = listOf(p(200f, -300f), target), pockets = listOf(pocket),
                ballRadius = R, tableDiagonal = DIAG,
            )
        )
        assertNotNull(straight)
        assertNotNull(cut)
        assertTrue(straight!!.makeProbability > cut!!.makeProbability)
        assertEquals(0f, straight.cutAngleDeg, 1f)
    }

    @Test fun `blocked path yields no recommendation`() {
        val target = p(0f, 0f)
        val pocket = p(0f, 300f)
        val cue = p(0f, -300f)
        val blocker = p(0f, -150f) // directly on the cue to ghost line
        val rec = advisor.recommend(
            AdvisorInput(
                cue = cue, targetBalls = listOf(target),
                allBalls = listOf(cue, target, blocker), pockets = listOf(pocket),
                ballRadius = R, tableDiagonal = DIAG,
            )
        )
        assertNull(rec)
    }

    @Test fun `no targets yields null`() {
        val rec = advisor.recommend(
            AdvisorInput(
                cue = p(0f, 0f), targetBalls = emptyList(), allBalls = emptyList(),
                pockets = listOf(p(0f, 300f)), ballRadius = R, tableDiagonal = DIAG,
            )
        )
        assertNull(rec)
    }

    @Test fun `with a table a clear straight pot is recommended as DIRECT`() {
        val table = Table(TableSize.NINE_FT, isVisible = false)
        val pocket = table.pockets[3] // BR corner
        // Target partway from center toward the pocket; cue lined up straight behind it.
        val target = p(pocket.x * 0.5f, pocket.y * 0.5f)
        val odLen = hypot(pocket.x - target.x, pocket.y - target.y)
        val odx = (pocket.x - target.x) / odLen
        val ody = (pocket.y - target.y) / odLen
        val cue = p(target.x - odx * (2f * R + 300f), target.y - ody * (2f * R + 300f))
        val rec = advisor.recommend(
            AdvisorInput(
                cue = cue, targetBalls = listOf(target), allBalls = listOf(cue, target),
                pockets = table.pockets, ballRadius = R,
                tableDiagonal = hypot(table.logicalWidth, table.logicalHeight), table = table,
            )
        )
        assertNotNull(rec)
        assertEquals(ShotType.DIRECT, rec!!.type)
    }

    @Test fun `prefers the straighter of two pockets`() {
        val target = p(0f, 0f)
        val cue = p(0f, -300f)
        val straightPocket = p(0f, 300f)
        val anglePocket = p(280f, 300f)
        val rec = advisor.recommend(
            AdvisorInput(
                cue = cue, targetBalls = listOf(target), allBalls = listOf(cue, target),
                pockets = listOf(anglePocket, straightPocket), ballRadius = R, tableDiagonal = DIAG,
            )
        )
        assertNotNull(rec)
        assertEquals(straightPocket.x, rec!!.pocketPos!!.x, 0.1f)
        assertEquals(straightPocket.y, rec.pocketPos!!.y, 0.1f)
    }
}

package com.hereliesaz.cuedetat.core.geometry

import com.hereliesaz.cuedetat.core.units.meters
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The pocket order is `(-,-)`, `(-,+)`, `(+,-)`, `(+,+)` (corners, long axis then short axis) and then
 * the side pockets at `-y` and `+y`: indices 0..5. Worked by hand, not read from `pockets`.
 */
class NearestPocketTest {

    private val table = Table(TableSpec.EIGHT_FOOT)
    private val hl = 1.1176 // half the long side, metres
    private val hw = 0.5588 // half the short side, metres

    @Test
    fun everyPocketMapsToItself() {
        for (index in table.pockets.indices) {
            assertEquals(index, table.nearestPocketIndex(table.pockets[index].mouthCenter))
        }
    }

    @Test
    fun aPointAtTheNominalRailCornerMapsToTheCornerPocket() {
        // The corner pocket's mouth centre is inset about 1.6 in; the rail corner itself is nearer to it
        // than to any other pocket.
        assertEquals(0, table.nearestPocketIndex(Vec2(-hl, -hw)))
        assertEquals(1, table.nearestPocketIndex(Vec2(-hl, hw)))
        assertEquals(2, table.nearestPocketIndex(Vec2(hl, -hw)))
        assertEquals(3, table.nearestPocketIndex(Vec2(hl, hw)))
    }

    @Test
    fun aPointJustPastASideRailMapsToTheSidePocket() {
        val past = 0.01 // 1 cm beyond the rail line, as a legacy table keeps its side pockets
        assertEquals(4, table.nearestPocketIndex(Vec2(0.0, -hw - past)))
        assertEquals(5, table.nearestPocketIndex(Vec2(0.0, hw + past)))
    }

    @Test
    fun aPointNearTheCentreOfALongRailIsASidePocketNotACornerOne() {
        assertEquals(5, table.nearestPocketIndex(Vec2(0.2, hw)))
        assertEquals(4, table.nearestPocketIndex(Vec2(-0.2, -hw)))
    }
}

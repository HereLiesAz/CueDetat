package com.hereliesaz.cuedetat.domain.core

import android.graphics.PointF
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.hereliesaz.cuedetat.core.geometry.Table as CoreTable

private fun pt(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

/**
 * The app's portrait, y-down logical plane against the core's long-axis-x metric one. The tables are
 * built independently on each side (the app's from inches and the ball-radius scale, the core's from a
 * TableSpec), so agreement between them is a real check of the frame, not of the conversion against
 * itself. The pocket indices are worked by hand: app order TL, TR, BL, BR, left-side, right-side; core
 * order (-,-), (-,+), (+,-), (+,+), side -y, side +y.
 */
class TableFrameTest {

    private val appTable = Table(TableSize.EIGHT_FT, isVisible = true)
    private val coreTable = CoreTable(TableSpec.EIGHT_FOOT)

    @Test
    fun theScaleHasOneSource() {
        assertEquals(TableFrameHomography.LOGICAL_UNITS_PER_METER.toDouble(), TableFrame.LOGICAL_UNITS_PER_METER, 0.0)
        assertEquals(TableFrameHomography.LOGICAL_UNITS_PER_INCH.toDouble(), TableFrame.LOGICAL_UNITS_PER_INCH, 0.0)
        assertEquals(TableFrame.LOGICAL_UNITS_PER_INCH, CoreBridge.LOGICAL_UNITS_PER_INCH, 0.0)
        assertEquals(TableFrame.LOGICAL_UNITS_PER_METER, CoreBridge.LOGICAL_UNITS_PER_METER, 0.0)
    }

    @Test
    fun aPointSurvivesTheRoundTrip() {
        for ((x, y) in listOf(0f to 0f, 123.5f to -987.25f, -400f to 950f)) {
            val back = TableFrame.toLogical(TableFrame.toCore(pt(x, y)))
            assertEquals(x, back.x, 1e-3f)
            assertEquals(y, back.y, 1e-3f)
        }
    }

    @Test
    fun theAppTablesLongAxisIsTheCoreTablesLongAxis() {
        // The app table is portrait: height is the long side. Its foot rail's centre is on the core
        // table's boundary along x; a bit further is off it. The long side taken across is off at once.
        val halfLong = appTable.logicalHeight / 2f
        val halfShort = appTable.logicalWidth / 2f
        assertTrue(coreTable.containsCenter(TableFrame.toCore(pt(0f, halfLong - 0.5f))))
        assertFalse(coreTable.containsCenter(TableFrame.toCore(pt(0f, halfLong + 0.5f))))
        assertTrue(coreTable.containsCenter(TableFrame.toCore(pt(halfShort - 0.5f, 0f))))
        assertFalse(coreTable.containsCenter(TableFrame.toCore(pt(halfShort + 0.5f, 0f))))
        assertFalse(coreTable.containsCenter(TableFrame.toCore(pt(halfLong - 0.5f, 0f))))
    }

    @Test
    fun everyAppPocketMapsToTheCorePocketInTheSamePlace() {
        // Worked by hand through x_core = y_app, y_core = x_app: TL (-,-) -> (-,-) = 0, TR (+x,-y) ->
        // (-,+) = 1, BL (-x,+y) -> (+,-) = 2, BR (+,+) -> (+,+) = 3, the side pockets at -x and +x ->
        // the side pockets at -y and +y = 4 and 5. Coincidentally the identity.
        val expected = listOf(0, 1, 2, 3, 4, 5)
        assertEquals(6, appTable.pockets.size)
        appTable.pockets.forEachIndexed { index, pocket ->
            assertEquals("app pocket $index", expected[index], TableFrame.corePocketIndex(coreTable, pocket))
        }
    }

    @Test
    fun lengthsCrossAtTheScale() {
        // One inch is LOGICAL_UNITS_PER_INCH logical units: 50 / 2.25.
        val inch = TableFrame.toLength(50f / 2.25f)
        assertEquals(0.0254, inch.meters, 1e-6)
        assertEquals(50f / 2.25f, TableFrame.toLogical(inch), 1e-3f)
    }
}

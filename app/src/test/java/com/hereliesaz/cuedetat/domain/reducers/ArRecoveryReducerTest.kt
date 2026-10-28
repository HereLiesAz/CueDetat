package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MAX_TABLE_FIT_SCALE
import com.hereliesaz.cuedetat.domain.MIN_TABLE_FIT_SCALE
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ArRecoveryReducerTest {

    @Test
    fun `a measured table size is applied and kept in range`() {
        assertEquals(1.1f, reduceControlAction(CueDetatState(), MainScreenEvent.SetTableFitScale(1.1f)).tableFitScale, 1e-5f)
        assertEquals(MAX_TABLE_FIT_SCALE, reduceControlAction(CueDetatState(), MainScreenEvent.SetTableFitScale(9f)).tableFitScale, 1e-5f)
        assertEquals(MIN_TABLE_FIT_SCALE, reduceControlAction(CueDetatState(), MainScreenEvent.SetTableFitScale(0f)).tableFitScale, 1e-5f)
    }

    @Test
    fun `a tracking notice shows and a null clears it`() {
        val shown = reduceControlAction(CueDetatState(), MainScreenEvent.ArNotice("Tracking lost: too dark."))
        assertEquals("Tracking lost: too dark.", shown.warningText)
        assertNull(reduceControlAction(shown, MainScreenEvent.ArNotice(null)).warningText)
    }

    @Test
    fun `losing the locked anchor unlocks the table and says so`() {
        val out = reduceControlAction(
            CueDetatState(isArTableLocked = true),
            MainScreenEvent.ArTableLockLost("Tracking lost the locked table."),
        )
        assertFalse(out.isArTableLocked)
        assertEquals("Tracking lost the locked table.", out.warningText)
    }
}

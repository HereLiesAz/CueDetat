package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableReseedPolicy.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class TableReseedPolicyTest {

    @Test
    fun `a fit that agrees verifies the table`() {
        assertEquals(Verdict.VERIFIED, TableReseedPolicy().onFit(0.95f, 0.9f, userBusy = false))
    }

    @Test
    fun `a weak fit says nothing however much it disagrees`() {
        val p = TableReseedPolicy()
        repeat(10) { assertEquals(Verdict.NONE, p.onFit(0.1f, 0.5f, userBusy = false)) }
    }

    @Test
    fun `three sure disagreeing fits in a row reseed`() {
        val p = TableReseedPolicy()
        assertEquals(Verdict.NONE, p.onFit(0.3f, 0.9f, false))
        assertEquals(Verdict.NONE, p.onFit(0.3f, 0.9f, false))
        assertEquals(Verdict.RESEED, p.onFit(0.3f, 0.9f, false))
    }

    @Test
    fun `one agreeing fit breaks the run`() {
        val p = TableReseedPolicy()
        p.onFit(0.3f, 0.9f, false); p.onFit(0.3f, 0.9f, false)
        p.onFit(0.95f, 0.9f, false)
        assertEquals(Verdict.NONE, p.onFit(0.3f, 0.9f, false))
    }

    @Test
    fun `the user's hand holds the judgement off`() {
        val p = TableReseedPolicy()
        repeat(10) { assertEquals(Verdict.NONE, p.onFit(0.3f, 0.9f, userBusy = true)) }
    }

    @Test
    fun `it gives up after its reseeds`() {
        val p = TableReseedPolicy(maxReseeds = 2)
        var reseeds = 0
        repeat(30) { if (p.onFit(0.3f, 0.9f, false) == Verdict.RESEED) reseeds++ }
        assertEquals(2, reseeds)
    }

    @Test
    fun `reset starts a new window`() {
        val p = TableReseedPolicy(maxReseeds = 1)
        repeat(3) { p.onFit(0.3f, 0.9f, false) }
        p.reset()
        repeat(2) { p.onFit(0.3f, 0.9f, false) }
        assertEquals(Verdict.RESEED, p.onFit(0.3f, 0.9f, false))
    }
}

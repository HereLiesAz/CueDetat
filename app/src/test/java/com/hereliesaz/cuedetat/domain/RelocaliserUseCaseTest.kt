package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelocaliserUseCaseTest {

    private val useCase = RelocaliserUseCase()

    private fun uniformHist(value: Float): List<Float> = List(16) { value / 16f }

    @Test
    fun identicalHistogramsPassValidation() {
        val hist = uniformHist(1f)
        val saved = mapOf(
            PocketId.TL to hist, PocketId.TR to hist,
            PocketId.BL to hist, PocketId.BR to hist,
            PocketId.SL to hist, PocketId.SR to hist
        )
        assertTrue(useCase.validateHistograms(saved, saved))
    }

    @Test
    fun totallyDifferentHistogramsFailValidation() {
        val histA = List(16) { i -> if (i < 8) 0.25f else 0f }
        val histB = List(16) { i -> if (i >= 8) 0.25f else 0f }
        val saved = mapOf(PocketId.TL to histA, PocketId.TR to histA,
            PocketId.BL to histA, PocketId.BR to histA,
            PocketId.SL to histA, PocketId.SR to histA)
        val current = mapOf(PocketId.TL to histB, PocketId.TR to histB,
            PocketId.BL to histB, PocketId.BR to histB,
            PocketId.SL to histB, PocketId.SR to histB)
        assertFalse(useCase.validateHistograms(saved, current))
    }

    @Test
    fun emptyReferenceAlwaysPasses() {
        assertTrue(useCase.validateHistograms(emptyMap(), emptyMap()))
    }

    @Test
    fun fourOutOfSixMatchesSuffice() {
        val good = uniformHist(1f)
        val bad = List(16) { 0f }.toMutableList().also { it[0] = 1f }
        val saved = mapOf(PocketId.TL to good, PocketId.TR to good,
            PocketId.BL to good, PocketId.BR to good,
            PocketId.SL to bad, PocketId.SR to bad)
        val current = mapOf(PocketId.TL to good, PocketId.TR to good,
            PocketId.BL to good, PocketId.BR to good,
            PocketId.SL to good, PocketId.SR to good)
        assertTrue(useCase.validateHistograms(saved, current))
    }
}

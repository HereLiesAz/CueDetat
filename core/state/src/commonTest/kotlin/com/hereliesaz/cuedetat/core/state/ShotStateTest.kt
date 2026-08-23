package com.hereliesaz.cuedetat.core.state

import com.hereliesaz.cuedetat.core.physics.Spin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShotStateTest {

    @Test
    fun bankingAndMasseCannotBothBeActive() {
        // The old state had these as independent booleans with no mutual
        // exclusion, and both nav-rail toggles were live: tapping masse then bank
        // left both true, the aiming pass silently resolved banking and skipped
        // masse, and the masse dial still rendered over the bank UI.
        val state = ShotState().toggleMasse()
        assertTrue(state.isMasse && !state.isBanking)

        val afterBank = state.toggleBanking()
        assertTrue(afterBank.isBanking, "banking should now be the mode")
        assertTrue(!afterBank.isMasse, "and masse must have been replaced, not left set")
    }

    @Test
    fun togglingAModeOffReturnsToADirectShot() {
        assertEquals(ShotMode.Direct, ShotState().toggleBanking().toggleBanking().mode)
        assertEquals(ShotMode.Direct, ShotState().toggleMasse().toggleMasse().mode)
    }

    @Test
    fun experienceModeIsATwoStateToggleWithNoJokeInTheMiddle() {
        // Hater mode used to be the third value of this cycle, so getting from
        // Beginner to Expert meant passing through an unrelated product.
        assertEquals(ExperienceMode.EXPERT, ExperienceMode.BEGINNER.toggled())
        assertEquals(ExperienceMode.BEGINNER, ExperienceMode.EXPERT.toggled())
        assertEquals(2, ExperienceMode.entries.size)
    }

    @Test
    fun strokeStateSanitisesAnImpossibleTipOffset() {
        val wild = StrokeState(spin = Spin(side = 5.0, vertical = -5.0))
        assertTrue(wild.spin.wouldMiscue)
        assertTrue(!wild.sanitized().spin.wouldMiscue)
    }

    @Test
    fun bankModeCarriesItsOwnAimTargetRatherThanASharedField() {
        val banking = ShotState().withMode(ShotMode.Bank(maxCushions = 2))
        val mode = banking.mode
        assertTrue(mode is ShotMode.Bank && mode.maxCushions == 2)
    }
}

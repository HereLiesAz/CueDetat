package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.geometry.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupRulesTest {

    private fun ball(id: Int, group: BallGroup) = TableBall(id, Vec2(id * 0.1, 0.0), group)

    private val mine1 = ball(1, BallGroup.MINE)
    private val mine2 = ball(2, BallGroup.MINE)
    private val theirs = ball(3, BallGroup.THEIRS)
    private val eight = ball(4, BallGroup.EIGHT)
    private val unknown = ball(5, BallGroup.UNKNOWN)

    @Test
    fun whileYourGroupStandsOnlyYourGroupIsPottable() {
        val table = listOf(mine1, mine2, theirs, eight, unknown)
        assertEquals(listOf(1, 2), GroupRules.pottable(table).map { it.id })
    }

    @Test
    fun onceYourGroupIsClearedTheEightIsTheTarget() {
        val table = listOf(theirs, eight, unknown)
        assertEquals(listOf(4), GroupRules.pottable(table).map { it.id })
    }

    @Test
    fun withNothingLegalNothingIsPottable() {
        assertTrue(GroupRules.pottable(listOf(theirs, unknown)).isEmpty())
        assertTrue(GroupRules.pottable(emptyList()).isEmpty())
    }

    @Test
    fun theFirstBallStruckMustBeLegalToo() {
        val table = listOf(mine1, theirs, eight)
        assertEquals(listOf(1), GroupRules.legalFirstContacts(table).map { it.id })
    }

    @Test
    fun aCombinationNeedsTwoDifferentBallsOfYours() {
        assertTrue(GroupRules.isLegalCombination(first = mine1, pocketed = mine2))
        assertFalse(GroupRules.isLegalCombination(first = mine1, pocketed = mine1))
    }

    @Test
    fun anOpponentsOrUnclassifiedFirstBallIsAFoul() {
        // The hole in the old advisor: the first ball could be anyone's.
        assertFalse(GroupRules.isLegalCombination(first = theirs, pocketed = mine1))
        assertFalse(GroupRules.isLegalCombination(first = unknown, pocketed = mine1))
    }

    @Test
    fun theEightIsNeverPartOfACombination() {
        assertFalse(GroupRules.isLegalCombination(first = eight, pocketed = mine1))
        assertFalse(GroupRules.isLegalCombination(first = mine1, pocketed = eight))
        // Even two balls misread as the 8.
        assertFalse(GroupRules.isLegalCombination(first = eight, pocketed = ball(6, BallGroup.EIGHT)))
    }

    @Test
    fun pocketingAnOpponentsBallIsNotWorthOffering() {
        assertFalse(GroupRules.isLegalCombination(first = mine1, pocketed = theirs))
    }
}

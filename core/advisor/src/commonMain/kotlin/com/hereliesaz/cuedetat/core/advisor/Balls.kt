package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.geometry.Vec2

/** Whose ball it is, as far as the shooter's game is concerned. */
enum class BallGroup {
    /** The shooter's own group: the balls they may pot, and may strike first. */
    MINE,

    /** The other player's group. Never a target; an obstacle. */
    THEIRS,

    /** The 8-ball. Legal to strike and pot only once none of [MINE] remain. */
    EIGHT,

    /** Not classified (a detection that could not say). An obstacle, never a target. */
    UNKNOWN,
}

/**
 * A ball with an identity. The advisor used to take bare positions, which cannot say "the first ball
 * struck must be yours", so a combination could be recommended that hit the opponent's ball first.
 * [id] is the caller's own and only has to be unique among the balls handed in together; positions are
 * not identities (two balls frozen together are two balls).
 */
data class TableBall(val id: Int, val position: Vec2, val group: BallGroup)

/**
 * Which balls the shooter may strike first and pot, under 8-ball rules: their own group, and the 8
 * only once none of their group remain. Pure functions over what is on the table now.
 *
 * Not modelled: calling pockets, ball in hand, the break, scratches as fouls (only as bad leaves, and
 * the advisor does not look at the leave yet), 9-ball's lowest-ball rule.
 */
object GroupRules {

    /** The balls that may be potted, and may be struck first: [BallGroup.MINE] while any remain, else the 8. */
    fun pottable(balls: List<TableBall>): List<TableBall> {
        val mine = balls.filter { it.group == BallGroup.MINE }
        return if (mine.isNotEmpty()) mine else balls.filter { it.group == BallGroup.EIGHT }
    }

    /** The balls that may be struck first. The same set as [pottable] under 8-ball rules. */
    fun legalFirstContacts(balls: List<TableBall>): List<TableBall> = pottable(balls)

    /**
     * Whether a combination, the cue striking [first], which drives [pocketed] into a pocket, is legal
     * and worth offering: both are the shooter's own group and they are different balls. The 8 is never
     * either: while the group stands it is not pottable, and once it is cleared the 8 is the only
     * legal first contact, so there is nothing for it to drive. An opponent's or an unclassified ball
     * as the first ball is a foul.
     */
    fun isLegalCombination(first: TableBall, pocketed: TableBall): Boolean =
        first.id != pocketed.id && first.group == BallGroup.MINE && pocketed.group == BallGroup.MINE
}

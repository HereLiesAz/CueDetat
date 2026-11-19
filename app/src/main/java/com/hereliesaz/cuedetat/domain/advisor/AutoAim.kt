package com.hereliesaz.cuedetat.domain.advisor

import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.TableOrientationLearner
import com.hereliesaz.cuedetat.view.state.InteractionMode
import kotlin.math.atan2

/**
 * The advisor aims the protractor: the target ball, the ghost cue ball and the aim line follow the
 * recommended shot, until the user takes the aim by hand.
 *
 * **Owner and override.** The advisor owns the aim while it is on and [CueDetatState.isAutoAimPaused]
 * is false. Dragging the target ball or rotating the aim sets that flag (`GestureReducer`), and the
 * advisor stops moving them; the Auto-aim rail item (`ResumeAutoAim`) and switching the advisor off
 * and on again hand the aim back. A recommendation that arrives while the user is mid-gesture is
 * recorded but not applied, so it never moves a ball under their finger.
 *
 * **Which shots aim.** The protractor can only express a cue ball that travels straight to the ghost
 * ball: direct pots, banks (the struck ball's rebound is the gold line's business) and combinations
 * (the struck ball is the target; the second leg is the gold line's). A kick sends the cue off a
 * cushion first, so the straight line the protractor would draw to the ghost is not the shot; kicks
 * are shown by the recommendation overlay alone and leave the aim where it is.
 *
 * **The angle.** The protractor puts the ghost at `center + 2r * (-sin a, cos a)`, so the aim that
 * puts it at the recommended ghost position is `a = atan2(-dx, dy)` for `(dx, dy)` from the target to
 * the ghost. It is taken as the equivalent angle nearest the current one, so the aim turns the short
 * way and keeps its winding.
 *
 * **Stale pull.** The target's CV anchor is cleared, so [com.hereliesaz.cuedetat.domain.reducers.SnapReducer]
 * cannot pull the target back to the ball it was last snapped to. The recommendation follows the balls
 * itself (the driver recomputes when they move). Balls and the ghost are kept on the table by
 * `BallBounds`; the planner offers no shot whose ghost is off it, so that has nothing to move here.
 */
object AutoAim {

    /** Whether the advisor currently owns the aim. */
    fun isEngaged(state: CueDetatState): Boolean =
        state.isAdvisorEnabled &&
            !state.isAutoAimPaused &&
            !state.isBankingMode &&
            !state.isMasseModeActive &&
            state.ballSelectionPhase == BallSelectionPhase.NONE

    /** Whether the protractor can show this shot: the cue goes straight to the ghost ball. */
    fun canAim(shot: RecommendedShot): Boolean = shot.type != ShotType.KICK

    /** Whether the aim on screen is the advisor's: engaged, with an aimable recommendation. */
    fun drives(state: CueDetatState): Boolean =
        isEngaged(state) && state.recommendedShot?.let(::canAim) == true

    /** [state] with the target and the aim moved to its recommended shot, when the advisor may. */
    fun apply(state: CueDetatState): CueDetatState {
        val shot = state.recommendedShot ?: return state
        if (!isEngaged(state) || !canAim(shot)) return state
        if (state.interactionMode != InteractionMode.NONE) return state

        val unit = state.protractorUnit
        val dx = shot.ghostCuePos.x - shot.targetPos.x
        val dy = shot.ghostCuePos.y - shot.targetPos.y
        val wanted = Math.toDegrees(atan2(-dx.toDouble(), dy.toDouble())).toFloat()
        val rotation = unit.rotationDegrees + TableOrientationLearner.wrapSigned180(wanted - unit.rotationDegrees)

        return state.copy(
            protractorUnit = unit.copy(center = shot.targetPos, rotationDegrees = rotation),
            targetCvAnchor = null,
        )
    }
}

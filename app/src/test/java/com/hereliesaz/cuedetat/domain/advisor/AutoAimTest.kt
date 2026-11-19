package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.domain.stateReducer
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

/**
 * The advisor aiming the protractor, and the user's hold on it. The protractor puts the ghost at
 * `center + 50 * (-sin a, cos a)` (radius 25); that is worked here by hand rather than through
 * `ProtractorUnit.ghostCueBallCenter`, which builds a `PointF` the unit-test stubs cannot hold.
 */
class AutoAimTest {

    private val gestures = GestureReducer()
    private val utils = ReducerUtils()

    private fun shot(target: PointF, ghost: PointF, type: ShotType = ShotType.DIRECT) = RecommendedShot(
        type = type, targetPos = target, pocketPos = pf(0f, 900f), ghostCuePos = ghost, cutAngleDeg = 0f,
        hardness = Hardness.MEDIUM, spin = pf(0f, 0f), makeProbability = 0.5f, confidence = 0.5f,
        shotPath = listOf(ghost, target),
    )

    private fun armed(shot: RecommendedShot?, rotation: Float = 0f) = CueDetatState().copy(
        isAdvisorEnabled = true,
        recommendedShot = shot,
        protractorUnit = ProtractorUnit(pf(-200f, -300f), 25f, rotation),
    )

    private fun ghostOf(s: CueDetatState): PointF {
        val a = Math.toRadians(s.protractorUnit.rotationDegrees.toDouble())
        return pf(
            s.protractorUnit.center.x - (50 * sin(a)).toFloat(),
            s.protractorUnit.center.y + (50 * cos(a)).toFloat(),
        )
    }

    @Test
    fun `the target and the aim follow the recommended shot, whichever way it runs`() {
        val target = pf(100f, 200f)
        // Ghost 50 away at several bearings: below, left, above, right, and a diagonal.
        for ((dx, dy) in listOf(0f to 50f, -50f to 0f, 0f to -50f, 50f to 0f, 35.355f to 35.355f)) {
            val ghost = pf(target.x + dx, target.y + dy)
            val out = AutoAim.apply(armed(shot(target, ghost)))
            assertEquals(target.x, out.protractorUnit.center.x, 1e-3f)
            assertEquals(target.y, out.protractorUnit.center.y, 1e-3f)
            assertEquals("ghost x for ($dx, $dy)", ghost.x, ghostOf(out).x, 0.01f)
            assertEquals("ghost y for ($dx, $dy)", ghost.y, ghostOf(out).y, 0.01f)
        }
    }

    @Test
    fun `the aim turns the short way and keeps its winding`() {
        // Ghost to the left of the target is aim 90. From 80 that is +10; from 450 (a turn and a quarter on)
        // it is 450, not 90.
        val shotLeft = shot(pf(0f, 0f), pf(-50f, 0f))
        assertEquals(90f, AutoAim.apply(armed(shotLeft, rotation = 80f)).protractorUnit.rotationDegrees, 0.01f)
        assertEquals(450f, AutoAim.apply(armed(shotLeft, rotation = 450f)).protractorUnit.rotationDegrees, 0.01f)
        assertEquals(-270f, AutoAim.apply(armed(shotLeft, rotation = -280f)).protractorUnit.rotationDegrees, 0.01f)
    }

    @Test
    fun `a stale anchor cannot pull the target back`() {
        val state = armed(shot(pf(10f, 10f), pf(10f, 60f))).copy(targetCvAnchor = pf(-200f, -300f))
        assertNull(AutoAim.apply(state).targetCvAnchor)
    }

    @Test
    fun `nothing moves when the advisor is off, paused, mid-gesture, banking, or has nothing`() {
        val s = armed(shot(pf(10f, 10f), pf(10f, 60f)))
        for (blocked in listOf(
            s.copy(isAdvisorEnabled = false),
            s.copy(isAutoAimPaused = true),
            s.copy(interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL),
            s.copy(isBankingMode = true),
            s.copy(isMasseModeActive = true),
            s.copy(recommendedShot = null),
        )) {
            assertSame(blocked, AutoAim.apply(blocked))
        }
    }

    @Test
    fun `a kick is shown by the overlay and does not aim the protractor`() {
        val s = armed(shot(pf(10f, 10f), pf(10f, 60f), ShotType.KICK))
        assertSame(s, AutoAim.apply(s))
        assertFalse(AutoAim.drives(s))
        assertTrue(AutoAim.drives(armed(shot(pf(10f, 10f), pf(10f, 60f), ShotType.COMBO))))
    }

    @Test
    fun `a new recommendation becomes the aim through the router, but not under the user's finger`() {
        val rec = shot(pf(100f, 200f), pf(100f, 250f))
        val base = CueDetatState().copy(isAdvisorEnabled = true, protractorUnit = ProtractorUnit(pf(-200f, -300f), 25f, 40f))

        val aimed = stateReducer(base, MainScreenEvent.RecommendationComputed(rec), utils, gestures)
        assertSame(rec, aimed.recommendedShot)
        assertEquals(100f, aimed.protractorUnit.center.x, 1e-3f)
        assertEquals(0f, aimed.protractorUnit.rotationDegrees, 0.01f)

        val busy = stateReducer(
            base.copy(interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL),
            MainScreenEvent.RecommendationComputed(rec), utils, gestures,
        )
        assertSame(rec, busy.recommendedShot)
        assertEquals(-200f, busy.protractorUnit.center.x, 1e-3f)
        assertEquals(40f, busy.protractorUnit.rotationDegrees, 1e-3f)
    }

    @Test
    fun `dragging the target or the aim pauses auto-aim`() {
        val drag = MainScreenEvent.LogicalDragApplied(pf(0f, 0f), pf(5f, 5f), Offset.Zero)
        val base = armed(shot(pf(10f, 10f), pf(10f, 60f)))
        assertFalse(base.isAutoAimPaused)
        for (mode in listOf(InteractionMode.MOVING_PROTRACTOR_UNIT, InteractionMode.ROTATING_PROTRACTOR)) {
            val out = gestures.reduce(base.copy(interactionMode = mode), drag)
            assertTrue("$mode pauses", out.isAutoAimPaused)
        }
        // Moving the cue ball is not taking the aim.
        assertFalse(gestures.reduce(base.copy(interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL), drag).isAutoAimPaused)
    }

    @Test
    fun `resuming hands the aim back at once, and switching the advisor on starts armed`() {
        val rec = shot(pf(100f, 200f), pf(100f, 250f))
        val paused = armed(rec).copy(isAutoAimPaused = true)

        val resumed = stateReducer(paused, MainScreenEvent.ResumeAutoAim, utils, gestures)
        assertFalse(resumed.isAutoAimPaused)
        assertEquals(100f, resumed.protractorUnit.center.x, 1e-3f)

        val off = paused.copy(isAdvisorEnabled = false)
        val on = stateReducer(off, MainScreenEvent.ToggleAdvisor, utils, gestures)
        assertTrue(on.isAdvisorEnabled)
        assertFalse(on.isAutoAimPaused)
    }
}

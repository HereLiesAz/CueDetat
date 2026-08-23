package com.hereliesaz.cuedetat.core.state

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.physics.ClothSpec
import com.hereliesaz.cuedetat.core.physics.CueSpec
import com.hereliesaz.cuedetat.core.physics.Spin
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees

/**
 * The state of the shot being planned.
 *
 * ## Why this is a sealed type
 *
 * The previous state was a single flat data class with 122 fields, in which
 * banking and massé were independent `Boolean`s. Nothing enforced that they
 * were mutually exclusive, and the enforcement had already lapsed: both nav-rail
 * toggles were live with no `enabled` guard, the aiming pass resolved banking
 * first and skipped massé entirely, and the massé dial still rendered on top of
 * the bank-shot UI and still wrote state as it was dragged.
 *
 * A sum type makes that state unrepresentable instead of merely undocumented.
 * There is no combination of [ShotMode] values, so there is no combination to
 * get wrong.
 */
sealed interface ShotMode {

    /** A normal shot: strike the object ball directly. */
    data object Direct : ShotMode

    /**
     * Bank or kick: send the cue ball off one or more cushions first.
     * [aimTarget] is where the player has dragged the bank aim, in table metres.
     */
    data class Bank(val aimTarget: Vec2? = null, val maxCushions: Int = 3) : ShotMode

    /**
     * Massé: an elevated cue curving the ball around an obstruction.
     * [elevation] is the cue's angle above the cloth.
     */
    data class Masse(val elevation: Angle = 45.0.degrees) : ShotMode
}

/**
 * How the player is currently manipulating the scene. Transient — this is
 * gesture state, not shot state, and it resets when the finger lifts.
 */
sealed interface Interaction {
    data object Idle : Interaction
    data object MovingCueBall : Interaction
    data object MovingObjectBall : Interaction
    data class MovingObstacle(val index: Int) : Interaction
    data object AdjustingSpin : Interaction
    data object AdjustingBankTarget : Interaction
    data object PanningView : Interaction
}

/**
 * How much the app shows and assumes.
 *
 * Hater mode is deliberately absent. It was a co-equal third value in the old
 * `ExperienceMode` cycle, which meant the single most load-bearing control in
 * the nav rail — "cycle mode" — put a physics-dice joke screen between Beginner
 * and Expert. Getting from one working mode to the other required passing
 * through an unrelated product. It now has its own entry point.
 */
enum class ExperienceMode {
    /** Fewer lines, locked view, more explanation. */
    BEGINNER,

    /** Everything, no hand-holding. Free, like the rest of the app. */
    EXPERT,
    ;

    fun toggled(): ExperienceMode = if (this == BEGINNER) EXPERT else BEGINNER
}

/** The balls in play, in table metres. */
data class BallsState(
    val cueBall: Vec2 = Vec2(-0.6, 0.0),
    val objectBall: Vec2 = Vec2(0.0, 0.0),
    val obstacles: List<Vec2> = emptyList(),
    val spec: BallSpec = BallSpec.AMERICAN_POOL,
)

/** How the shot will be struck. */
data class StrokeState(
    val spin: Spin = Spin.NONE,
    val speed: Speed = Speed.MEDIUM,
    val cue: CueSpec = CueSpec.STANDARD,
) {
    /** Clamps the tip offset into the miscue circle before anything uses it. */
    fun sanitized(): StrokeState = copy(spin = spin.clampedToMiscueLimit())
}

/** The surface being played on. */
data class TableState(
    val spec: TableSpec = TableSpec.NINE_FOOT,
    val cloth: ClothSpec = ClothSpec.WORSTED,
    val isVisible: Boolean = true,
) {
    val table: Table get() = Table(spec)
}

/**
 * The complete planning state.
 *
 * Note what is *not* here: no matrices, no bitmaps, no OpenCV handles, no
 * derived aiming lines, no dialog flags, no snapshot of a previous copy of
 * itself. Derived values are computed by the solvers from this input, and view
 * concerns live in the view layer. The old state class carried all of those,
 * which is why it needed a hand-maintained 122-entry list to compare two
 * instances, and why one Reset press pinned a full nested snapshot — including
 * whatever `Bitmap` and `Mat` happened to be live — in memory for the rest of
 * the session.
 */
data class ShotState(
    val balls: BallsState = BallsState(),
    val stroke: StrokeState = StrokeState(),
    val tableState: TableState = TableState(),
    val mode: ShotMode = ShotMode.Direct,
    val experience: ExperienceMode = ExperienceMode.BEGINNER,
    val interaction: Interaction = Interaction.Idle,
    val showHelperLabels: Boolean = true,
) {
    val isBanking: Boolean get() = mode is ShotMode.Bank
    val isMasse: Boolean get() = mode is ShotMode.Masse

    /**
     * Switching mode is a replacement, not a flag flip, so the previous mode's
     * settings cannot survive into a mode that does not use them.
     */
    fun withMode(next: ShotMode): ShotState = copy(mode = next)

    fun toggleBanking(): ShotState =
        withMode(if (mode is ShotMode.Bank) ShotMode.Direct else ShotMode.Bank())

    fun toggleMasse(): ShotState =
        withMode(if (mode is ShotMode.Masse) ShotMode.Direct else ShotMode.Masse())
}

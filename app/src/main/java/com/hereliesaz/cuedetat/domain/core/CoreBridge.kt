package com.hereliesaz.cuedetat.domain.core

import android.graphics.PointF
import com.hereliesaz.cuedetat.core.aim.AimSolver
import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.physics.CueSpec
import com.hereliesaz.cuedetat.core.physics.Spin
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.METERS_PER_INCH
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.state.TableSize

/**
 * The seam between the app's legacy logical plane and the metric core.
 *
 * ## Why a bridge rather than a rewrite
 *
 * The app still computes and renders in the old dimensionless plane, where
 * `LOGICAL_BALL_RADIUS = 25f` is the unit and a real 2¼-inch ball is scaled into
 * it. The rebuilt core computes in metres. Converting the entire renderer in one
 * step is a large, risky change that deserves its own pass with a compiler and a
 * device in the loop.
 *
 * This bridge lets the two coexist honestly in the meantime: the app keeps its
 * coordinates, and anything that needs to be *physically correct* — the aim, the
 * throw compensation, whether a pocket will actually accept the ball — is
 * answered by the core, in real units, and converted back at the boundary.
 *
 * The conversion is exact, not approximate: the old plane's scale is defined as
 * `(LOGICAL_BALL_RADIUS * 2) / 2.25 inches`, so one logical unit is a fixed,
 * knowable length. That it *is* knowable is the whole point — the old code never
 * wrote it down anywhere, which is how spin constants ended up specified "per
 * logical unit" and the distance readout ended up as `1200 / screenRadius`.
 */
object CoreBridge {

    /** Ball diameter the legacy plane was scaled around. */
    private const val REFERENCE_BALL_DIAMETER_INCHES = 2.25

    /** Logical units per inch, from the legacy plane's own definition. */
    const val LOGICAL_UNITS_PER_INCH: Double =
        (LOGICAL_BALL_RADIUS * 2.0) / REFERENCE_BALL_DIAMETER_INCHES

    /** Logical units per metre. */
    const val LOGICAL_UNITS_PER_METER: Double = LOGICAL_UNITS_PER_INCH / METERS_PER_INCH

    // ── Conversions ─────────────────────────────────────────────────────────

    fun Float.logicalToLength(): Length = Length(this / LOGICAL_UNITS_PER_METER)

    fun Length.toLogical(): Float = (meters * LOGICAL_UNITS_PER_METER).toFloat()

    /** A legacy logical point as a metric table-plane point. */
    fun PointF.toTablePlane(): Vec2 =
        Vec2(x / LOGICAL_UNITS_PER_METER, y / LOGICAL_UNITS_PER_METER)

    /** A metric table-plane point back in legacy logical coordinates. */
    fun Vec2.toLogicalPoint(): PointF =
        PointF((x * LOGICAL_UNITS_PER_METER).toFloat(), (y * LOGICAL_UNITS_PER_METER).toFloat())

    // ── Specs ───────────────────────────────────────────────────────────────

    fun tableSpecFor(size: TableSize): TableSpec = when (size) {
        TableSize.SEVEN_FT -> TableSpec.SEVEN_FOOT
        TableSize.EIGHT_FT -> TableSpec.EIGHT_FOOT
        TableSize.NINE_FT -> TableSpec.NINE_FOOT
    }

    /**
     * Reads the tip offset the spin dial has set.
     *
     * The dial's `selectedSpinOffset` is already normalised to −1..1 in each
     * axis, which is exactly the miscue-limit convention [Spin] uses, so this is
     * a rename rather than a conversion. It is clamped into the miscue circle
     * because the dial's corners are outside it — you cannot strike a ball at
     * full right *and* full draw.
     */
    fun spinFrom(state: CueDetatState): Spin {
        val offset = state.selectedSpinOffset ?: state.lingeringSpinOffset ?: return Spin.NONE
        return Spin(side = offset.x.toDouble(), vertical = offset.y.toDouble())
            .clampedToMiscueLimit()
    }

    // ── The one thing worth crossing the boundary for ───────────────────────

    /**
     * Solves the aim for the current state, in real units, including
     * cut-induced throw and squirt.
     *
     * Returns `null` when the state has no cue ball placed — in dynamic beginner
     * mode the shot line is anchored to the bottom of the screen rather than to
     * a ball, and there is nothing physical to solve.
     */
    fun solveAim(
        state: CueDetatState,
        target: PointF,
        speed: Speed = Speed.MEDIUM,
        cue: CueSpec = CueSpec.STANDARD,
    ): AimSolver.Solution? {
        val cueBall = state.onPlaneBall?.center ?: return null
        return AimSolver.solve(
            cueBall = cueBall.toTablePlane(),
            objectBall = state.protractorUnit.center.toTablePlane(),
            target = target.toTablePlane(),
            speed = speed,
            spin = spinFrom(state),
            cue = cue,
            ball = BallSpec.AMERICAN_POOL,
        )
    }

    /**
     * The throw the current shot will suffer, for display.
     *
     * This is a number the old app could not produce at all, and it is the one
     * players most need: it says how far off the textbook ghost ball the real
     * aim is, and which way.
     */
    fun throwAngleFor(state: CueDetatState, target: PointF): Angle? =
        solveAim(state, target)?.throwAngle

    /**
     * Distance between two logical points, as a real length.
     *
     * Replaces the old readout, which was `1200f / screenRadiusInPixels`
     * displayed as feet and inches — a number that changed when the user moved
     * the zoom slider, because it was derived from the projection rather than
     * from the table.
     */
    fun distanceBetween(a: PointF, b: PointF): Length =
        a.toTablePlane().distanceTo(b.toTablePlane())
}

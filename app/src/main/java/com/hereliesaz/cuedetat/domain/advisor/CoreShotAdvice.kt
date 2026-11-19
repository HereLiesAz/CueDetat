package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF
import com.hereliesaz.cuedetat.core.advisor.BallGroup
import com.hereliesaz.cuedetat.core.advisor.ShotKind
import com.hereliesaz.cuedetat.core.advisor.ShotPlan
import com.hereliesaz.cuedetat.core.advisor.ShotPlanner
import com.hereliesaz.cuedetat.core.advisor.TableBall
import com.hereliesaz.cuedetat.core.geometry.Table as CoreTable
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.TargetType
import com.hereliesaz.cuedetat.domain.core.TableFrame
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.hypot

/**
 * The shot advisor as the app uses it: app state in, [RecommendedShot] out, with the choosing done by
 * `:core:advisor`'s [ShotPlanner] (direct pots, banks, kicks and legal two-ball combinations).
 *
 * **Which balls count.** The cue ball is the detected one, else the hand-placed one
 * ([CueDetatState.onPlaneBall]). The rest are the detected balls, which carry the group CV gave them
 * (solids/stripes against [CueDetatState.targetType]; the 8; unclassified ones are never struck, only
 * avoided), and the hand-placed [CueDetatState.obstacleBalls], which are all treated as the shooter's
 * own: there is no UI to say otherwise. The protractor's target ball is the advisor's *output*, not an
 * input, so it is not a ball here.
 *
 * Detections are in logical coordinates only once the table pose is calibrated
 * ([CueDetatState.hasInverseMatrix]); without it they are ignored, and hand-placed balls (always
 * logical) are still used.
 *
 * Pockets come from the core table for the chosen [TableSize], which is the same table the logical
 * plane is built from, so scanned pocket positions add nothing here.
 *
 * Not offered yet, as the core has no post-contact cue path: cue-ball leave and safeties. The
 * recommendation carries no spin ([RecommendedShot.spin] is centre ball) and medium pace.
 */
internal object CoreShotAdvice {

    /** Balls handed to the planner, in the core frame, plus the table they sit on. */
    data class Layout(
        val tableSize: TableSize,
        val cue: Vec2,
        val balls: List<TableBall>,
    )

    /** The layout to plan from, or null when there is no cue ball or nothing to shoot at. */
    fun layout(state: CueDetatState): Layout? {
        val detected = if (state.hasInverseMatrix) state.visionData?.balls.orEmpty() else emptyList()

        val cue = detected.firstOrNull { it.type == BallType.CUE }?.position
            ?: state.onPlaneBall?.center
            ?: return null

        val placed = ArrayList<Pair<PointF, BallGroup>>()
        for (ball in detected) {
            if (ball.type == BallType.CUE) continue
            placed += ball.position to groupOf(ball.type, state.targetType)
        }
        for (ball in state.obstacleBalls) {
            // A ball both detected and hand-placed is one ball, not two stacked on each other.
            val duplicate = placed.any { distance(it.first, ball.center) < LOGICAL_BALL_RADIUS } ||
                distance(cue, ball.center) < LOGICAL_BALL_RADIUS
            if (!duplicate) placed += ball.center to BallGroup.MINE
        }
        if (placed.isEmpty()) return null

        return Layout(
            tableSize = state.table.size,
            cue = TableFrame.toCore(cue),
            balls = placed.mapIndexed { id, (position, group) -> TableBall(id, TableFrame.toCore(position), group) },
        )
    }

    /** Plans from [state] and maps the best shot, or null when there is none worth showing. */
    fun recommend(state: CueDetatState): RecommendedShot? {
        val layout = layout(state) ?: return null
        val table = CoreTable(specOf(layout.tableSize))
        val best = ShotPlanner(table).plan(layout.cue, layout.balls).best ?: return null
        return toRecommendedShot(best)
    }

    /**
     * What [recommend] reads, reduced to what can change its answer: the table and the group, whether
     * detections are usable, and each ball's kind and place. The driver recomputes only when a new
     * signature is not [Signature.sameAs] the last one it acted on.
     *
     * Positions are compared by distance with a tolerance, not rounded onto a grid: a grid flips its
     * answer whenever a ball sits on a cell edge, and a jittery detection there recomputed (and
     * changed the shot) every frame.
     */
    fun signature(state: CueDetatState): Signature {
        val head = "${state.table.size}|${state.targetType}|${state.hasInverseMatrix}"
        val balls = ArrayList<Signature.Ball>()
        state.onPlaneBall?.center?.let { balls += Signature.Ball("cue", it.x, it.y) }
        state.obstacleBalls.forEach { balls += Signature.Ball("placed", it.center.x, it.center.y) }
        if (state.hasInverseMatrix) {
            state.visionData?.balls?.forEach { balls += Signature.Ball("seen:${it.type}", it.position.x, it.position.y) }
        }
        return Signature(head, balls)
    }

    /** The signature while the advisor is off: equal only to itself. */
    val OFF = Signature("off", emptyList())

    class Signature(val head: String, val balls: List<Ball>) {

        class Ball(val tag: String, val x: Float, val y: Float)

        /**
         * Same inputs for the planner's purposes: the same head, and every ball here has one of the
         * same kind within [tolerance] logical units in [other], matched one to one, nearest first.
         */
        fun sameAs(other: Signature, tolerance: Float = MOVE_TOLERANCE): Boolean {
            if (head != other.head || balls.size != other.balls.size) return false
            val free = other.balls.toMutableList()
            for (ball in balls) {
                var best = -1
                var bestDistance = Float.MAX_VALUE
                for ((i, candidate) in free.withIndex()) {
                    if (candidate.tag != ball.tag) continue
                    val d = hypot(candidate.x - ball.x, candidate.y - ball.y)
                    if (d <= tolerance && d < bestDistance) { best = i; bestDistance = d }
                }
                if (best < 0) return false
                free.removeAt(best)
            }
            return true
        }
    }

    /**
     * A ball moving less than this does not recompute the shot. Two logical units is about a tenth of
     * an inch; at a thousand units to the pocket that is about a tenth of a degree of aim, well inside
     * the 0.6 degree aiming spread the probabilities assume.
     */
    const val MOVE_TOLERANCE = 2.0f

    /** A plan as the HUD and the renderer know it, in logical coordinates. */
    fun toRecommendedShot(plan: ShotPlan): RecommendedShot {
        fun logical(p: Vec2) = TableFrame.toLogical(p)
        val approach = plan.cuePath.map(::logical)
        val ballPath = (plan.firstBallPath + plan.secondBallPath).map(::logical)
        // The pocket is where the ball that drops finishes: the last waypoint of its own path.
        val pocket = (plan.secondBallPath.ifEmpty { plan.firstBallPath }).last()
        return RecommendedShot(
            type = when (plan.kind) {
                ShotKind.DIRECT -> ShotType.DIRECT
                ShotKind.BANK -> ShotType.BANK
                ShotKind.KICK -> ShotType.KICK
                ShotKind.COMBO -> ShotType.COMBO
            },
            targetPos = logical(plan.firstBall.position),
            pocketPos = logical(pocket),
            ghostCuePos = logical(plan.ghostBallCenter),
            cutAngleDeg = plan.cutAngle.degrees.toFloat(),
            hardness = Hardness.MEDIUM,
            spin = PointF().apply { x = 0f; y = 0f },
            makeProbability = plan.makeProbability.toFloat(),
            confidence = plan.makeProbability.toFloat(),
            shotPath = approach + ballPath,
            approachOrigin = if (plan.kind == ShotKind.KICK) approach[1] else null,
        )
    }

    fun specOf(size: TableSize): TableSpec = when (size) {
        TableSize.SEVEN_FT -> TableSpec.SEVEN_FOOT
        TableSize.EIGHT_FT -> TableSpec.EIGHT_FOOT
        TableSize.NINE_FT -> TableSpec.NINE_FOOT
    }

    private fun groupOf(type: BallType, target: TargetType): BallGroup = when (type) {
        BallType.SOLID -> if (target == TargetType.SOLIDS) BallGroup.MINE else BallGroup.THEIRS
        BallType.STRIPE -> if (target == TargetType.STRIPES) BallGroup.MINE else BallGroup.THEIRS
        BallType.EIGHT -> BallGroup.EIGHT
        // Never struck, only avoided. (A cue ball is handled before this is reached.)
        BallType.UNKNOWN, BallType.CUE -> BallGroup.UNKNOWN
    }

    private fun distance(a: PointF, b: PointF): Float = hypot(a.x - b.x, a.y - b.y)
}

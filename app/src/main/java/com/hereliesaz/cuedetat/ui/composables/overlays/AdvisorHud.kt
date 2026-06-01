package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.advisor.RecommendedShot
import com.hereliesaz.cuedetat.domain.advisor.ShotType
import kotlin.math.roundToInt

/**
 * Compact readout of the advisor's current recommendation: shot type (when not a plain pot),
 * how hard to hit, the cut angle, and the estimated make-probability.
 */
@Composable
fun AdvisorHud(shot: RecommendedShot, modifier: Modifier = Modifier) {
    val spin = spinLabel(shot.spin)?.let { " · $it" } ?: ""
    val label = if (shot.type == ShotType.SAFETY) {
        // A defensive shot — no pocket / make%; show how good the leave is for us.
        val safe = (shot.positionScore * 100f).roundToInt()
        "Advisor: safety · ${shot.hardness.name.lowercase()}$spin · leaves opp. ${100 - safe}%"
    } else {
        val pct = (shot.makeProbability * 100f).roundToInt()
        val cut = shot.cutAngleDeg.roundToInt()
        val prefix = if (shot.type == ShotType.DIRECT) "" else shot.type.name.lowercase() + " · "
        "Advisor: $prefix${shot.hardness.name.lowercase()} · ${cut}° cut$spin · $pct%"
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 13.sp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** "follow", "draw", "left/right english", or combinations; null for a centre-ball (stun) hit. */
private fun spinLabel(spin: PointF): String? {
    val vertical = when {
        spin.y > 0.2f -> "follow"
        spin.y < -0.2f -> "draw"
        else -> null
    }
    val english = when {
        spin.x > 0.2f -> "right english"
        spin.x < -0.2f -> "left english"
        else -> null
    }
    if (vertical == null && english == null) return null
    return listOfNotNull(vertical, english).joinToString(" + ")
}

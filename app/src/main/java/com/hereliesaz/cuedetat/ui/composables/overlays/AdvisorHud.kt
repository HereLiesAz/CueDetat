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
import com.hereliesaz.cuedetat.domain.advisor.RecommendedShot
import com.hereliesaz.cuedetat.domain.advisor.ShotType
import kotlin.math.roundToInt

/**
 * Compact readout of the advisor's current recommendation: shot type (when not a plain pot),
 * how hard to hit, the cut angle, and the estimated make-probability.
 */
@Composable
fun AdvisorHud(shot: RecommendedShot, modifier: Modifier = Modifier) {
    val pct = (shot.makeProbability * 100f).roundToInt()
    val cut = shot.cutAngleDeg.roundToInt()
    val prefix = if (shot.type == ShotType.DIRECT) "" else shot.type.name.lowercase() + " · "
    Text(
        text = "Advisor: $prefix${shot.hardness.name.lowercase()} · ${cut}° cut · $pct%",
        color = Color.White,
        fontSize = 13.sp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

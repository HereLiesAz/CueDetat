// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/sliders/TableRotationSwiper.kt

package com.hereliesaz.cuedetat.ui.composables.sliders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.TableOrientationLearner
import kotlin.math.floor
import kotlin.math.roundToInt

/** Degrees of table rotation per dp of swipe. A full-width swipe (~350 dp) is about half a turn. */
private const val DEGREES_PER_DP = 0.5f

/** A minor tick every this many degrees; a major tick every [MAJOR_TICK_DEGREES]. */
private const val MINOR_TICK_DEGREES = 5f
private const val MAJOR_TICK_DEGREES = 45f

/**
 * A handle-less swiper for rotating the virtual table by hand. Swiping drags a strip of ticks
 * with the finger and turns the table by the swipe's length; there is no end stop.
 *
 * Shows and moves by the user's own rotation only ([userRotationDegrees]). The compass turns the
 * table as the user walks around it (SystemReducer.followCompass) and is deliberately not shown
 * here: the swiper corrects the table, it doesn't read the heading.
 *
 * Occupies the same space the old rotation slider did (label + 32 dp strip).
 *
 * @param isVisible Visibility flag.
 * @param userRotationDegrees The user's accumulated rotation (CueDetatState.userRotationDegrees).
 * @param onRotate Called with each swipe step's rotation delta in degrees (TableRotationApplied).
 * @param modifier Styling modifier.
 */
@Composable
fun TableRotationSwiper(
    isVisible: Boolean,
    userRotationDegrees: Float,
    onRotate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    val density = LocalDensity.current
    val currentOnRotate by rememberUpdatedState(onRotate)
    val majorColor = MaterialTheme.colorScheme.primary
    val minorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Table Rotation: ${-TableOrientationLearner.normalize360(userRotationDegrees).roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Canvas(
            modifier = Modifier
                .semantics { contentDescription = "Table Rotation" }
                .fillMaxWidth()
                .height(32.dp)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { deltaPx ->
                        // Swipe right turns the table the way the old slider's rightward drag did.
                        val deltaDp = with(density) { deltaPx.toDp().value }
                        currentOnRotate(-deltaDp * DEGREES_PER_DP)
                    }
                )
        ) {
            val pxPerDegree = (1f / DEGREES_PER_DP).dp.toPx()
            // The strip rides with the finger: rotation falls as the finger moves right, so the
            // ticks are shifted by its negation.
            val shiftDeg = -userRotationDegrees
            val centerX = size.width / 2f
            val halfSpanDeg = centerX / pxPerDegree
            var deg = floor((shiftDeg - halfSpanDeg) / MINOR_TICK_DEGREES) * MINOR_TICK_DEGREES
            while (deg <= shiftDeg + halfSpanDeg + MINOR_TICK_DEGREES) {
                val x = centerX + (shiftDeg - deg) * pxPerDegree
                val isMajor = deg % MAJOR_TICK_DEGREES == 0f
                val h = if (isMajor) size.height else size.height * 0.45f
                val top = (size.height - h) / 2f
                drawLine(
                    color = if (isMajor) majorColor else minorColor,
                    start = Offset(x, top),
                    end = Offset(x, top + h),
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                )
                deg += MINOR_TICK_DEGREES
            }
        }
    }
}

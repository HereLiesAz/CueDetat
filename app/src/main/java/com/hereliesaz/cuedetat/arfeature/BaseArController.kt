// FILE: app/src/main/java/com/hereliesaz/cuedetat/arfeature/BaseArController.kt

package com.hereliesaz.cuedetat.arfeature

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.data.ArFrameProcessor
import com.hereliesaz.cuedetat.data.ArTableSession
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import com.hereliesaz.cuedetat.ui.composables.ArCoreBackground
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-base [ArController] implementation. Wraps the existing AR singletons so the
 * rest of the base talks only to the [ArController] interface. Step 2 of the
 * extraction replaces this binding with a reflection-loaded implementation that
 * lives in the on-demand `:feature_expert_ar` module.
 */
@Singleton
class BaseArController @Inject constructor(
    private val arTableSession: ArTableSession,
    private val arFrameProcessor: ArFrameProcessor,
) : ArController {

    override fun probeCapability(): DepthCapability =
        if (arTableSession.isArCoreAvailable()) {
            // Probe depth support by creating (and immediately closing) a test session.
            val testSession = arTableSession.createSession()
            val cap = arTableSession.capability
            if (testSession != null) arTableSession.close()
            cap
        } else {
            DepthCapability.NONE
        }

    override fun updateUiState(state: CueDetatState) {
        arFrameProcessor.updateUiState(state)
    }

    override fun setTableZOffsetLogical(tableZOffsetLogical: Float) {
        // tableZOffset is in logical units; convert to metres for the AR plane lift.
        arTableSession.setTableHeightMeters(
            tableZOffsetLogical / TableFrameHomography.LOGICAL_UNITS_PER_METER
        )
    }

    @Composable
    override fun ArBackground(modifier: Modifier, onEvent: (MainScreenEvent) -> Unit) {
        ArCoreBackground(
            modifier = modifier,
            arTableSession = arTableSession,
            arFrameProcessor = arFrameProcessor,
            onEvent = onEvent,
        )
    }
}

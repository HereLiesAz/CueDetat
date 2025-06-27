package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class WarningManager @Inject constructor() {

    private var warningIndex = 0
    private var warningDismissJob: Job? = null

    private val _currentWarning = MutableStateFlow<String?>(null)
    val currentWarning = _currentWarning.asStateFlow()

    fun triggerWarning(warnings: Array<String>, scope: CoroutineScope) {
        warningDismissJob?.cancel()
        _currentWarning.value = warnings[warningIndex]
        warningIndex = (warningIndex + 1) % warnings.size
        warningDismissJob = scope.launch {
            delay(3000L)
            dismissWarning()
        }
    }

    private fun dismissWarning() {
        _currentWarning.value = null
    }

    /**
     * Re-added: Function to check for warnings based on OverlayState.
     * This is a skeletal implementation; you should populate the actual warning
     * logic based on your application's specific conditions.
     */
    fun checkWarnings(state: OverlayState): String? {
        if (state.isImpossibleShot) {
            return "Impossible Shot Detected!"
        }
        // Example: Add a warning if ARCore session exists but its tracking is not reliable
        // (Note: `isDepthSensorUsed` is not a direct property of CameraConfig accessible this way).
        // You would need to check `frame.camera.trackingState` from the ARCore rendering loop
        // and propagate that state to OverlayState if you want to warn based on tracking quality.
        if (state.isSpatiallyLocked && state.arSession != null /* && state.arTrackingState != TrackingState.TRACKING */) {
            // This condition is illustrative and needs actual AR tracking state check which isn't in OverlayState.
            // For a real check, you'd need AR tracking state propagated to OverlayState.
            // return "Poor AR Tracking Quality!"
        }
        // Add more warning conditions here (e.g., if a ball is out of bounds, etc.)
        return null // No warning
    }
}
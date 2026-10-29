package com.hereliesaz.cuedetat.domain

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

    /**
     * Shows the next of [warnings] for three seconds. [reason], when given, leads the message so
     * the user learns which condition they hit (an impossible shot, something in the way, too
     * steep a tilt); the insults alone were the same whatever was wrong.
     */
    fun triggerWarning(warnings: Array<String>, scope: CoroutineScope, reason: String? = null) {
        warningDismissJob?.cancel()
        _currentWarning.value = warnings[warningIndex].let { if (reason != null) "$reason $it" else it }
        warningIndex = (warningIndex + 1) % warnings.size
        warningDismissJob = scope.launch {
            delay(3000L)
            dismissWarning()
        }
    }

    fun dismissWarning() {
        warningDismissJob?.cancel()
        _currentWarning.value = null
    }
}
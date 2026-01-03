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
}
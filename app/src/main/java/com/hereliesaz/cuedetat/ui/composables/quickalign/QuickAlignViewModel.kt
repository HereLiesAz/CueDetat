// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class QuickAlignViewModel @Inject constructor() : ViewModel() {

    private val _tappedPoints = MutableStateFlow<List<Offset>>(emptyList())
    val tappedPoints = _tappedPoints.asStateFlow()

    private val _currentTapIndex = MutableStateFlow(0)
    val currentTapIndex = _currentTapIndex.asStateFlow()

    val logicalPointsToTap: List<String> = listOf(
        "Top-Left Corner Pocket",
        "Top-Right Corner Pocket",
        "Bottom-Right Corner Pocket",
        "Bottom-Left Corner Pocket"
    )

    fun onScreenTapped(offset: Offset) {
        if (_currentTapIndex.value < 4) {
            _tappedPoints.value = _tappedPoints.value + offset
            _currentTapIndex.value++
        }
    }

    fun onResetPoints() {
        _tappedPoints.value = emptyList()
        _currentTapIndex.value = 0
    }
}
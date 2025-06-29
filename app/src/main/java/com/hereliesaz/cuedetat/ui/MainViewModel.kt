package com.hereliesaz.cuedetat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.ui.state.AppAnchor
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: UiEvent) {
        viewModelScope.launch {
            when (event) {
                is UiEvent.OnPlaneTap -> {
                    if (!_uiState.value.tablePlaced) {
                        val newAnchors = _uiState.value.anchors + AppAnchor(event.anchor, isTable = true)
                        _uiState.update {
                            it.copy(
                                anchors = newAnchors,
                                tablePlaced = true,
                                statusText = "Table placed. Tap balls to select."
                            )
                        }
                    }
                }
                is UiEvent.OnShotTypeSelect -> _uiState.update { it.copy(shotType = event.shotType) }
                is UiEvent.OnSpinChange -> _uiState.update { it.copy(spinOffset = event.offset) }
                is UiEvent.OnElevationChange -> _uiState.update { it.copy(cueElevation = event.elevation) }
                is UiEvent.OnBallSelect -> _uiState.update { it.copy(selectedBallId = event.ballId) }
                UiEvent.OnUndo -> { /* Implement undo logic */ }
                UiEvent.OnHelpToggle -> { /* Implement help toggle logic */ }
                UiEvent.OnDarkModeToggle -> { /* Implement dark mode toggle */ }
            }
        }
    }
}
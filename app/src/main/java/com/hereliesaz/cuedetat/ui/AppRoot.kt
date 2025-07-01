package com.hereliesaz.cuedetat.ui

import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.ui.state.UiState
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme

@Composable
fun AppRoot(
    viewModel: MainViewModel,
    uiState: UiState
) {
    CueDetatTheme(darkTheme = uiState.isDarkMode) {
        MainScreen(viewModel = viewModel, uiState = uiState)
    }
}

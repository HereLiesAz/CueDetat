// app/src/main/java/com/hereliesaz/cuedetat/ui/AppRoot.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme

@Composable
fun AppRoot() {
    CueDetatTheme {
        val viewModel: MainViewModel = hiltViewModel()
        MainScreen(viewModel = viewModel)
    }
}
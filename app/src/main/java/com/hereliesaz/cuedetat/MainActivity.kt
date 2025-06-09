package com.hereliesaz.cuedetat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.theme.CueD8atTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            viewModel.uiState.collect {
                // You can react to state changes here if needed outside of Compose
            }
        }

        setContent {
            val dynamicColorScheme by viewModel.dynamicColorScheme.collectAsState()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            CueD8atTheme(
                darkTheme = dynamicColorScheme?.let { ColorUtils.calculateLuminance(it.primary.toArgb()) < 0.5 }
                    ?: isSystemInDarkTheme,
                dynamicColorScheme = dynamicColorScheme
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
package com.hereliesaz.cuedetat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            val context = LocalContext.current
            val uiState by viewModel.uiState.collectAsState()

            val useDarkTheme = when (uiState.isForceLightMode) {
                true -> false
                false -> true
                null -> isSystemInDarkTheme()
            }

            LaunchedEffect(viewModel) {
                viewModel.singleEvent.collect { event ->
                    when (event) {
                        is SingleEvent.ShowToast -> {
                            // Toast logic here
                        }
                        is SingleEvent.OpenUrl -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                            context.startActivity(intent)
                        }
                        null -> {}
                    }
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
            }

            CueDetatTheme(darkTheme = useDarkTheme) {
                LaunchedEffect(useDarkTheme) {
                    viewModel.onEvent(MainScreenEvent.ThemeChanged(if (useDarkTheme) darkColorScheme() else lightColorScheme()))
                }
                MainScreen(viewModel)
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}
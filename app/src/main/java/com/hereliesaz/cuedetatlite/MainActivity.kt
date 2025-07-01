// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/MainActivity.kt
package com.hereliesaz.cuedetatlite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hereliesaz.cuedetatlite.ui.MainScreen
import com.hereliesaz.cuedetatlite.ui.MainViewModel
import com.hereliesaz.cuedetatlite.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            CueDetatTheme {
                MainScreen(viewModel = viewModel)

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        event?.getContentIfNotHandled()?.let { uiEvent ->
                            when (uiEvent) {
                                is MainViewModel.UiEvent.OpenUrl -> {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiEvent.url))
                                    startActivity(intent)
                                }
                                is MainViewModel.UiEvent.ShowUpdateAvailable -> {
                                    // This is handled by the UI state, no action needed here.
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

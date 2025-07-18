// FILE: app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt

package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.composables.SplashScreen
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setContent { AppContent(viewModel) }
            } else {
                // Heresy is not tolerated. The user will comply or they will not use the app.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        when {
            hasCameraPermission() -> {
                setContent { AppContent(viewModel) }
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        observeSingleEvents()
    }

    private fun observeSingleEvents() {
        viewModel.singleEvent.onEach { event ->
            when (event) {
                is SingleEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    startActivity(intent)
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.SendFeedbackEmail -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // Only email apps should handle this
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(event.email))
                        putExtra(Intent.EXTRA_SUBJECT, event.subject)
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                null -> { /* Do nothing */ }
            }
        }.launchIn(lifecycleScope)
    }

    @Composable
    private fun AppContent(viewModel: MainViewModel) {
        var showSplashScreen by remember { mutableStateOf(true) }

        CueDetatTheme {
            if (showSplashScreen) {
                SplashScreen(onTimeout = { showSplashScreen = false })
            } else {
                val currentAppControlColorScheme = MaterialTheme.colorScheme
                LaunchedEffect(currentAppControlColorScheme) {
                    viewModel.onEvent(MainScreenEvent.ThemeChanged(currentAppControlColorScheme))
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
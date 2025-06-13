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
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.SingleEvent
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                recreate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (hasCameraPermission()) {
            setContent {
                AppContent()
            }
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        observeSingleEvents()
    }

    private fun observeSingleEvents() {
        viewModel.singleEvent.onEach { event ->
            when (event) {
                is SingleEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    startActivity(intent)
                    viewModel.onSingleEventConsumed()
                }

                null -> {
                    // Do nothing
                }
            }
        }.launchIn(lifecycleScope)
    }

    @Composable
    private fun AppContent() {
        CueDetatTheme {
            MainScreen(viewModel = viewModel)
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}

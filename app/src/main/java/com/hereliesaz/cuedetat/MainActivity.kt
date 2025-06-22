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
import androidx.compose.runtime.LaunchedEffect // For initial theme dispatch
import androidx.compose.material3.MaterialTheme // For getting current theme
// import androidx.compose.runtime.collectAsState // No longer needed here for theme
// import androidx.compose.runtime.getValue // No longer needed here for theme
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
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
                if (findViewById<android.view.ViewGroup>(android.R.id.content).childCount == 0) {
                    setContent { AppContent(viewModel) }
                }
            } else {
                // TODO: Handle permission denial gracefully (e.g., show a message)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (hasCameraPermission()) {
            setContent {
                AppContent(viewModel)
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
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.ShowDonationDialog -> {
                    showDonationDialog()
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                null -> { /* Do nothing */
                }
            }
        }.launchIn(lifecycleScope)
    }

    private fun showDonationDialog() {
        val items = arrayOf("PayPal", "Venmo", "CashApp")
        val urls = arrayOf(
            "https://paypal.me/azcamehere",
            "https://venmo.com/u/hereliesaz",
            "https://cash.app/\$azcamehere"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("Chalk Your Tip")
            .setItems(items) { _, which ->
                if (which < urls.size) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[which])))
                }
            }
            .setNegativeButton("Maybe Later", null)
            .show()
    }

    @Composable
    private fun AppContent(viewModel: MainViewModel) {
        CueDetatTheme { // CueDetatTheme now manages its dark/light mode internally based on system
            // Dispatch the initial app control theme to the ViewModel
            val currentAppControlColorScheme = MaterialTheme.colorScheme
            LaunchedEffect(currentAppControlColorScheme) {
                viewModel.onEvent(MainScreenEvent.ThemeChanged(currentAppControlColorScheme))
            }
            MainScreen(viewModel = viewModel)
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
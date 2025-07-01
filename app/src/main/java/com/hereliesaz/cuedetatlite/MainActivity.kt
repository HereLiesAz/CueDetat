package com.hereliesaz.cuedetatlite

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.hereliesaz.cuedetatlite.ui.MainScreen
import com.hereliesaz.cuedetatlite.ui.MainViewModel
import com.hereliesaz.cuedetatlite.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.onPermissionGranted()
            } else {
                // The user has denied the permission.
                // The app will continue without camera functionality.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForCameraPermission()
        setContent {
            CueDetatTheme {
                MainScreen(viewModel)
            }
        }
    }

    private fun checkForCameraPermission() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                viewModel.onPermissionGranted()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
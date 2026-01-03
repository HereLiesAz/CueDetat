package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.utils.SecurityUtils
import com.hereliesaz.cuedetat.view.state.SingleEvent
import androidx.core.view.WindowCompat
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.ui.ProtractorScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()
    val calibrationViewModel: CalibrationViewModel by viewModels()
    val quickAlignViewModel: QuickAlignViewModel by viewModels()

    private var isFlashlightOn = false
    private var backCameraId: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                this.recreate()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initializeCameraId()

        if (hasCameraPermission()) {
            setContent()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFlashlightOn) {
            toggleFlashlight(forceOff = true)
        }
    }

    private fun setContent() {
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val singleEvent by viewModel.singleEvent.collectAsState(initial = null)
            val calibrationAnalyzer = remember(calibrationViewModel) { CalibrationAnalyzer(calibrationViewModel) }

            LaunchedEffect(singleEvent) {
                singleEvent?.let { event ->
                    when (event) {
                        is SingleEvent.OpenUrl -> {
                            if (SecurityUtils.isSafeUrl(event.url)) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                                startActivity(intent)
                            } else {
                                Log.e("MainActivity", "Blocked unsafe URL: ${event.url}")
                            }
                        }
                        is SingleEvent.SendFeedbackEmail -> {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(event.email))
                                putExtra(Intent.EXTRA_SUBJECT, event.subject)
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is SingleEvent.InitiateHaterMode -> {
                            Toast.makeText(this@MainActivity, "Hater Mode Initiated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
            }

            val darkTheme = isSystemInDarkTheme() || uiState.isForceLightMode == false

            CueDetatTheme(darkTheme = darkTheme) {
                ProtractorScreen(
                    mainViewModel = viewModel,
                    calibrationViewModel = calibrationViewModel,
                    quickAlignViewModel = quickAlignViewModel,
                    calibrationAnalyzer = calibrationAnalyzer
                )
            }
        }
    }

    private fun initializeCameraId() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                if (facing == CameraCharacteristics.LENS_FACING_BACK && hasFlash == true) {
                    backCameraId = id
                    return
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("MainActivity", "Failed to access camera", e)
        }
    }

    fun toggleFlashlight(forceOff: Boolean = false) {
        if (backCameraId == null) {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
            return
        }
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val newState = if (forceOff) false else !isFlashlightOn
            cameraManager.setTorchMode(backCameraId!!, newState)
            isFlashlightOn = newState
        } catch (e: Exception) {
            isFlashlightOn = false
            Toast.makeText(this, "Flashlight unavailable while AR is active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

// app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt
package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.ProtractorScreen
import com.hereliesaz.cuedetat.ui.composables.SplashScreen
import com.hereliesaz.cuedetat.ui.hatemode.HaterEvent
import com.hereliesaz.cuedetat.ui.hatemode.HaterScreen
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel
import com.hereliesaz.cuedetat.ui.composables.dialogs.PermissionExplanationScreen
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.utils.SecurityUtils
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val haterViewModel: HaterViewModel by viewModels()

    // Backed by rememberSaveable-created MutableStates (assigned once composition starts,
    // see AppContent/setContent below) instead of plain mutableStateOf Activity fields, so
    // an in-progress support-sheet / permission-gate survives Activity recreation on a
    // configuration change (e.g. system dark-mode toggle) instead of silently resetting and
    // dismissing an open sheet with no callback. Written to from non-composable
    // callbacks (requestPermissionsLauncher, observeSingleEvents' flow collector) via
    // `.value`, which is safe: Compose's snapshot system observes such writes regardless of
    // whether they originate inside composition.
    private lateinit var cameraPermissionGrantedState: MutableState<Boolean>
    private lateinit var showSupportSheetState: MutableState<Boolean>

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val bluetoothGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
            } else true

            if (cameraGranted) {
                cameraPermissionGrantedState.value = true
                if (bluetoothGranted) {
                    (application as? MyApplication)?.initializeWearables()
                }
            } else {
                // Heresy is not tolerated. The user will comply or they will not use the app.
                Toast.makeText(
                    this,
                    R.string.permission_denied,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val cameraGranted = rememberSaveable { mutableStateOf(hasCameraPermission()) }
            val showSupport = rememberSaveable { mutableStateOf(false) }
            // Publish the saveable MutableState instances to the Activity fields once per
            // composition commit so the non-composable callbacks above/below can write to
            // them. SideEffect (rather than a bare assignment) avoids re-publishing during a
            // composition that gets discarded before being committed.
            SideEffect {
                cameraPermissionGrantedState = cameraGranted
                showSupportSheetState = showSupport
            }

            if (cameraGranted.value) {
                AppContent()
            } else {
                CueDetatTheme {
                    PermissionExplanationScreen(onConfirm = {
                        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        }
                        requestPermissionsLauncher.launch(permissions)
                    })
                }
            }
        }
        observeSingleEvents()
    }

    override fun onResume() {
        super.onResume()
        // Camera permission is only checked once up front in onCreate; if the user revokes
        // it from system Settings while the app is backgrounded, CameraX fails silently
        // inside a swallowed catch(Exception) in CameraBackground, leaving a frozen blank
        // camera feed with no recovery UI. Re-check on every resume and, if it was revoked,
        // flip back to the permission-gate screen (same mechanism as the initial request).
        if (::cameraPermissionGrantedState.isInitialized) {
            val currentlyGranted = hasCameraPermission()
            if (cameraPermissionGrantedState.value != currentlyGranted) {
                cameraPermissionGrantedState.value = currentlyGranted
            }
        }
    }

    /**
     * Opens an external link through the same allowlist every other outbound
     * link uses. Donation links go through here too — nothing gets a raw intent
     * just because it is ours.
     */
    private fun openUrlSafely(url: String) {
        if (SecurityUtils.isSafeUrl(url)) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } else {
            Toast.makeText(this, "Link blocked for security.", Toast.LENGTH_SHORT).show()
            android.util.Log.w("MainActivity", "Suspicious URL blocked: $url")
        }
    }

    private fun observeSingleEvents() {
        viewModel.singleEvent.onEach { event ->
            when (event) {
                is SingleEvent.OpenUrl -> {
                    openUrlSafely(event.url)
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.SendFeedbackEmail -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(event.email))
                        putExtra(Intent.EXTRA_SUBJECT, event.subject)
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.InitiateHaterMode -> {
                    haterViewModel.onEvent(HaterEvent.EnterHaterMode)
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.HaterShake -> {
                    haterViewModel.onEvent(HaterEvent.ShakeDetected)
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.ShowSupportSheet -> {
                    showSupportSheetState.value = true
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                null -> { /* Do nothing */ }
            }
        }.launchIn(lifecycleScope)
    }

    @Composable
    private fun AppContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
        val showSplashScreen = uiState.experienceMode == null
        var haterModeLockedOrientation by rememberSaveable { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

        LaunchedEffect(uiState.experienceMode, uiState.orientationLock) {
            if (uiState.experienceMode == ExperienceMode.HATER) {
                if (haterModeLockedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    haterModeLockedOrientation = when (resources.configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                requestedOrientation = haterModeLockedOrientation
            } else {
                haterModeLockedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                requestedOrientation = when (uiState.orientationLock) {
                    CueDetatState.OrientationLock.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    CueDetatState.OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    CueDetatState.OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }

        CueDetatTheme(luminanceAdjustment = uiState.luminanceAdjustment) {
            updateInfo?.let { info ->
                com.hereliesaz.cuedetat.ui.composables.dialogs.AppUpdateDialog(
                    versionName = info.versionName,
                    onInstall = { viewModel.installUpdate(this@MainActivity) },
                    onDismiss = { viewModel.dismissUpdate() }
                )
            }

            if (showSupportSheetState.value) {
                com.hereliesaz.cuedetat.ui.composables.support.SupportSheet(
                    onDismiss = { showSupportSheetState.value = false },
                    onOpenUrl = { url ->
                        showSupportSheetState.value = false
                        openUrlSafely(url)
                    },
                )
            }

            if (showSplashScreen) {
                SplashScreen(onRoleSelected = { selectedMode ->
                    viewModel.onEvent(MainScreenEvent.SetExperienceMode(selectedMode))
                })
            } else {
                val currentAppControlColorScheme = MaterialTheme.colorScheme
                LaunchedEffect(currentAppControlColorScheme) {
                    viewModel.onEvent(MainScreenEvent.ThemeChanged(currentAppControlColorScheme))
                }

                when (uiState.experienceMode) {
                    ExperienceMode.HATER -> HaterScreen(
                        haterViewModel = haterViewModel,
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                    else -> {
                        ProtractorScreen(
                            mainViewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
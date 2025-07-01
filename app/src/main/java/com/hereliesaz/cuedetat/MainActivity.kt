package com.hereliesaz.cuedetat

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreatePermissionsNotGranted
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import com.hereliesaz.cuedetat.ui.AppRoot
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var session by remember { mutableStateOf<Session?>(null) }
            var error by remember { mutableStateOf<String?>(null) }

            // DisposableEffect correctly ties the session's lifecycle to the composable's lifecycle.
            DisposableEffect(lifecycle) {
                val observer = object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> session?.resume()
                            Lifecycle.Event.ON_PAUSE -> session?.pause()
                            else -> {}
                        }
                    }
                }
                lifecycle.addObserver(observer)

                // The onDispose block is crucial for cleanup, ensuring the session is destroyed.
                onDispose {
                    lifecycle.removeObserver(observer)
                    session?.destroy()
                }
            }

            // LaunchedEffect handles the one-time, asynchronous creation of the session.
            LaunchedEffect(Unit) {
                if (session == null) {
                    // The 'when' statement must be exhaustive. Adding an 'else' branch
                    // satisfies the compiler.
                    when (val result = Session.create(this@MainActivity)) {
                        is SessionCreateSuccess -> {
                            session = result.session
                            viewModel.onEvent(UiEvent.SetSession(result.session))
                        }
                        is Error -> {
                            error = result.message
                            Log.e("MainActivity", "Session creation failed: ${result.message}")
                        }
                        is SessionCreatePermissionsNotGranted -> {
                            error = "Permissions not granted for session."
                            Log.e("MainActivity", "Permissions not granted")
                        }
                        else -> {
                            error = "An unknown error occurred during session creation."
                            Log.e("MainActivity", "Unknown session creation result: $result")
                        }
                    }
                }
            }

            // The root of the UI is now AppRoot. Spatial rendering will be handled
            // by components within AppRoot.
            AppRoot(viewModel = viewModel, uiState = uiState)
        }
    }
}

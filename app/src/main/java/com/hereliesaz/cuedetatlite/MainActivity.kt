package com.hereliesaz.cuedetatlite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.cuedetatlite.ui.MainScreen
import com.hereliesaz.cuedetatlite.ui.MainViewModel
import com.hereliesaz.cuedetatlite.ui.UiEvent
import com.hereliesaz.cuedetatlite.ui.theme.CueDetatLiteTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as MyApplication
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(
                    app.stateReducer,
                    app.updateStateUseCase,
                    app.sensorRepository
                    // REMOVED: app.updateChecker
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(viewModel.sensorRepository)

        setContent {
            CueDetatLiteTheme {
                LaunchedEffect(Unit) {
                    viewModel.uiEvents.collectLatest { event ->
                        when (event) {
                            is UiEvent.OpenUrl -> {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                                startActivity(intent)
                            }
                            is UiEvent.ShowToast -> {
                                // Handle toast messages
                            }
                        }
                    }
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
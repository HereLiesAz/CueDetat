package com.hereliesaz.cuedetatlite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.hereliesaz.cuedetatlite.ui.MainScreen
import com.hereliesaz.cuedetatlite.ui.MainViewModel
import com.hereliesaz.cuedetatlite.ui.UiEvent
import com.hereliesaz.cuedetatlite.ui.theme.CueDetatLiteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
package com.hereliesaz.cuedetat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CueDetatTheme {
                MainScreen()
            }
        }
    }
}
package com.hereliesaz.cuedetat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.ar.core.ArCoreApk
import com.hereliesaz.cuedetat.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import org.opencv.android.OpenCVLoader

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.i("OpenCV", "Initialized successfully")
        }

        ArCoreApk.getInstance().requestInstall(this, true)

        setContent {
            AppRoot()
        }
    }
}

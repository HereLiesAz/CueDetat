package com.hereliesaz.cuedetat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.ar.core.ArCoreApk
import com.hereliesaz.cuedetat.ar.processing.SensorFusionManager
import com.hereliesaz.cuedetat.ar.processing.ShotPredictor
import com.hereliesaz.cuedetat.ar.processing.SpinTracker
import com.hereliesaz.cuedetat.ar.vision.BilliardsComputerVisionEngine
import com.hereliesaz.cuedetat.ui.AppRoot
import com.hereliesaz.cuedetat.view.model.SpinTracker
import dagger.hilt.android.AndroidEntryPoint
import org.opencv.android.OpenCVLoader

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayViewModel: OverlayViewModel by viewModels()
    private lateinit var visionEngine: BilliardsComputerVisionEngine
    private lateinit var spinTracker: SpinTracker
    private lateinit var shotPredictor: ShotPredictor
    private lateinit var sensorFusionManager: SensorFusionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.i("OpenCV", "Initialized successfully")
        }

        ArCoreApk.getInstance().requestInstall(this, true)

        visionEngine = BilliardsComputerVisionEngine()
        spinTracker = SpinTracker()
        shotPredictor = ShotPredictor()
        sensorFusionManager = SensorFusionManager(this).apply {
            onOrientationUpdate = { orientation ->
                // Optional: pass to overlay view model
            }
            start()
        }

        setContent {
            AppRoot(
                overlayViewModel = overlayViewModel,
                visionEngine = visionEngine,
                spinTracker = spinTracker,
                shotPredictor = shotPredictor,
                sensorFusionManager = sensorFusionManager
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorFusionManager.stop()
    }
}

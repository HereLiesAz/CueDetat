// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/data/SensorRepository.kt
package com.hereliesaz.cuedetatlite.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FullOrientation(val azimuth: Float, val pitch: Float, val roll: Float)

class SensorRepository(context: Context) : SensorEventListener, LifecycleObserver {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(FullOrientation(0f, 0f, 0f))
    val orientation: StateFlow<FullOrientation> = _orientation.asStateFlow()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            _orientation.value = FullOrientation(
                azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat(),
                pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
                roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    fun getOrientationFlow(): StateFlow<FullOrientation> {
        return orientation
    }
}

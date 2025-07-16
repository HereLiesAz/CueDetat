// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/DeviceSensorManager.kt
package com.hereliesaz.cuedetat.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class FullOrientation(
    val azimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f
)

@Singleton
class DeviceSensorManager @Inject constructor(@ApplicationContext context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val orientationData: Flow<FullOrientation> = callbackFlow {
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val orientation = FullOrientation(
                        azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat(),
                        pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
                        roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                    )
                    trySend(orientation)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    fun stopSensorUpdates() {
        // The flow's awaitClose will handle unregistering
    }
}
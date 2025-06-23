// app/src/main/java/com/hereliesaz/cuedetat/data/SensorRepository.kt
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

/**
 * Holds the full orientation data: yaw, pitch, roll in degrees.
 * Yaw (Azimuth): Rotation around Z-axis.
 * Pitch: Rotation around X-axis.
 * Roll: Rotation around Y-axis.
 */
data class FullOrientation(val yaw: Float, val pitch: Float, val roll: Float)

@Singleton
class SensorRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Keep the old pitchAngleFlow for now if anything still relies on it directly,
    // but prefer using fullOrientationFlow.
    val pitchAngleFlow: Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3) // For yaw, pitch, roll

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    // orientationAngles[1] is pitch. Convert radians to degrees.
                    val pitchInDegrees = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    trySend(-pitchInDegrees) // Send negative pitch as per existing convention
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Not used */ }
        }
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    val fullOrientationFlow: Flow<FullOrientation> = callbackFlow {
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3) // For yaw, pitch, roll

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    // orientationAngles[0] is yaw (azimuth)
                    // orientationAngles[1] is pitch
                    // orientationAngles[2] is roll
                    // Convert radians to degrees
                    val yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    // Send pitch as negative to match existing convention for `pitchAngle` in state.
                    trySend(FullOrientation(yaw = yaw, pitch = -pitch, roll = roll))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Not used */ }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(
                listener,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
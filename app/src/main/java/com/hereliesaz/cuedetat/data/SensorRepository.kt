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

    // EMA filter parameters
    private val alpha = 0.2f // Smoothing factor; lower means more smoothing (less responsive)
    private var smoothedYaw: Float? = null
    private var smoothedPitch: Float? = null
    private var smoothedRoll: Float? = null

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

                    val rawYaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    // Apply EMA filter
                    smoothedYaw = smoothedYaw?.let { (rawYaw * alpha) + (it * (1 - alpha)) } ?: rawYaw
                    smoothedPitch = smoothedPitch?.let { (rawPitch * alpha) + (it * (1 - alpha)) } ?: rawPitch
                    smoothedRoll = smoothedRoll?.let { (rawRoll * alpha) + (it * (1 - alpha)) } ?: rawRoll

                    // Send pitch as negative to match existing convention for `pitchAngle` in state.
                    trySend(FullOrientation(yaw = smoothedYaw!!, pitch = -smoothedPitch!!, roll = smoothedRoll!!))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Not used */ }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(
                listener,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME // SENSOR_DELAY_UI or SENSOR_DELAY_NORMAL might be better for smoother UI if GAME is too fast
            )
        }
        awaitClose {
            sensorManager.unregisterListener(listener)
            // Reset smoothed values when listener is unregistered
            smoothedYaw = null
            smoothedPitch = null
            smoothedRoll = null
        }
    }
}
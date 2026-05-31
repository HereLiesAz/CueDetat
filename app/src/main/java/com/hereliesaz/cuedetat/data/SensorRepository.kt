// app/src/main/java/com/hereliesaz/cuedetat/data/SensorRepository.kt
package com.hereliesaz.cuedetat.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.abs
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

    private var smoothedYaw: Float? = null
    private var smoothedPitch: Float? = null
    private var smoothedRoll: Float? = null

    val fullOrientationFlow: Flow<FullOrientation> = callbackFlow {
        // Battery: register at the slow UI rate (~15Hz) by default and bump to the fast
        // GAME rate (~50Hz) only while the phone is actually being moved, dropping back
        // once it has been still for STILL_TIMEOUT_MS. A still phone is the common case
        // (lining up a shot), so this cuts sensor callbacks ~3x without costing
        // responsiveness during a deliberate tilt.
        var currentDelay = SensorManager.SENSOR_DELAY_UI
        var lastMovementTime = 0L
        lateinit var listener: SensorEventListener

        listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3) // For yaw, pitch, roll

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    val rawYaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    // Adaptive EMA: heavy smoothing when phone is still (stabilization),
                    // lighter smoothing when deliberately tilted (responsiveness).
                    val deltaP = abs(rawPitch - (smoothedPitch ?: rawPitch))
                    val deltaR = abs(rawRoll - (smoothedRoll ?: rawRoll))
                    val movement = maxOf(deltaP, deltaR)
                    val alpha = when {
                        movement > 4f -> 0.20f  // deliberate tilt
                        movement > 1.5f -> 0.07f // moderate movement
                        else -> 0.025f                         // near-still: maximum stabilization
                    }
                    smoothedYaw = smoothedYaw?.let { (rawYaw * alpha) + (it * (1 - alpha)) } ?: rawYaw
                    smoothedPitch = smoothedPitch?.let { (rawPitch * alpha) + (it * (1 - alpha)) } ?: rawPitch
                    smoothedRoll = smoothedRoll?.let { (rawRoll * alpha) + (it * (1 - alpha)) } ?: rawRoll

                    // Adaptive sampling rate: speed up while moving, slow down when still.
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (movement > 1.5f) lastMovementTime = now
                    val desiredDelay =
                        if (now - lastMovementTime < STILL_TIMEOUT_MS) SensorManager.SENSOR_DELAY_GAME
                        else SensorManager.SENSOR_DELAY_UI
                    if (desiredDelay != currentDelay && rotationVectorSensor != null) {
                        currentDelay = desiredDelay
                        sensorManager.unregisterListener(listener)
                        sensorManager.registerListener(listener, rotationVectorSensor, desiredDelay)
                    }

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
                currentDelay
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

    private companion object {
        const val STILL_TIMEOUT_MS = 1000L // drop to the slow rate after this long without movement
    }
}
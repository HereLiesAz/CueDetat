// app/src/main/java/com/hereliesaz/cuedetat/data/SensorRepository.kt
package com.hereliesaz.cuedetat.data

import androidx.annotation.Keep
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.hereliesaz.cuedetat.domain.OrientationFilter
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
@Keep
data class FullOrientation(val yaw: Float, val pitch: Float, val roll: Float)

@Singleton
class SensorRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val fullOrientationFlow: Flow<FullOrientation> = callbackFlow {
        // Smoothing state is local to this collector (closure-captured), not a shared
        // instance field. MainViewModel and HaterViewModel each collect this flow
        // concurrently via independent callbackFlow invocations; sharing smoothing state
        // across them meant one collector's cancellation (awaitClose) reset the other's
        // in-progress smoothing, causing a visible orientation jump.
        val filter = OrientationFilter()

        // Battery: register at the slow UI rate (~15Hz) by default and bump to the fast
        // GAME rate (~50Hz) only while the phone is actually being moved, dropping back
        // once it has been still for STILL_TIMEOUT_MS. The filter is a function of time, so the
        // change of rate does not change how the orientation moves; it only changes how often.
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

                    // One Euro filter per axis (see OrientationFilter): heavy while nearly still,
                    // light while turning, and yaw counts like the other two.
                    val smoothed = filter.update(rawYaw, rawPitch, rawRoll, event.timestamp)

                    // Adaptive sampling rate: speed up while moving, slow down when still. "Moving"
                    // is the filter's own speed estimate, which noise does not reach.
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (filter.speedDegPerSecond > MOVING_DEG_PER_SECOND) lastMovementTime = now
                    val desiredDelay =
                        if (now - lastMovementTime < STILL_TIMEOUT_MS) SensorManager.SENSOR_DELAY_GAME
                        else SensorManager.SENSOR_DELAY_UI
                    if (desiredDelay != currentDelay && rotationVectorSensor != null) {
                        currentDelay = desiredDelay
                        sensorManager.unregisterListener(listener)
                        sensorManager.registerListener(listener, rotationVectorSensor, desiredDelay)
                    }

                    // Send pitch as negative to match existing convention for `pitchAngle` in state.
                    trySend(FullOrientation(yaw = smoothed.yaw, pitch = -smoothed.pitch, roll = smoothed.roll))
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
        }
    }

    private companion object {
        const val STILL_TIMEOUT_MS = 1000L // drop to the slow rate after this long without movement

        /** Faster than this, in degrees a second on any axis, the phone is being moved (use the fast rate). */
        const val MOVING_DEG_PER_SECOND = 3f
    }
}

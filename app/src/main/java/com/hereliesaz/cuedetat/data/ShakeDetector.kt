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

@Singleton
class ShakeDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastUpdate: Long = 0
    private var last_x: Float = 0.0f
    private var last_y: Float = 0.0f
    private var last_z: Float = 0.0f

    companion object {
        private const val SHAKE_THRESHOLD = 2200
    }

    val shakeFlow: Flow<Unit> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val curTime = System.currentTimeMillis()
                    if ((curTime - lastUpdate) > 100) {
                        val diffTime = (curTime - lastUpdate)
                        lastUpdate = curTime

                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        val speed =
                            Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000

                        if (speed > SHAKE_THRESHOLD) {
                            trySend(Unit)
                        }

                        last_x = x
                        last_y = y
                        last_z = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
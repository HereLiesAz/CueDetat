package com.hereliesaz.cuedetat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * SensorFusionManager provides fused orientation from accelerometer and gyroscope.
 */
class SensorFusionManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    var onOrientationUpdate: ((FloatArray) -> Unit)? = null

    fun start() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rotation = FloatArray(9)
        val inclination = FloatArray(9)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).firstOrNull()?.let {
            gravity[0] = event.values[0]
            gravity[1] = event.values[1]
            gravity[2] = event.values[2]
        }

        sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).firstOrNull()?.let {
            geomagnetic[0] = event.values[0]
            geomagnetic[1] = event.values[1]
            geomagnetic[2] = event.values[2]
        }

        if (SensorManager.getRotationMatrix(rotation, inclination, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotation, orientationAngles)
            onOrientationUpdate?.invoke(orientationAngles)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

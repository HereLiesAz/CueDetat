package com.hereliesaz.cuedetat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.hereliesaz.cuedetat.data.FullOrientation

class SensorFusionManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    var onOrientationUpdate: ((FullOrientation) -> Unit)? = null

    fun start() {
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            onOrientationUpdate?.invoke(FullOrientation(yaw, pitch, roll))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }
}

package com.hereliesaz.cuedetat.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import com.hereliesaz.cuedetat.config.AppConfig // For TAG only

class PitchSensor(
    private val context: Context,
    private val forwardTiltOffsetDegrees: Float, // Specific value passed in
    private val onPitchChanged: (Float) -> Unit
) : SensorEventListener {

    private companion object {
        private val TAG = AppConfig.TAG + "_PitchSensor"
    }

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            Log.e(TAG, "Failed to get SensorManager service.")
            Toast.makeText(context, "Sensor service not available.", Toast.LENGTH_LONG).show()
        } else {
            rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotationVectorSensor == null) {
                Log.w(TAG, "Rotation Vector Sensor not available on this device.")
                Toast.makeText(context, "Rotation Vector Sensor not available.", Toast.LENGTH_LONG).show()
            } else {
                Log.i(TAG, "Rotation Vector Sensor initialized.")
            }
        }
    }

    fun register() {
        if (rotationVectorSensor == null || sensorManager == null) {
            Log.w(TAG, "Cannot register listener: Sensor or SensorManager not available.")
            return
        }
        val registered = sensorManager?.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        if (registered == true) {
            Log.i(TAG, "PitchSensor listener registered.")
        } else {
            Log.e(TAG, "Failed to register PitchSensor listener.")
        }
    }

    fun unregister() {
        if (sensorManager == null) {
            Log.w(TAG, "Cannot unregister listener: SensorManager not available.")
            return
        }
        sensorManager?.unregisterListener(this)
        Log.i(TAG, "PitchSensor listener unregistered.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val rawPitchInRadians = orientationAngles[1]
            val rawPitchInDegrees = Math.toDegrees(rawPitchInRadians.toDouble()).toFloat()
            val conventionalAppPitch = -rawPitchInDegrees
            val finalAppPitch = conventionalAppPitch - forwardTiltOffsetDegrees // Use passed offset
            onPitchChanged(finalAppPitch)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Log.d(TAG, "Sensor accuracy changed for ${sensor?.name}: $accuracy")
    }
}
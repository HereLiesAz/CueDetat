package com.hereliesaz.cuedetat.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import com.hereliesaz.cuedetat.protractor.ProtractorConfig // Import Config

class DevicePitchSensor(
    private val context: Context,
    private val onPitchChanged: (Float) -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor == null) {
            Toast.makeText(context, "Rotation Vector Sensor not available.", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun register() {
        rotationVectorSensor?.also { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun unregister() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // orientationAngles[1] is pitch in radians.
            // Android sensor coordinate system:
            // Positive pitch: -Z axis (top of phone) rotates towards +Y axis.
            // If phone is upright (screen facing user, top edge up), this means top edge tilting AWAY from user.
            val rawPitchInRadians = orientationAngles[1]
            val rawPitchInDegrees = Math.toDegrees(rawPitchInRadians.toDouble()).toFloat()

            // Convert to application's convention for pitch:
            // We want positive pitch in the app to mean the protractor plane is tilted "up"
            // (top edge of the visual plane further away on screen, simulating looking down).
            // If rawPitchInDegrees is +15 (top away), -rawPitchInDegrees is -15.
            // If rawPitchInDegrees is -15 (top towards), -rawPitchInDegrees is +15.
            val conventionalAppPitch = -rawPitchInDegrees

            // Apply offset:
            // If user holds phone with top tilted FORWARD by OFFSET degrees,
            // rawPitch is approx -OFFSET. conventionalAppPitch is approx +OFFSET.
            // We want this +OFFSET to become 0 for the app.
            // So, finalAppPitch = conventionalAppPitch - OFFSET.
            val finalAppPitch =
                conventionalAppPitch - ProtractorConfig.FORWARD_TILT_AS_FLAT_OFFSET_DEGREES

            onPitchChanged(finalAppPitch)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not typically used for rotation vector
    }
}
package com.hereliesaz.cuedetat.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hereliesaz.cuedetat.R

class OrientationTrackingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private val currentQ = FloatArray(4).also { it[3] = 1f }
    private val baselineQ = FloatArray(4).also { it[3] = 1f }
    private var isSampling = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> startSampling()
                Intent.ACTION_SCREEN_ON -> stopSamplingAndSave()
            }
        }
    }
    private var receiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
        receiverRegistered = true

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_help_outline_24)
            .setContentTitle("Cue d'État")
            .setContentText("Tracking table orientation")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { sensorManager.unregisterListener(this) }
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
    }

    // If the user swipes the app away, stop the service so we don't keep a notification
    // and sensor listener around for an app the user has dismissed.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSampling() {
        if (isSampling) return
        System.arraycopy(currentQ, 0, baselineQ, 0, 4)
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isSampling = true
    }

    private fun stopSamplingAndSave() {
        if (!isSampling) return
        sensorManager.unregisterListener(this)
        isSampling = false

        val delta = quaternionDelta(baselineQ, currentQ)
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_Q0, delta[0])
            .putFloat(KEY_Q1, delta[1])
            .putFloat(KEY_Q2, delta[2])
            .putFloat(KEY_Q3, delta[3])
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getQuaternionFromVector(currentQ, event.values)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "orientation_tracking"
        const val PREFS_NAME = "orientation_prefs"
        const val KEY_Q0 = "delta_q0"
        const val KEY_Q1 = "delta_q1"
        const val KEY_Q2 = "delta_q2"
        const val KEY_Q3 = "delta_q3"
        const val KEY_TIMESTAMP = "delta_ts"
        const val MAX_AGE_MS = 30L * 60 * 1000

        fun start(context: Context) =
            context.startForegroundService(Intent(context, OrientationTrackingService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, OrientationTrackingService::class.java))

        fun readDelta(context: Context): FloatArray? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val ts = prefs.getLong(KEY_TIMESTAMP, 0L)
            if (ts == 0L || System.currentTimeMillis() - ts > MAX_AGE_MS) return null
            return floatArrayOf(
                prefs.getFloat(KEY_Q0, 0f),
                prefs.getFloat(KEY_Q1, 0f),
                prefs.getFloat(KEY_Q2, 0f),
                prefs.getFloat(KEY_Q3, 1f)
            )
        }

        internal fun quaternionMultiply(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(
            a[3]*b[0] + a[0]*b[3] + a[1]*b[2] - a[2]*b[1],
            a[3]*b[1] - a[0]*b[2] + a[1]*b[3] + a[2]*b[0],
            a[3]*b[2] + a[0]*b[1] - a[1]*b[0] + a[2]*b[3],
            a[3]*b[3] - a[0]*b[0] - a[1]*b[1] - a[2]*b[2]
        )

        internal fun quaternionDelta(from: FloatArray, to: FloatArray): FloatArray {
            val conjFrom = floatArrayOf(-from[0], -from[1], -from[2], from[3])
            return quaternionMultiply(to, conjFrom)
        }
    }
}

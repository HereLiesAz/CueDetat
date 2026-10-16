package com.hereliesaz.cuedetat.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.hereliesaz.cuedetat.wear.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Captures accelerometer + gyroscope samples while a stroke-consistency training session is
 * active and streams them to the paired phone over the Wearable Data Layer.
 *
 * Each sensor axis is updated independently (accelerometer and gyroscope callbacks fire on
 * their own cadence), so the latest reading of each is buffered and flushed together as a
 * single combined sample on a fixed timer. This decouples the wire format (one 8-byte
 * timestamp + 6 little-endian floats per message, matching what
 * `WristWearableRepository.onMessageReceived` parses on path "/trainer/imu") from the raw
 * sensor callback rate, and avoids saturating the MessageClient with two independent streams.
 *
 * Lifecycle messages: "/trainer/connected" is sent when streaming starts and
 * "/trainer/disconnected" when the service is destroyed. The phone also treats the IMU stream
 * itself as a liveness signal (see `WristWearableRepository.IMU_TIMEOUT_MS`), so a lost
 * lifecycle message cannot leave its overlay stuck. No "/trainer/hr" is sent: this module holds
 * no BODY_SENSORS permission and reads no heart-rate sensor.
 */
class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var sendJob: Job? = null
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }

    // Latest sample per axis group. Guarded by @Volatile since sensor callbacks and the send
    // loop run on different threads/dispatchers.
    @Volatile private var lastAccel = floatArrayOf(0f, 0f, 0f)
    @Volatile private var lastGyro = floatArrayOf(0f, 0f, 0f)
    @Volatile private var hasAccel = false
    @Volatile private var hasGyro = false

    /** Incremented on every sensor callback; a sample is sent only when it has moved. */
    @Volatile private var sampleSeq = 0L
    private var lastSentSeq = -1L
    private var sensorsRegistered = false

    /**
     * The phone's node id, supplied by [WearableMessageService] from the message that started
     * this service (the "/trainer/start" command's source node). Falls back to querying
     * connected nodes if the service was restarted by the system without that extra (e.g.
     * after process death with START_STICKY). Cleared on any send failure so the next attempt
     * re-resolves it (the phone may have re-paired or restarted with a new node).
     */
    @Volatile private var targetNodeId: String? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        // Listeners are registered in onStartCommand, when streaming actually begins.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_NODE_ID)?.let { targetNodeId = it }

        startForeground(NOTIFICATION_ID, buildNotification())

        if (!sensorsRegistered) {
            // GAME delay (~50Hz).
            accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            sensorsRegistered = true
        }

        if (sendJob == null || sendJob?.isActive != true) {
            sendJob = scope.launch {
                sendLifecycle(PATH_CONNECTED)
                sendLoop()
            }
        }

        return START_STICKY
    }

    /**
     * Sends at [SEND_INTERVAL_MS] while sends succeed; on failure the wait doubles up to
     * [MAX_BACKOFF_MS] and the node id is re-resolved.
     */
    private suspend fun sendLoop() {
        var waitMs = SEND_INTERVAL_MS
        while (scope.isActive) {
            val seq = sampleSeq
            if (hasAccel && hasGyro && seq != lastSentSeq) {
                if (sendSample()) {
                    lastSentSeq = seq
                    waitMs = SEND_INTERVAL_MS
                } else {
                    // Do not resend this (now stale) sample later; only fresh data goes out.
                    lastSentSeq = seq
                    waitMs = (waitMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            }
            kotlinx.coroutines.delay(waitMs)
        }
    }

    private suspend fun sendLifecycle(path: String) {
        val nodeId = resolveNodeId() ?: return
        runCatching { messageClient.sendMessage(nodeId, path, ByteArray(0)).await() }
            .onFailure { Log.w(TAG, "Failed to send $path to $nodeId", it); targetNodeId = null }
    }

    /** @return true if the sample was delivered to the Data Layer. */
    private suspend fun sendSample(): Boolean {
        val nodeId = resolveNodeId() ?: return false
        val accel = lastAccel
        val gyro = lastGyro

        val payload = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN).apply {
            putLong(System.currentTimeMillis())
            putFloat(accel[0])
            putFloat(accel[1])
            putFloat(accel[2])
            putFloat(gyro[0])
            putFloat(gyro[1])
            putFloat(gyro[2])
        }.array()

        return runCatching {
            messageClient.sendMessage(nodeId, PATH_IMU, payload).await()
        }.onFailure {
            Log.w(TAG, "Failed to send IMU sample to $nodeId", it)
            targetNodeId = null
        }.isSuccess
    }

    private suspend fun resolveNodeId(): String? {
        targetNodeId?.let { return it }
        return runCatching {
            nodeClient.connectedNodes.await().firstOrNull()?.id
        }.onSuccess { targetNodeId = it }
            .onFailure { Log.w(TAG, "Failed to resolve connected node", it) }
            .getOrNull()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel = floatArrayOf(event.values[0], event.values[1], event.values[2])
                hasAccel = true
                sampleSeq++
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyro = floatArrayOf(event.values[0], event.values[1], event.values[2])
                hasGyro = true
                sampleSeq++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stroke Trainer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recording wrist motion during stroke consistency training"
            }
            manager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording stroke data")
            .setContentText("Cue d'État is tracking your stroke consistency")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        sensorsRegistered = false
        scope.cancel()
        // Best-effort goodbye on a scope that outlives this one, bounded so it cannot leak.
        val nodeId = targetNodeId
        val client = messageClient
        CoroutineScope(Dispatchers.IO).launch {
            withTimeoutOrNull(LIFECYCLE_TIMEOUT_MS) {
                val id = nodeId ?: runCatching { nodeClient.connectedNodes.await().firstOrNull()?.id }.getOrNull()
                if (id != null) runCatching { client.sendMessage(id, PATH_DISCONNECTED, ByteArray(0)).await() }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SensorService"
        private const val CHANNEL_ID = "stroke_trainer_channel"
        private const val NOTIFICATION_ID = 1001

        /** Send a combined accel+gyro sample at ~20Hz, decoupled from the raw sensor rate. */
        private const val SEND_INTERVAL_MS = 50L

        /** Upper bound on the retry wait after consecutive send failures. */
        private const val MAX_BACKOFF_MS = 2_000L

        /** Bound on the best-effort "/trainer/disconnected" send from onDestroy. */
        private const val LIFECYCLE_TIMEOUT_MS = 3_000L

        private const val PATH_IMU = "/trainer/imu"
        private const val PATH_CONNECTED = "/trainer/connected"
        private const val PATH_DISCONNECTED = "/trainer/disconnected"

        /** Intent extra carrying the phone's node id, set by [WearableMessageService]. */
        const val EXTRA_NODE_ID = "node_id"
    }
}

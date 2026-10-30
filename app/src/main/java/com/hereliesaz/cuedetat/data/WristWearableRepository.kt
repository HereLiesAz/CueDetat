package com.hereliesaz.cuedetat.data

import androidx.annotation.Keep
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hereliesaz.cuedetat.domain.StrokeProfile
import com.hereliesaz.cuedetat.domain.StrokeSessionTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class WearableState(
    val isConnected: Boolean = false,
    /** Latest heart rate in BPM; null until a watch actually sends one (the current wear build never does). */
    val heartRate: Float? = null,
    val isShaking: Boolean = false,
    /** Fluidity of the last completed stroke (1.0 = perfect DTW match); null until one completes. */
    val consistencyScore: Float? = null
)

@Singleton
class WristWearableRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : MessageClient.OnMessageReceivedListener {

    private val messageClient = Wearable.getMessageClient(context)

    // Buffers IMU samples into stroke-sized windows and owns baseline calibration.
    // The first completed stroke window in a session becomes the baseline that
    // subsequent strokes are compared against (see StrokeSessionTracker).
    val sessionTracker = StrokeSessionTracker()

    private val _wearableState = MutableStateFlow(WearableState())
    val wearableState: StateFlow<WearableState> = _wearableState.asStateFlow()

    /**
     * Liveness watchdog. The watch announces "/trainer/connected" / "/trainer/disconnected"
     * itself, but those single messages can be lost (watch process killed, Bluetooth drop), so
     * the stream is also treated as the source of truth: any "/trainer/imu" sample marks the
     * watch connected, and [IMU_TIMEOUT_MS] without one marks it disconnected.
     */
    private val mainHandler = Handler(Looper.getMainLooper())
    private val disconnectOnSilence = Runnable { markConnected(false) }

    init {
        messageClient.addListener(this)
    }

    private fun markConnected(connected: Boolean) {
        mainHandler.removeCallbacks(disconnectOnSilence)
        if (connected) mainHandler.postDelayed(disconnectOnSilence, IMU_TIMEOUT_MS)
        if (_wearableState.value.isConnected != connected) {
            _wearableState.value = _wearableState.value.copy(isConnected = connected)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = messageEvent.data

        when (path) {
            // Heart rate is accepted if a watch build ever sends it (UTF-8 decimal string), but
            // the current wear module does not: it holds no BODY_SENSORS permission. It is
            // recorded for display/analysis only and does not by itself flag shaking.
            "/trainer/hr" -> {
                val hr = String(payload).toFloatOrNull()?.takeIf { it > 0f }
                _wearableState.value = _wearableState.value.copy(heartRate = hr)
            }
            "/trainer/imu" -> {
                markConnected(true)
                _wearableState.value = processImuSample(payload, sessionTracker, _wearableState.value)
            }
            "/trainer/connected" -> markConnected(true)
            "/trainer/disconnected" -> markConnected(false)
        }
    }

    // Helper to send command to the watch
    fun startTrainerSession() {
        // Starting a new session discards any previous baseline so the first
        // stroke of this session is re-calibrated as the new baseline.
        sessionTracker.reset()
        // Broadcast to all connected nodes
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, "/trainer/start", ByteArray(0))
            }
        }
    }

    fun stopTrainerSession() {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, "/trainer/stop", ByteArray(0))
            }
        }
    }

    companion object {
        /** Wire format: 1 little-endian long timestamp + 6 little-endian floats (3-axis accel, 3-axis gyro). */
        const val IMU_PAYLOAD_MIN_SIZE = 32

        /**
         * Silence after which the watch is considered gone. The watch streams at ~20 Hz
         * (wear `SensorService.SEND_INTERVAL_MS` = 50 ms) and backs off to at most 2 s between
         * retries on send failure, so 5 s of nothing means the stream has stopped.
         */
        const val IMU_TIMEOUT_MS = 5_000L

        /**
         * Parses a raw `/trainer/imu` message payload into a [StrokeProfile].
         * Returns null if the payload is too small to contain a full sample.
         */
        fun parseImuPayload(payload: ByteArray): StrokeProfile? {
            if (payload.size < IMU_PAYLOAD_MIN_SIZE) return null
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val time = buffer.long
            val ax = buffer.float
            val ay = buffer.float
            val az = buffer.float
            val gx = buffer.float
            val gy = buffer.float
            val gz = buffer.float
            return StrokeProfile(time, floatArrayOf(ax, ay, az), floatArrayOf(gx, gy, gz))
        }
    }
}

/**
 * Parses [payload] and feeds it into [tracker], returning the updated
 * [WearableState] reflecting the latest analysis (or [currentState] unchanged
 * if the payload was malformed or the stroke window hasn't completed yet).
 *
 * Extracted as a top-level function (rather than a private method) so it can
 * be unit tested without needing to construct [WristWearableRepository] and
 * its Android/Play-Services dependencies (Context, MessageClient).
 */
fun processImuSample(
    payload: ByteArray,
    tracker: StrokeSessionTracker,
    currentState: WearableState
): WearableState {
    val profile = WristWearableRepository.parseImuPayload(payload) ?: return currentState
    val result = tracker.addSample(profile, currentState.heartRate ?: 0f) ?: return currentState
    return currentState.copy(
        consistencyScore = result.fluidityScore,
        // Shaking is the stroke analyser's erratic flag only. A former `heartRate > 120` term
        // was an unprovenanced threshold on a signal the watch never sends; removed.
        isShaking = result.isErratic
    )
}

package com.hereliesaz.cuedetat.data

import android.content.Context
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

data class WearableState(
    val isConnected: Boolean = false,
    val heartRate: Float = 0f,
    val isShaking: Boolean = false,
    val consistencyScore: Float = 1.0f // 1.0 = Perfect DTW match
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

    init {
        messageClient.addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = messageEvent.data

        // Parse Wear OS data (e.g., HR, raw IMU buffers)
        // For now, stubbing state updates based on path
        when (path) {
            "/trainer/hr" -> {
                val hr = String(payload).toFloatOrNull() ?: 0f
                _wearableState.value = _wearableState.value.copy(
                    heartRate = hr,
                    isShaking = hr > 120f // simple arbitrary baseline heuristic
                )
            }
            "/trainer/imu" -> {
                _wearableState.value = processImuSample(payload, sessionTracker, _wearableState.value)
            }
            "/trainer/connected" -> {
                _wearableState.value = _wearableState.value.copy(isConnected = true)
            }
            "/trainer/disconnected" -> {
                _wearableState.value = _wearableState.value.copy(isConnected = false)
            }
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
    val result = tracker.addSample(profile, currentState.heartRate) ?: return currentState
    return currentState.copy(
        consistencyScore = result.fluidityScore,
        isShaking = result.isErratic || currentState.heartRate > 120f
    )
}

package com.hereliesaz.cuedetat.data

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hereliesaz.cuedetat.domain.StrokeAnalyzer
import com.hereliesaz.cuedetat.domain.StrokeProfile
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
    private val strokeAnalyzer = StrokeAnalyzer()
    
    // Simplistic rolling buffer for stroke profiles
    private val currentProfiles = mutableListOf<StrokeProfile>()
    private val baselineProfiles = mutableListOf<StrokeProfile>() // Ideally populated from a successful calibration stroke
    
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
                // Parse 6 floats (3 accel, 3 gyro) + 1 timestamp
                if (payload.size >= 32) {
                    val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                    val time = buffer.long
                    val ax = buffer.float
                    val ay = buffer.float
                    val az = buffer.float
                    val gx = buffer.float
                    val gy = buffer.float
                    val gz = buffer.float
                    
                    val profile = StrokeProfile(time, floatArrayOf(ax, ay, az), floatArrayOf(gx, gy, gz))
                    currentProfiles.add(profile)
                    
                    if (currentProfiles.size > 50) { // arbitrary window size
                        val result = strokeAnalyzer.analyzeStroke(baselineProfiles, currentProfiles, _wearableState.value.heartRate)
                        _wearableState.value = _wearableState.value.copy(
                            consistencyScore = result.fluidityScore,
                            isShaking = result.isErratic || _wearableState.value.heartRate > 120f
                        )
                        currentProfiles.removeAt(0)
                    }
                }
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
}

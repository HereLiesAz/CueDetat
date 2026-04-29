package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.meta.wearable.dat.camera.*
import com.meta.wearable.dat.camera.types.*
import com.meta.wearable.dat.core.*
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaWearableRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val tag = "MetaWearableRepo"
    
    private var currentSession: Session? = null
    private var currentStream: Stream? = null
    private var sessionJob: Job? = null
    
    private val _videoFrame = MutableStateFlow<Bitmap?>(null)
    val videoFrame = _videoFrame.asStateFlow()
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    fun startStreaming() {
        if (_isStreaming.value) return
        
        scope.launch {
            Log.d(tag, "Starting Meta glasses session...")
            try {
                val config = StreamConfiguration(
                    videoQuality = VideoQuality.MEDIUM,
                    frameRate = 24
                )

                Wearables.createSession(AutoDeviceSelector()).onSuccess { session ->
                    currentSession = session
                    session.start()

                    session.addStream(config).onSuccess { stream ->
                        currentStream = stream
                        stream.start()
                        _isStreaming.value = true
                        observeStream(stream)
                        Log.d(tag, "Meta stream started successfully")
                    }.onFailure { error ->
                        Log.e(tag, "Failed to add stream: $error")
                        stopStreaming()
                    }
                }.onFailure { error ->
                    Log.e(tag, "Failed to create session: $error")
                    _isStreaming.value = false
                }
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error in startStreaming", e)
                _isStreaming.value = false
                stopStreaming()
            }
        }
    }

    private fun observeStream(stream: Stream) {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            launch {
                stream.videoStream.collect { frame ->
                    _videoFrame.value = frame.toBitmap()
                }
            }

            launch {
                stream.state.collect { state ->
                    Log.d(tag, "Meta Stream State: $state")
                }
            }
        }
    }

    fun stopStreaming() {
        Log.d(tag, "Stopping Meta glasses stream/session")
        sessionJob?.cancel()
        sessionJob = null
        currentStream?.stop()
        currentStream = null
        currentSession?.stop()
        currentSession = null
        _isStreaming.value = false
        _videoFrame.value = null
    }

    private fun VideoFrame.toBitmap(): Bitmap? {
        return try {
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            buffer.rewind()

            val yuvImage = YuvImage(bytes, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e(tag, "Error converting VideoFrame to Bitmap", e)
            null
        }
    }
}

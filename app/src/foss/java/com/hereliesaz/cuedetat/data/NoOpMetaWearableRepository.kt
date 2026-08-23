package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS builds ship no Meta Wearables SDK, so there is nothing to connect to.
 *
 * This reports [isSupported] false rather than pretending to be an idle device,
 * so the UI can hide the glasses affordance instead of offering a toggle that
 * silently does nothing.
 */
@Singleton
class NoOpMetaWearableRepository @Inject constructor() : MetaWearableRepository {

    private val _videoFrame = MutableStateFlow<Bitmap?>(null)
    override val videoFrame: StateFlow<Bitmap?> = _videoFrame.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _connectionStatus = MutableStateFlow(MetaConnectionStatus.IDLE)
    override val connectionStatus: StateFlow<MetaConnectionStatus> =
        _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    override val isSupported: Boolean = false

    override fun initialize() = Unit

    override fun startStreaming() = Unit

    override fun stopStreaming() = Unit
}

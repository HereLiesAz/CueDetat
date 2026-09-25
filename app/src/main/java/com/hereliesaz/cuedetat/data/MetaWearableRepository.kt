package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Coarse connection state for the Meta glasses stream, surfaced to the UI so the
 * "glasses" toggle gives visible feedback instead of failing silently.
 */
enum class MetaConnectionStatus {
    IDLE,
    CONNECTING,
    STREAMING,
    NO_DEVICE,
    ERROR
}

/**
 * The glasses video source, behind a flavour seam.
 *
 * ## Why this is an interface
 *
 * It separates the app from the Meta SDK (resolved from a credentialed GitHub Packages registry)
 * and gives tests a seam. `WearableModule` binds [PlayMetaWearableRepository].
 */
interface MetaWearableRepository {

    /** Latest decoded frame from the glasses camera, or null when not streaming. */
    val videoFrame: StateFlow<Bitmap?>

    val isStreaming: StateFlow<Boolean>

    val connectionStatus: StateFlow<MetaConnectionStatus>

    /** Human-readable detail of the most recent failure, for diagnostics. */
    val lastError: StateFlow<String?>

    /**
     * False in builds with no glasses support at all. The UI should hide the
     * glasses affordance entirely rather than offering a control that cannot
     * work.
     */
    val isSupported: Boolean

    /**
     * Initialises the vendor SDK. Safe to call more than once, and safe to call
     * before Bluetooth permission has been granted — implementations report
     * failure through [lastError] rather than throwing.
     */
    fun initialize()

    fun startStreaming()

    fun stopStreaming()
}

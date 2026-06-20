// FILE: app/src/play/java/com/hereliesaz/cuedetat/delivery/PlayArFeatureDelivery.kt

package com.hereliesaz.cuedetat.delivery

import android.content.Context
import android.util.Log
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Play-flavor [ArFeatureDelivery]. The Expert-AR code + ARCore ship in the
 * `:feature_expert_ar` on-demand dynamic feature, so the base AAB stays smaller
 * and free users never download ARCore. This requests the split via Play Feature
 * Delivery and installs SplitCompat so the freshly installed split's classes
 * become loadable (for the facade's reflection) without an app restart.
 */
@Singleton
class PlayArFeatureDelivery @Inject constructor(
    @ApplicationContext private val context: Context,
) : ArFeatureDelivery {

    private val manager: SplitInstallManager by lazy { SplitInstallManagerFactory.create(context) }

    override val isInstalled: Boolean
        get() = manager.installedModules.contains(MODULE_NAME)

    override suspend fun ensureInstalled(): Boolean {
        if (isInstalled) {
            runCatching { SplitCompat.install(context) }
            return true
        }
        return suspendCancellableCoroutine { cont ->
            val request = SplitInstallRequest.newBuilder().addModule(MODULE_NAME).build()
            var mySessionId: Int? = null
            val listener = object : SplitInstallStateUpdatedListener {
                override fun onStateUpdate(state: com.google.android.play.core.splitinstall.SplitInstallSessionState) {
                    if (mySessionId != null && state.sessionId() != mySessionId) return
                    if (!state.moduleNames().contains(MODULE_NAME)) return
                    when (state.status()) {
                        SplitInstallSessionStatus.INSTALLED -> {
                            manager.unregisterListener(this)
                            runCatching { SplitCompat.install(context) }
                            if (cont.isActive) cont.resume(true)
                        }

                        SplitInstallSessionStatus.FAILED,
                        SplitInstallSessionStatus.CANCELED -> {
                            Log.w(TAG, "Expert-AR split install ended: ${state.status()}")
                            manager.unregisterListener(this)
                            if (cont.isActive) cont.resume(false)
                        }

                        else -> { /* PENDING / DOWNLOADING / INSTALLING — keep waiting */ }
                    }
                }
            }
            manager.registerListener(listener)
            try {
                manager.startInstall(request)
                    .addOnSuccessListener { id -> mySessionId = id }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Expert-AR split install failed to start", e)
                        runCatching { manager.unregisterListener(listener) }
                        if (cont.isActive) cont.resume(false)
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Expert-AR split install threw on start", t)
                runCatching { manager.unregisterListener(listener) }
                if (cont.isActive) cont.resume(false)
            }
            cont.invokeOnCancellation { runCatching { manager.unregisterListener(listener) } }
        }
    }

    companion object {
        private const val TAG = "PlayArFeatureDelivery"
        // Matches the Gradle module name in settings.gradle (":feature_expert_ar").
        private const val MODULE_NAME = "feature_expert_ar"
    }
}

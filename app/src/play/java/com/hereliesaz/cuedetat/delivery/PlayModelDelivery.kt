// FILE: app/src/play/java/com/hereliesaz/cuedetat/delivery/PlayModelDelivery.kt

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
 * Play-flavor [ModelDelivery]. The TFLite master model ships in the
 * `:feature_mlmodel` on-demand dynamic feature, so the base AAB stays ~24 MB
 * smaller. This requests the split via Play Feature Delivery the first time the
 * model is needed and installs SplitCompat so the freshly installed split's
 * assets become visible without an app restart.
 */
@Singleton
class PlayModelDelivery @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelDelivery {

    private val manager: SplitInstallManager by lazy { SplitInstallManagerFactory.create(context) }

    override val isModelInstalled: Boolean
        get() = manager.installedModules.contains(MODULE_NAME)

    override suspend fun ensureInstalled(): Boolean {
        if (isModelInstalled) {
            runCatching { SplitCompat.install(context) }
            return true
        }
        return suspendCancellableCoroutine { cont ->
            val request = SplitInstallRequest.newBuilder().addModule(MODULE_NAME).build()
            // React to terminal states for *our* session/module only, so a
            // concurrent install of some other split can't resume this
            // continuation. We match on module name immediately and, once the
            // session id is known, on session id too. Matching by module name
            // first also avoids a race where INSTALLED can arrive before
            // startInstall's id callback returns.
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
                            Log.w(TAG, "Model split install ended: ${state.status()}")
                            manager.unregisterListener(this)
                            if (cont.isActive) cont.resume(false)
                        }

                        else -> { /* PENDING / DOWNLOADING / INSTALLING — keep waiting */ }
                    }
                }
            }
            manager.registerListener(listener)
            // startInstall can fail both synchronously (throw) and asynchronously
            // (failure listener); cover both so the continuation always resumes.
            try {
                manager.startInstall(request)
                    .addOnSuccessListener { id -> mySessionId = id }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Model split install failed to start", e)
                        runCatching { manager.unregisterListener(listener) }
                        if (cont.isActive) cont.resume(false)
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Model split install threw on start", t)
                runCatching { manager.unregisterListener(listener) }
                if (cont.isActive) cont.resume(false)
            }
            cont.invokeOnCancellation { runCatching { manager.unregisterListener(listener) } }
        }
    }

    override fun assetContext(base: Context): Context {
        // The app context created before the on-demand install may not see the
        // new split, so build a fresh, SplitCompat-aware context.
        return runCatching {
            SplitCompat.install(base)
            base.createPackageContext(base.packageName, 0)
        }.getOrDefault(base)
    }

    companion object {
        private const val TAG = "PlayModelDelivery"
        // Matches the Gradle module name in settings.gradle (":feature_mlmodel").
        private const val MODULE_NAME = "feature_mlmodel"
    }
}

package com.hereliesaz.cuedetat.arfeature

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.delivery.ArFeatureDelivery
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The base-bound [ArController]. Stays stable for the app's lifetime while
 * swapping its [delegate] from [NoOpArController] to the real implementation once
 * the on-demand `:feature_expert_ar` split is available.
 *
 * Loading is on demand: the module is fetched the first time AR is actually
 * requested, so nobody pays the download for a feature they never open.
 * The implementation class lives in the dynamic feature module and is resolved
 * reflectively — the base has no compile-time dependency on it or on ARCore.
 *
 * [delegate] is a Compose [mutableStateOf] so the @Composable surfaces recompose
 * onto the real implementation the moment it loads.
 */
@Singleton
class ArControllerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val delivery: ArFeatureDelivery,
) : ArController {

    private var delegate by mutableStateOf<ArController>(NoOpArController)
    private val loadMutex = Mutex()
    @Volatile private var loaded = false

    override suspend fun ensureLoaded(): Boolean {
        if (loaded) return true
        if (!delivery.ensureInstalled()) {
            Log.e(TAG, "Expert AR delivery reported not installed")
            return false
        }
        return loadMutex.withLock {
            if (loaded) return@withLock true
            // Reflective class loading + instantiation can touch disk (loading a
            // freshly installed split's dex), so keep it off the main thread.
            val impl = withContext(Dispatchers.IO) {
                runCatching {
                    Class.forName(
                        IMPL_CLASS,
                        true,
                        context.classLoader,
                    ).getConstructor(Context::class.java).newInstance(context) as ArController
                }.onFailure { t ->
                    // The reason a load fails lives here and nowhere else; the UI only sees false.
                    Log.e(TAG, "Failed to instantiate $IMPL_CLASS", t)
                }.getOrNull()
            } ?: return@withLock false
            delegate = impl
            loaded = true
            true
        }
    }

    override fun probeCapability() = delegate.probeCapability()

    override fun updateUiState(state: CueDetatState) = delegate.updateUiState(state)

    override fun setTableZOffsetLogical(tableZOffsetLogical: Float) =
        delegate.setTableZOffsetLogical(tableZOffsetLogical)

    override fun lockTable(screenCorners: List<android.graphics.PointF>, logicalCorners: List<android.graphics.PointF>) =
        delegate.lockTable(screenCorners, logicalCorners)

    override fun unlockTable() = delegate.unlockTable()

    @Composable
    override fun ArBackground(modifier: Modifier, onEvent: (MainScreenEvent) -> Unit) =
        delegate.ArBackground(modifier, onEvent)

    // Stable analyzer that forwards each frame to the current delegate, so the
    // value remembered by ProtractorScreen keeps working across a delegate swap.
    private val analyzerForwarder = ImageAnalysis.Analyzer { proxy ->
        delegate.scanAnalyzer().analyze(proxy)
    }

    override fun scanAnalyzer(): ImageAnalysis.Analyzer = analyzerForwarder

    @Composable
    override fun ScanOverlay(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit) =
        delegate.ScanOverlay(uiState, onEvent)

    private companion object {
        /** Logcat tag: `adb logcat -s ExpertAR`. */
        const val TAG = "ExpertAR"
        const val IMPL_CLASS = "com.hereliesaz.cuedetat.feature.expert.ar.ArControllerImpl"
    }
}

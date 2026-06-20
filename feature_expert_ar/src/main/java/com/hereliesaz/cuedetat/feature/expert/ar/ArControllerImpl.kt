package com.hereliesaz.cuedetat.feature.expert.ar

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.arfeature.ArFeatureEntryPoint
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import dagger.hilt.android.EntryPointAccessors

/**
 * The on-demand-module implementation of the base [ArController] interface.
 *
 * Instantiated reflectively by ArControllerFacade once the `:feature_expert_ar`
 * split is installed (play) or compiled in (foss). It owns the AR singletons and
 * the scan controller, building them manually — Hilt does not extend into a
 * dynamic feature module, so base dependencies are pulled through the
 * [ArFeatureEntryPoint] Hilt entry point instead of constructor injection.
 *
 * Must expose a single public constructor taking a [Context] (that is the
 * contract the facade's reflection relies on).
 */
@Suppress("unused")
class ArControllerImpl(context: Context) : ArController {

    private val appContext = context.applicationContext
    private val deps = EntryPointAccessors.fromApplication(appContext, ArFeatureEntryPoint::class.java)

    private val arTableSession = ArTableSession(appContext)
    private val arFrameProcessor = ArFrameProcessor(deps.visionRepository())
    private val tableScanViewModel = TableScanViewModel(
        deps.tableScanRepository(),
        deps.pocketDetector(),
        arTableSession,
        arFrameProcessor,
    )

    private val analyzer: ImageAnalysis.Analyzer by lazy {
        TableScanAnalyzer(
            tableScanViewModel::onFrame,
            tableScanViewModel::onFeltColorSampled,
            tableScanViewModel::onCenterVSampled,
            tableScanViewModel.pocketDetector,
        )
    }

    override fun probeCapability(): DepthCapability =
        if (arTableSession.isArCoreAvailable()) {
            val testSession = arTableSession.createSession()
            val cap = arTableSession.capability
            if (testSession != null) arTableSession.close()
            cap
        } else {
            DepthCapability.NONE
        }

    override fun updateUiState(state: CueDetatState) {
        arFrameProcessor.updateUiState(state)
    }

    override fun setTableZOffsetLogical(tableZOffsetLogical: Float) {
        arTableSession.setTableHeightMeters(
            tableZOffsetLogical / TableFrameHomography.LOGICAL_UNITS_PER_METER
        )
    }

    @Composable
    override fun ArBackground(modifier: Modifier, onEvent: (MainScreenEvent) -> Unit) {
        ArCoreBackground(
            modifier = modifier,
            arTableSession = arTableSession,
            arFrameProcessor = arFrameProcessor,
            onEvent = onEvent,
        )
    }

    override fun scanAnalyzer(): ImageAnalysis.Analyzer = analyzer

    @Composable
    override fun ScanOverlay(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit) {
        TableScanScreen(
            onEvent = onEvent,
            uiState = uiState,
            viewModel = tableScanViewModel,
        )
    }

    override fun startManualHoleCapture() {
        tableScanViewModel.startManualHoleCapture()
    }
}

package com.hereliesaz.cuedetat.feature.expert.ar

import android.content.Context
import android.graphics.PointF
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
 * The implementation of the base [ArController] interface.
 *
 * `feature_expert_ar/` is not a Gradle module: its sources are compiled straight into :app
 * (app/build.gradle.kts sourceSets). ArControllerFacade constructs it directly (a
 * `Context` constructor). It builds the AR singletons manually and pulls base
 * dependencies through the [ArFeatureEntryPoint] Hilt entry point.
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
        arFrameProcessor,
    )

    private val analyzer: ImageAnalysis.Analyzer by lazy {
        TableScanAnalyzer(
            tableScanViewModel::onFrame,
            tableScanViewModel::onFeltColorSampled,
            pocketDetector = tableScanViewModel.pocketDetector,
        )
    }

    /** Uses a throwaway probe session; never closes the live AR session (see ArTableSession.probeCapability). */
    override fun probeCapability(): DepthCapability = arTableSession.probeCapability()

    override fun updateUiState(state: CueDetatState) {
        arFrameProcessor.updateUiState(state)
        arTableSession.updatePreviewInput(state)
    }

    override fun setTableZOffsetLogical(tableZOffsetLogical: Float) {
        arTableSession.setTableHeightMeters(
            tableZOffsetLogical / TableFrameHomography.LOGICAL_UNITS_PER_METER
        )
    }

    override fun nudgeTableRange(factor: Float) = arTableSession.nudgeRange(factor)

    override fun lockTable(screenCorners: List<PointF>, logicalCorners: List<PointF>) {
        arTableSession.requestLock(
            screenCorners.map { TableFrameHomography.Pt(it.x, it.y) },
            logicalCorners.map { TableFrameHomography.Pt(it.x, it.y) },
            feltHsv = tableScanViewModel.capturedFeltHsv.value,
        )
    }

    override fun unlockTable() = arTableSession.unlock()

    override fun saveCloudPlacement() = arTableSession.requestSaveCloudPlacement()

    override fun restoreCloudPlacement(logicalCorners: List<PointF>) =
        arTableSession.requestRestoreCloudPlacement(
            logicalCorners.map { TableFrameHomography.Pt(it.x, it.y) },
        )

    override fun forgetCloudPlacement() = arTableSession.forgetCloudPlacement()

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
}

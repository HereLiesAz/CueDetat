package com.hereliesaz.cuedetat.drawing.screen

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import com.hereliesaz.cuedetat.drawing.screen.elements.ActualCueBallOverlayDrawer
import com.hereliesaz.cuedetat.drawing.screen.elements.ActualTargetBallOverlayDrawer
import com.hereliesaz.cuedetat.drawing.screen.elements.DetectedBallOutlineDrawer
import com.hereliesaz.cuedetat.drawing.screen.labels.*
import kotlin.math.abs

class ScreenRenderer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val actualCueBallOverlayDrawer = ActualCueBallOverlayDrawer()
    private val actualTargetBallOverlayDrawer = ActualTargetBallOverlayDrawer()
    // Removed: private val floatingGhostCueBallDrawer = FloatingGhostCueBallDrawer()
    // Removed: private val floatingGhostTargetBallDrawer = FloatingGhostTargetBallDrawer()
    private val detectedBallOutlineDrawer = DetectedBallOutlineDrawer()

    private val invalidShotWarningDrawer = InvalidShotWarningDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val ghostTargetNameDrawer = GhostTargetNameDrawer(textLayoutHelper)
    private val ghostCueNameDrawer = GhostCueNameDrawer(textLayoutHelper)
    private val fitTargetInstructionDrawer = FitTargetInstructionDrawer(textLayoutHelper, viewWidthProvider)
    private val placeCueInstructionDrawer = PlaceCueInstructionDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val panHintDrawer = PanHintDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val pinchHintDrawer = PinchHintDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)

    private val selectionInstructionDrawer = SelectionInstructionDrawer(viewWidthProvider, viewHeightProvider)

    private val GHOST_LABEL_HORIZONTAL_PROXIMITY_THRESHOLD_FACTOR = 1.5f
    private val GHOST_LABEL_VERTICAL_ADJUSTMENT_AMOUNT_DP = 10f

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        actualTargetOverlayPosition: PointF, // Position for overlay on actual target ball (pitch-adjusted Y)
        actualTargetOverlayRadius: Float,    // Radius for overlay on actual target ball (pitch-scaled)
        actualCueOverlayPosition: PointF,    // Position for overlay on actual cue ball (pitch-adjusted Y)
        actualCueOverlayRadius: Float,       // Radius for overlay on actual cue ball (pitch-scaled)
        showErrorStyleForGhostBalls: Boolean,
        invalidShotWarningString: String?
    ) {
        if (!appState.isInitialized) return

        // 1. Draw Outlines for ALL currently detected balls (except selected ones)
        val selectedCueId = appState.selectedCueBall?.id
        val selectedTargetId = appState.selectedTargetBall?.id
        val unselectedDetectedBalls = appState.trackedBalls.filter {
            it.id != selectedCueId && it.id != selectedTargetId
        }
        // Pass zoomFactor to DetectedBallOutlineDrawer
        detectedBallOutlineDrawer.draw(canvas, appPaints, unselectedDetectedBalls, appState.zoomFactor)


        // 2. Draw Screen Space Visual Elements (Overlays on actual balls)
        if (appState.selectedTargetBall != null) {
            // Draw overlay on the actual target ball (this is now the "floating" element for the target)
            actualTargetBallOverlayDrawer.draw(
                canvas, appPaints,
                actualTargetOverlayPosition.x, actualTargetOverlayPosition.y, actualTargetOverlayRadius
            )
        }
        if (appState.selectedCueBall != null) {
            // Draw overlay on the actual cue ball (this is now the "floating" element for the cue)
            actualCueBallOverlayDrawer.draw(
                canvas, appPaints,
                actualCueOverlayPosition.x, actualCueOverlayPosition.y, actualCueOverlayRadius,
                showErrorStyleForGhostBalls
            )
        }

        // 3. Draw Screen Space Text Labels
        invalidShotWarningDrawer.draw(
            canvas, appState, appPaints, config, invalidShotWarningString
        )

        if (appState.areHelperTextsVisible) {
            selectionInstructionDrawer.draw(canvas, appState, appPaints, config)

            var targetLabelVerticalOffset = 0f
            var cueLabelVerticalOffset = 0f

            // Adjust label positions if balls are horizontally close
            if (appState.selectedTargetBall != null && appState.selectedCueBall != null &&
                actualTargetOverlayRadius > 0 && actualCueOverlayRadius > 0) {
                val horizontalDistance = abs(actualTargetOverlayPosition.x - actualCueOverlayPosition.x)
                val proximityThreshold = (actualTargetOverlayRadius + actualCueOverlayRadius) * GHOST_LABEL_HORIZONTAL_PROXIMITY_THRESHOLD_FACTOR

                if (horizontalDistance < proximityThreshold) {
                    val adjustmentPixels = (GHOST_LABEL_VERTICAL_ADJUSTMENT_AMOUNT_DP / appState.zoomFactor.coerceAtLeast(0.3f))
                    cueLabelVerticalOffset = -adjustmentPixels
                    targetLabelVerticalOffset = adjustmentPixels / 2f
                }
            }

            if (appState.selectedTargetBall != null) {
                // Label for the pitch-adjusted actual target overlay.
                ghostTargetNameDrawer.draw(
                    canvas, appState, appPaints, config,
                    actualTargetOverlayPosition, actualTargetOverlayRadius, // Use pitch-adjusted position and radius
                    targetLabelVerticalOffset
                )
            }
            if (appState.selectedCueBall != null) {
                // Label for the pitch-adjusted actual cue overlay.
                ghostCueNameDrawer.draw(
                    canvas, appState, appPaints, config,
                    actualCueOverlayPosition, actualCueOverlayRadius, // Use pitch-adjusted position and radius
                    cueLabelVerticalOffset
                )
            }

            fitTargetInstructionDrawer.draw(
                canvas, appState, appPaints, config,
                actualTargetOverlayPosition, actualTargetOverlayRadius
            )
            placeCueInstructionDrawer.draw(canvas, appState, appPaints, config)
            panHintDrawer.draw(canvas, appState, appPaints, config)
            pinchHintDrawer.draw(canvas, appState, appPaints, config)
        }
    }
}
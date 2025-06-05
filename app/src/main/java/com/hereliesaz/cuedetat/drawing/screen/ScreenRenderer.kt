package com.hereliesaz.cuedetat.drawing.screen

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import com.hereliesaz.cuedetat.drawing.screen.elements.*
import com.hereliesaz.cuedetat.drawing.screen.labels.*
import kotlin.math.abs // For horizontal distance check

class ScreenRenderer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val ghostCueBallDrawer = GhostCueBallDrawer()
    private val ghostTargetBallDrawer = GhostTargetBallDrawer()

    private val invalidShotWarningDrawer = InvalidShotWarningDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val ghostTargetNameDrawer = GhostTargetNameDrawer(textLayoutHelper)
    private val ghostCueNameDrawer = GhostCueNameDrawer(textLayoutHelper)
    private val fitTargetInstructionDrawer = FitTargetInstructionDrawer(textLayoutHelper, viewWidthProvider)
    private val placeCueInstructionDrawer = PlaceCueInstructionDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val panHintDrawer = PanHintDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val pinchHintDrawer = PinchHintDrawer(textLayoutHelper, viewWidthProvider, viewHeightProvider)

    private val selectionInstructionDrawer = SelectionInstructionDrawer(viewWidthProvider, viewHeightProvider) // New drawer

    // Threshold for horizontal proximity to trigger vertical offset adjustment
    private val GHOST_LABEL_HORIZONTAL_PROXIMITY_THRESHOLD_FACTOR = 1.5f // e.g., 1.5 * (sum of radii)
    private val GHOST_LABEL_VERTICAL_ADJUSTMENT_AMOUNT_DP = 10f // DP to adjust by

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        projectedTargetGhostCenter: PointF,
        targetGhostRadius: Float,
        projectedCueGhostCenter: PointF,
        cueGhostRadius: Float,
        showErrorStyleForGhostBalls: Boolean,
        invalidShotWarningString: String?
    ) {
        if (!appState.isInitialized) return

        // 1. Draw Screen Space Visual Elements (Ghost Balls)
        // These are drawn regardless of mode, as they represent the projection of tracked balls.
        ghostTargetBallDrawer.draw(
            canvas, appPaints,
            projectedTargetGhostCenter.x, projectedTargetGhostCenter.y, targetGhostRadius
        )
        ghostCueBallDrawer.draw(
            canvas, appPaints,
            projectedCueGhostCenter.x, projectedCueGhostCenter.y, cueGhostRadius,
            showErrorStyleForGhostBalls
        )

        // 2. Draw Screen Space Text Labels
        // Warning text for invalid shots (only in AIMING mode)
        invalidShotWarningDrawer.draw(
            canvas, appState, appPaints, config, invalidShotWarningString
        )

        // Conditional drawing of instructions based on selection modes
        if (appState.areHelperTextsVisible) {
            selectionInstructionDrawer.draw(canvas, appState, appPaints, config)

            // Calculate label offsets if needed for ghost balls
            var targetLabelVerticalOffset = 0f
            var cueLabelVerticalOffset = 0f
            if (targetGhostRadius > 0 && cueGhostRadius > 0) {
                val horizontalDistance = abs(projectedTargetGhostCenter.x - projectedCueGhostCenter.x)
                val proximityThreshold = (targetGhostRadius + cueGhostRadius) * GHOST_LABEL_HORIZONTAL_PROXIMITY_THRESHOLD_FACTOR

                if (horizontalDistance < proximityThreshold) {
                    val adjustmentPixels = (GHOST_LABEL_VERTICAL_ADJUSTMENT_AMOUNT_DP / appState.zoomFactor.coerceAtLeast(0.3f))
                    cueLabelVerticalOffset = -adjustmentPixels
                    targetLabelVerticalOffset = adjustmentPixels / 2f
                }
            }

            // Ghost ball name labels should be visible if helper texts are on and balls are selected, regardless of AIMING mode.
            // Their individual drawers (`GhostTargetNameDrawer`, `GhostCueNameDrawer`) now only check for `areHelperTextsVisible`.
            if (appState.selectedTargetBallId != null) { // Only draw label if a target ball is actually selected
                ghostTargetNameDrawer.draw(
                    canvas, appState, appPaints, config,
                    projectedTargetGhostCenter, targetGhostRadius,
                    targetLabelVerticalOffset
                )
            }
            if (appState.selectedCueBallId != null) { // Only draw label if a cue ball is actually selected
                ghostCueNameDrawer.draw(
                    canvas, appState, appPaints, config,
                    projectedCueGhostCenter, cueGhostRadius,
                    cueLabelVerticalOffset
                )
            }

            // Aiming-specific instructions and hints
            if (appState.currentMode == SelectionMode.AIMING) {
                fitTargetInstructionDrawer.draw(
                    canvas, appState, appPaints, config,
                    projectedTargetGhostCenter, targetGhostRadius
                )
                placeCueInstructionDrawer.draw(canvas, appState, appPaints, config)
                panHintDrawer.draw(canvas, appState, appPaints, config)
                pinchHintDrawer.draw(canvas, appState, appPaints, config)
            }
        }
    }
}
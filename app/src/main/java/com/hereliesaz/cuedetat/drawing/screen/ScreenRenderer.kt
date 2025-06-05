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

        // Conditional drawing of instructions based on selection mode
        if (appState.areHelperTextsVisible) {
            selectionInstructionDrawer.draw(canvas, appState, appPaints, config)

            when (appState.currentMode) {
                SelectionMode.AIMING -> {
                    // Only draw aiming-related labels in AIMING mode
                    var targetLabelVerticalOffset = 0f
                    var cueLabelVerticalOffset = 0f

                    // Simple overlap avoidance for ghost ball labels
                    if (targetGhostRadius > 0 && cueGhostRadius > 0) {
                        val horizontalDistance = abs(projectedTargetGhostCenter.x - projectedCueGhostCenter.x)
                        val proximityThreshold = (targetGhostRadius + cueGhostRadius) * GHOST_LABEL_HORIZONTAL_PROXIMITY_THRESHOLD_FACTOR

                        if (horizontalDistance < proximityThreshold) {
                            val adjustmentPixels = (GHOST_LABEL_VERTICAL_ADJUSTMENT_AMOUNT_DP / appState.zoomFactor.coerceAtLeast(0.3f))
                            cueLabelVerticalOffset = -adjustmentPixels
                            targetLabelVerticalOffset = adjustmentPixels / 2f
                        }
                    }

                    ghostTargetNameDrawer.draw(
                        canvas, appState, appPaints, config,
                        projectedTargetGhostCenter, targetGhostRadius,
                        targetLabelVerticalOffset
                    )
                    ghostCueNameDrawer.draw(
                        canvas, appState, appPaints, config,
                        projectedCueGhostCenter, cueGhostRadius,
                        cueLabelVerticalOffset
                    )
                    fitTargetInstructionDrawer.draw(
                        canvas, appState, appPaints, config,
                        projectedTargetGhostCenter, targetGhostRadius
                    )
                    placeCueInstructionDrawer.draw(canvas, appState, appPaints, config)
                    panHintDrawer.draw(canvas, appState, appPaints, config)
                    pinchHintDrawer.draw(canvas, appState, appPaints, config)
                }
                SelectionMode.SELECTING_CUE_BALL, SelectionMode.SELECTING_TARGET_BALL -> {
                    // No other labels are drawn during selection modes, only the instruction text
                }
            }
        }
    }
}
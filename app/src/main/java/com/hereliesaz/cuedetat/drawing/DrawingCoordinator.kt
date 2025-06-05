package com.hereliesaz.cuedetat.drawing

import android.graphics.Canvas
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode
import com.hereliesaz.cuedetat.geometry.GeometryCalculator
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords
import com.hereliesaz.cuedetat.geometry.models.DeflectionLineParams
import com.hereliesaz.cuedetat.geometry.models.ActualBallOverlayCoords
import com.hereliesaz.cuedetat.drawing.plane.PlaneRenderer
import com.hereliesaz.cuedetat.drawing.screen.ScreenRenderer
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import com.hereliesaz.cuedetat.drawing.utility.VisualStateLogic
import com.hereliesaz.cuedetat.ui.theme.AppPurple
import com.hereliesaz.cuedetat.ui.theme.AppWhite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class DrawingCoordinator(
    private val appState: AppState,
    private val appPaints: AppPaints,
    private val config: AppConfig,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val pitchOffsetPower = 2.0f

    private val geometryCalculator = GeometryCalculator(viewWidthProvider, viewHeightProvider)
    private val textLayoutHelper = TextLayoutHelper(viewWidthProvider, viewHeightProvider)
    private val planeRenderer = PlaneRenderer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val screenRenderer = ScreenRenderer(textLayoutHelper, viewWidthProvider, viewHeightProvider)

    private var currentWarningTextToDisplay: String? = null
    private var wasPreviouslyInvalidSetup: Boolean = false
    private var lastRandomWarningIndex: Int = -1

    private var lastActualBallOverlayCoords: ActualBallOverlayCoords? = null

    /**
     * Provides the last calculated actual ball overlay coordinates and radii.
     */
    fun getActualBallOverlayCoords(): ActualBallOverlayCoords? {
        return lastActualBallOverlayCoords
    }

    /**
     * Main drawing function called by MainOverlayView on each frame.
     */
    fun onDraw(canvas: Canvas) {
        // Ensure selected balls exist (even if default manual ones) before proceeding with drawing calculations
        if (appState.selectedCueBall == null || appState.selectedTargetBall == null || !appState.isInitialized || appState.logicalBallRadius <= 0.01f) {
            // These logs are useful if the default manual balls aren't being set for some reason
            // Log.w(TAG, "onDraw returning early: selected balls not initialized or radius too small.")
            return
        }

        textLayoutHelper.prepareForNewFrame()

        appState.graphicsCamera.save()
        appState.graphicsCamera.rotateX(appState.currentPitchAngle)
        appState.graphicsCamera.getMatrix(appState.pitchMatrix)
        appState.graphicsCamera.restore()
        appState.pitchMatrix.preTranslate(-appState.targetCircleCenter.x, -appState.targetCircleCenter.y)
        appState.pitchMatrix.postTranslate(appState.targetCircleCenter.x, appState.targetCircleCenter.y)
        val hasInversePitchMatrix = appState.pitchMatrix.invert(appState.inversePitchMatrix)

        // Get base (un-pitched) coordinates for the overlays on the *actual* balls on screen
        val baseActualBallOverlayCoords = geometryCalculator.calculateActualBallOverlayCoords(appState)
        lastActualBallOverlayCoords = baseActualBallOverlayCoords // Store the base coordinates

        val aimingLineLogicalCoords = geometryCalculator.calculateAimingLineLogicalCoords(appState, hasInversePitchMatrix)
        val deflectionParams = geometryCalculator.calculateDeflectionLineParams(appState)

        // Evaluate visual states unconditionally, as drawing is now always on.
        val visualStates = VisualStateLogic.evaluate(
            appState, geometryCalculator, aimingLineLogicalCoords,
            appState.cueCircleCenter, appState.targetCircleCenter
        )

        // Manage Warning Text Display (still only in AIMING mode)
        if (appState.currentMode == SelectionMode.AIMING) {
            if (visualStates.isCurrentlyInvalidShotSetup) {
                if (!wasPreviouslyInvalidSetup) {
                    if (config.INSULTING_WARNING_STRINGS.isNotEmpty()) {
                        var randomIndex = Random.nextInt(config.INSULTING_WARNING_STRINGS.size)
                        if (config.INSULTING_WARNING_STRINGS.size > 1) {
                            while (randomIndex == lastRandomWarningIndex) {
                                randomIndex = Random.nextInt(config.INSULTING_WARNING_STRINGS.size)
                            }
                        }
                        currentWarningTextToDisplay = config.INSULTING_WARNING_STRINGS[randomIndex]
                        lastRandomWarningIndex = randomIndex
                    } else {
                        currentWarningTextToDisplay = "Invalid Shot Setup"
                    }
                }
            } else {
                currentWarningTextToDisplay = null
            }
        } else {
            currentWarningTextToDisplay = null
        }
        wasPreviouslyInvalidSetup = visualStates.isCurrentlyInvalidShotSetup


        // Adjust Paint Properties based on Visual State
        if (visualStates.showWarningStyleForGhostBalls) {
            appPaints.targetLineGuidePaint.apply {
                strokeWidth = config.STROKE_TARGET_LINE_GUIDE + config.STROKE_DEFLECTION_LINE_BOLD_INCREASE
                setShadowLayer(config.GLOW_RADIUS_FIXED, 0f, 0f, appPaints.M3_GLOW_COLOR)
            }
        } else {
            appPaints.targetLineGuidePaint.apply {
                strokeWidth = config.STROKE_TARGET_LINE_GUIDE
                clearShadowLayer()
            }
        }

        // Adjust the aiming assist line colors based on validity (still only in AIMING mode for color change)
        val isPhysicalOverlap = geometryCalculator.distance(appState.cueCircleCenter, appState.targetCircleCenter) < (appState.logicalBallRadius * 2 - 0.1f)
        if (appState.currentMode == SelectionMode.AIMING && visualStates.isCurrentlyInvalidShotSetup) {
            appPaints.shotGuideNearPaint.apply { color = appPaints.M3_COLOR_ERROR; clearShadowLayer(); strokeWidth = config.STROKE_AIM_LINE_FAR }
            appPaints.shotGuideFarPaint.apply { color = appPaints.M3_COLOR_ERROR; clearShadowLayer(); strokeWidth = config.STROKE_AIM_LINE_FAR }
        } else {
            appPaints.shotGuideNearPaint.apply { color = AppWhite.toArgb(); strokeWidth = config.STROKE_AIM_LINE_NEAR }
            appPaints.shotGuideFarPaint.apply { color = AppPurple.toArgb(); strokeWidth = if (!isPhysicalOverlap) config.STROKE_AIM_LINE_NEAR else config.STROKE_AIM_LINE_FAR}
        }


        // Render plane elements (logical balls, protractor lines, shot guide)
        canvas.save()
        canvas.concat(appState.pitchMatrix)
        planeRenderer.draw(
            canvas, appState, appPaints, config,
            aimingLineCoords = aimingLineLogicalCoords,
            deflectionParams = deflectionParams,
            useErrorColor = visualStates.isCurrentlyInvalidShotSetup,
            actualCueBallScreenCenter = baseActualBallOverlayCoords.actualCueOverlayPosition // Pass actual cue's original screen position (unadjusted Y)
        )
        canvas.restore()

        // Calculate pitch-affected positions and radii for the *actual ball overlays*
        val pitchRadians = Math.toRadians(appState.currentPitchAngle.toDouble())
        val basePitchFactor = abs(sin(pitchRadians)).toFloat()
        val pitchYOffsetFactor = basePitchFactor.pow(config.PITCH_OFFSET_POWER) // Use config constant for power

        // Cosine for radius scaling: radius scales down as ball tilts "away" from viewer
        val radiusPerspectiveScaleFactor = abs(cos(pitchRadians)).toFloat().coerceAtLeast(0.5f) // Ensure minimum size
        val combinedRadiusScale = appState.zoomFactor * radiusPerspectiveScaleFactor


        // Pitch-adjusted Y and scaled radius for the actual target overlay
        val actualTargetOverlayDrawnCenterY = baseActualBallOverlayCoords.actualTargetOverlayPosition.y -
                (pitchYOffsetFactor * (appState.logicalBallRadius * appState.zoomFactor)) // Apply logical radius * zoom for Y offset
        val actualTargetOverlayScaledRadius = appState.logicalBallRadius * combinedRadiusScale // Apply logical radius * combined scale


        // Pitch-adjusted Y and scaled radius for the actual cue overlay
        val actualCueOverlayDrawnCenterY = baseActualBallOverlayCoords.actualCueOverlayPosition.y -
                (pitchYOffsetFactor * (appState.logicalBallRadius * appState.zoomFactor)) // Apply logical radius * zoom for Y offset
        val actualCueOverlayScaledRadius = appState.logicalBallRadius * combinedRadiusScale // Apply logical radius * combined scale


        // Render screen elements (actual ball overlays, labels)
        screenRenderer.draw(
            canvas, appState, appPaints, config,
            actualTargetOverlayPosition = PointF(baseActualBallOverlayCoords.actualTargetOverlayPosition.x, actualTargetOverlayDrawnCenterY),
            actualTargetOverlayRadius = actualTargetOverlayScaledRadius,
            actualCueOverlayPosition = PointF(baseActualBallOverlayCoords.actualCueOverlayPosition.x, actualCueOverlayDrawnCenterY),
            actualCueOverlayRadius = actualCueOverlayScaledRadius,
            showErrorStyleForGhostBalls = visualStates.showWarningStyleForGhostBalls,
            invalidShotWarningString = currentWarningTextToDisplay
        )
    }
}
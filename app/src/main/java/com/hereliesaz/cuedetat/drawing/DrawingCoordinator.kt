// app/src/main/java/com/hereliesaz/cuedetat/drawing/DrawingCoordinator.kt
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
import com.hereliesaz.cuedetat.geometry.models.ProjectedCoords
import com.hereliesaz.cuedetat.drawing.plane.PlaneRenderer
import com.hereliesaz.cuedetat.drawing.screen.ScreenRenderer
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import com.hereliesaz.cuedetat.drawing.utility.VisualStateLogic
import com.hereliesaz.cuedetat.ui.theme.AppPurple
import com.hereliesaz.cuedetat.ui.theme.AppWhite
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class DrawingCoordinator(
    private val appState: AppState,
    private val appPaints: AppPaints,
    private val config: AppConfig, // AppConfig instance is correctly passed
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val pitchOffsetPower = 2.0f // Controls the intensity of the pitch-based Y-offset for ghost balls

    private val geometryCalculator = GeometryCalculator(viewWidthProvider, viewHeightProvider)
    private val textLayoutHelper = TextLayoutHelper(viewWidthProvider, viewHeightProvider)
    private val planeRenderer = PlaneRenderer(textLayoutHelper, viewWidthProvider, viewHeightProvider)
    private val screenRenderer = ScreenRenderer(textLayoutHelper, viewWidthProvider, viewHeightProvider)

    private var currentWarningTextToDisplay: String? = null
    private var wasPreviouslyInvalidSetup: Boolean = false // Tracks if the previous frame was invalid
    private var lastRandomWarningIndex: Int = -1 // Stores index of last displayed random warning

    // Exposed for MainOverlayView to use for tap detection
    private var lastProjectedScreenData: ProjectedCoords? = null

    /**
     * Provides the last calculated projected screen coordinates and radii.
     * This is useful for hit testing in MainOverlayView (e.g., ball selection).
     */
    fun getProjectedScreenData(): ProjectedCoords? {
        return lastProjectedScreenData
    }

    /**
     * Main drawing function called by MainOverlayView on each frame.
     * Orchestrates all drawing operations for the protractor plane and screen-space elements.
     *
     * @param canvas The Android Canvas to draw on.
     */
    fun onDraw(canvas: Canvas) {
        // Do not draw if AppState is not initialized or if logical radius is negligible
        if (!appState.isInitialized || appState.currentLogicalRadius <= 0.01f) {
            return
        }

        // Clear text bounds for the new frame to allow for fresh collision detection
        textLayoutHelper.prepareForNewFrame()

        // --- Apply 3D Pitch Transformation to the Canvas ---
        appState.graphicsCamera.save()
        appState.graphicsCamera.rotateX(appState.currentPitchAngle) // Rotate around X-axis based on device pitch
        appState.graphicsCamera.getMatrix(appState.pitchMatrix) // Get the transformation matrix
        appState.graphicsCamera.restore()
        // Pre-translate to pivot around the targetCircleCenter, then post-translate back
        appState.pitchMatrix.preTranslate(-appState.targetCircleCenter.x, -appState.targetCircleCenter.y)
        appState.pitchMatrix.postTranslate(appState.targetCircleCenter.x, appState.targetCircleCenter.y)
        // Attempt to invert the matrix; useful for mapping screen points back to logical plane
        val hasInversePitchMatrix = appState.pitchMatrix.invert(appState.inversePitchMatrix)


        // --- Calculate Geometrical Data for Drawing ---
        val projectedScreenData = geometryCalculator.calculateProjectedScreenData(appState)
        lastProjectedScreenData = projectedScreenData // Store for external access (e.g., tap detection)
        val aimingLineLogicalCoords = geometryCalculator.calculateAimingLineLogicalCoords(appState, hasInversePitchMatrix)
        val deflectionParams = geometryCalculator.calculateDeflectionLineParams(appState)

        // --- Evaluate Current Visual States (e.g., valid shot, warnings) ---
        // Only evaluate complex shot states if in AIMING mode
        val visualStates = if (appState.currentMode == SelectionMode.AIMING) {
            VisualStateLogic.evaluate(
                appState, geometryCalculator, aimingLineLogicalCoords,
                appState.cueCircleCenter, appState.targetCircleCenter
            )
        } else {
            VisualStateLogic.EvaluatedVisualStates(isCurrentlyInvalidShotSetup = false, showWarningStyleForGhostBalls = false)
        }


        // --- Manage Warning Text Display (only in AIMING mode) ---
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
                        currentWarningTextToDisplay = "Invalid Shot Setup" // Fallback warning
                    }
                }
            } else {
                currentWarningTextToDisplay = null // Clear warning when state is valid
            }
        } else {
            currentWarningTextToDisplay = null // No warnings outside AIMING mode
        }
        wasPreviouslyInvalidSetup = visualStates.isCurrentlyInvalidShotSetup // Update state for next frame


        // --- Adjust Paint Properties based on Visual State ---
        // Adjust the yellow target line (main aiming line) based on warning style
        if (visualStates.showWarningStyleForGhostBalls) {
            appPaints.targetLineGuidePaint.apply {
                strokeWidth = config.STROKE_TARGET_LINE_GUIDE + config.STROKE_DEFLECTION_LINE_BOLD_INCREASE
                setShadowLayer(config.GLOW_RADIUS_FIXED, 0f, 0f, appPaints.M3_GLOW_COLOR)
            }
        } else {
            appPaints.targetLineGuidePaint.apply {
                strokeWidth = config.STROKE_TARGET_LINE_GUIDE // Use normal stroke
                clearShadowLayer() // Remove any glow
            }
        }

        // Adjust the aiming assist line colors based on validity
        // Only modify if in AIMING mode
        if (appState.currentMode == SelectionMode.AIMING) {
            val isPhysicalOverlap = geometryCalculator.distance(appState.cueCircleCenter, appState.targetCircleCenter) < (appState.currentLogicalRadius * 2 - 0.1f)
            if (visualStates.isCurrentlyInvalidShotSetup) {
                appPaints.shotGuideNearPaint.apply { color = appPaints.M3_COLOR_ERROR; clearShadowLayer(); strokeWidth = config.STROKE_AIM_LINE_FAR }
                appPaints.shotGuideFarPaint.apply { color = appPaints.M3_COLOR_ERROR; clearShadowLayer(); strokeWidth = config.STROKE_AIM_LINE_FAR }
            } else {
                appPaints.shotGuideNearPaint.apply { color = AppWhite.toArgb(); strokeWidth = config.STROKE_AIM_LINE_NEAR }
                appPaints.shotGuideFarPaint.apply { color = AppPurple.toArgb(); strokeWidth = if (!isPhysicalOverlap) config.STROKE_AIM_LINE_NEAR else config.STROKE_AIM_LINE_FAR}
            }
        } else {
            // If not in AIMING mode, lines are either not drawn or drawn with a neutral style
            // Ensure they are not drawn with error color when not in AIMING mode.
            appPaints.shotGuideNearPaint.apply { color = AppWhite.toArgb(); clearShadowLayer(); strokeWidth = config.STROKE_AIM_LINE_NEAR }
            appPaints.shotGuideFarPaint.apply { color = AppPurple.toArgb(); clearShadowLayer(); strokeWidth = config.STROKE_AIM_LINE_FAR }
        }


        // --- Draw Protractor Plane Elements ---
        canvas.save()
        canvas.concat(appState.pitchMatrix) // Apply the 3D pitch transformation
        planeRenderer.draw(
            canvas, appState, appPaints, config,
            aimingLineCoords = aimingLineLogicalCoords ?: AimingLineLogicalCoords(0f,0f,0f,0f,0f,0f,0f,0f),
            deflectionParams = deflectionParams,
            useErrorColor = visualStates.isCurrentlyInvalidShotSetup,
            actualCueBallScreenCenter = appState.selectedCueBallScreenCenter // Pass actual cue ball screen position
        )
        canvas.restore() // Restore canvas to original (non-pitched) state for screen-space drawing

        // --- Calculate Y-offset for Ghost Balls for Pseudo-3D Effect ---
        // This makes ghost balls appear to "float" above the plane based on pitch.
        val pitchRadians = Math.toRadians(appState.currentPitchAngle.toDouble())
        val basePitchFactor = abs(sin(pitchRadians)).toFloat()
        val pitchScaleFactor = basePitchFactor.pow(pitchOffsetPower)
        val targetGhostDrawnCenterY = projectedScreenData.targetProjected.y - (pitchScaleFactor * projectedScreenData.targetScreenRadius)
        val cueGhostDrawnCenterY = projectedScreenData.cueProjected.y - (pitchScaleFactor * projectedScreenData.cueScreenRadius)

        // --- Draw Screen Space Elements (Ghost Balls and Labels) ---
        screenRenderer.draw(
            canvas, appState, appPaints, config,
            projectedTargetGhostCenter = PointF(projectedScreenData.targetProjected.x, targetGhostDrawnCenterY),
            targetGhostRadius = projectedScreenData.targetScreenRadius,
            projectedCueGhostCenter = PointF(projectedScreenData.cueProjected.x, cueGhostDrawnCenterY),
            cueGhostRadius = projectedScreenData.cueScreenRadius,
            showErrorStyleForGhostBalls = visualStates.showWarningStyleForGhostBalls,
            invalidShotWarningString = currentWarningTextToDisplay
        )
    }
}
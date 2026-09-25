package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHostScope
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import com.hereliesaz.aznavrail.tutorial.AzGuidanceController
import com.hereliesaz.aznavrail.tutorial.AzInstructionStep
import com.hereliesaz.aznavrail.tutorial.LocalAzGuidanceController
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.composables.overlays.tutorialAimingLineShape
import com.hereliesaz.cuedetat.ui.composables.overlays.tutorialCueBallShape
import com.hereliesaz.cuedetat.ui.composables.overlays.tutorialGhostBallShape
import com.hereliesaz.cuedetat.ui.composables.overlays.tutorialTargetBallShape
import com.hereliesaz.cuedetat.ui.composables.overlays.tutorialZoomSliderShape

private val TUTORIAL_GOAL_IDS = setOf(
    "tutorial.expert",
    "tutorial.beginnerStatic",
    "tutorial.beginnerDynamic",
    "tutorial.dynamicNonAr",
    "tutorial.dynamicAr",
)

private fun buildTutorialSteps(
    texts: Array<String>,
    highlights: Map<Int, String>,
): List<AzInstructionStep> =
    texts.mapIndexed { index, text ->
        AzInstructionStep(text = text, highlightTargetId = highlights[index])
    }

/** Rail items hidden, not deleted; flip to restore. */
private const val SHOW_GLASSES = false
private const val SHOW_ADVISOR = false

private fun AzNavHostScope.azRailItemLowerCase(
    id: String,
    text: String,
    fillColor: Color,
    route: String? = null,
    onClick: () -> Unit
) {
    azRailItem(id = id, text = text.lowercase(), fillColor = fillColor, route = route, onClick = onClick)
}

@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
    captureEnabled: Boolean = false,
    onToggleCapture: () -> Unit = {},
    content: AzNavHostScope.() -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val b1Y = Color(0xFFFFEB3B); val b2B = Color(0xFF2196F3); val b3R = Color(0xFFF44336)
    val b4P = Color(0xFF9C27B0); val b5O = Color(0xFFFF9800); val b6G = Color(0xFF4CAF50)
    val b7M = Color(0xFFE91E63); val b8K = Color(0xFF212121); val b9Y = Color(0xFFFFF59D)
    val b10B = Color(0xFF64B5F6); val b11R = Color(0xFFE57373); val b12P = Color(0xFFBA68C8)
    val b13O = Color(0xFFFFB74D); val b14G = Color(0xFF81C784); val b15M = Color(0xFFF06292)

    // Tutorial flows for the AzNavRail 10.18 guidance framework. Each flow reuses the verbatim step
    // text from strings.xml; per-step highlightTargetId points the spotlight at the in-camera element
    // (registered below via azGuidanceTarget).
    val tutorialFlows = listOf(
        "tutorial.expert" to buildTutorialSteps(
            stringArrayResource(R.array.tutorial_general),
            mapOf(1 to "cue.targetBall", 2 to "cue.ghostBall", 3 to "cue.cueBall", 4 to "cue.zoomSlider"),
        ),
        "tutorial.beginnerStatic" to buildTutorialSteps(
            stringArrayResource(R.array.tutorial_beginner_static),
            mapOf(2 to "cue.targetBall", 3 to "cue.aimingLine", 4 to "cue.ghostBall"),
        ),
        "tutorial.beginnerDynamic" to buildTutorialSteps(
            stringArrayResource(R.array.tutorial_beginner_dynamic),
            mapOf(2 to "cue.targetBall", 3 to "cue.zoomSlider", 4 to "cue.aimingLine", 5 to "cue.ghostBall"),
        ),
        "tutorial.dynamicNonAr" to buildTutorialSteps(
            stringArrayResource(R.array.tutorial_dynamic_non_ar),
            mapOf(2 to "cue.targetBall"),
        ),
        "tutorial.dynamicAr" to buildTutorialSteps(
            stringArrayResource(R.array.tutorial_dynamic_ar),
            mapOf(2 to "cue.targetBall"),
        ),
    )
    // Holds the controller returned by AzHostActivityLayout so the "Tutorial" rail item's onClick —
    // defined inside the non-@Composable content lambda, before `guidance` is assigned — can reach it.
    val guidanceHolder = remember { mutableStateOf<AzGuidanceController?>(null) }

    // The rail accent, and every AzNavRail surface drawn in it, follows the app theme (guide §2.B).
    val railAccent = MaterialTheme.colorScheme.primary

    val guidance = AzHostActivityLayout(
        navController = navController,
        modifier = Modifier,
        currentDestination = currentDestination,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        // [SECTION 1] Configuration (DSL) - MANDATORY TOP POSITION
        azConfig(dockingSide = AzDockingSide.LEFT, packButtons = false, showFooter = true)
        azTheme(defaultShape = AzButtonShape.CIRCLE, activeColor = railAccent)
        azAdvanced(isLoading = false, helpEnabled = true, onDismissHelp = {})

        // [SECTION 1b] Tutorial guidance (AzNavRail 10.18 status-driven framework).
        // Register the moving on-screen highlight targets; the framework auto-draws the spotlight.
        azGuidanceTarget("cue.targetBall") { tutorialTargetBallShape(uiState) }
        azGuidanceTarget("cue.ghostBall") { tutorialGhostBallShape(uiState) }
        azGuidanceTarget("cue.cueBall") { tutorialCueBallShape(uiState) }
        azGuidanceTarget("cue.aimingLine") { tutorialAimingLineShape(uiState) }
        azGuidanceTarget("cue.zoomSlider") { tutorialZoomSliderShape(uiState) }

        // One goal per flow. Each goal's target is a sentinel status that never auto-fires; the flow
        // completes (and persists) when the user taps the Finish affordance on the final step.
        tutorialFlows.forEach { (goalId, steps) ->
            val done = "$goalId.done"
            azStatus(done) { false }
            azEdge(from = "az.app.ready", to = done, text = "", steps = steps)
            azGoal(id = goalId, target = done)
        }

        // Finish affordance: the framework leaves the final step non-tappable, so surface an explicit
        // "OK" that marks the active tutorial goal reached (target-independent; persists immediately).
        onscreen(alignment = Alignment.BottomCenter) {
            val controller = LocalAzGuidanceController.current
            val finishGoalId = controller?.currentInstructions?.firstOrNull { snap ->
                val gid = snap.goalId
                gid != null && gid in TUTORIAL_GOAL_IDS && snap.stepIndex >= snap.stepTotal - 1
            }?.goalId
            if (controller != null && finishGoalId != null) {
                TextButton(
                    onClick = { controller.markReached(finishGoalId) },
                    modifier = Modifier
                        .padding(bottom = 96.dp)
                        .background(railAccent, RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 2.dp)
                ) {
                    Text(text = "OK", color = Color.Black)
                }
            }
        }

        // [SECTION 2] Global Onscreen & Background layers (DSL)
        if (uiState.areHelpersVisible) {
            onscreen(alignment = Alignment.TopStart) {
                Text(
                    text = "Tap the icon for more",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }

        if (uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_CUE) {
            onscreen(alignment = Alignment.BottomCenter) {
                Text(
                    text = "Tap the cue ball",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        } else if (uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_TARGET) {
            onscreen(alignment = Alignment.BottomCenter) {
                Text(
                    text = "Tap the target ball",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }

        // [SECTION 3] Extra content from the caller (ProtractorScreen's Camera & Overlays)
        content()

        val inArSubMode = uiState.cameraMode == CameraMode.AR_SETUP || 
                          uiState.cameraMode == CameraMode.AR_ACTIVE || 
                          uiState.cameraMode == CameraMode.LITE_AR

        // For non-Beginner modes the Solids/Stripes toggle stays at the top of
        // the rail. Beginner mode renders the same toggle inside the Beginner
        // block below "view" so the related controls cluster together.
        if (inArSubMode && uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "target_type",
                isChecked = uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES,
                toggleOnText = "Stripes", toggleOffText = "Solids",
                fillColor = if (uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES) b4P else b8K,
                onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
            )
        }

        // [SECTION 4] Mode-specific Rail Items
        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItemLowerCase(id = "shake", text = "Shake", fillColor = b1Y, onClick = { onEvent(MainScreenEvent.Shake) })
            azRailItemLowerCase(id = "exit", text = "Exit", fillColor = b2B, onClick = { onEvent(MainScreenEvent.ExitToSplash) })
            return@AzHostActivityLayout // Hater Mode doesn't show standard nav
        }

        azRailToggle(
            // "wtf", not "help": AzNavRail reserves its own auto-placed help item.
            id = "wtf",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = "wtf?",
            toggleOffText = "wtf?",
            fillColor = b1Y,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )
        azMenuItem(
            id = "tutorial",
            text = "Tutorial",
            fillColor = b2B,
            onClick = {
                if (uiState.cameraMode == CameraMode.OFF) {
                    onEvent(MainScreenEvent.SetCameraMode(CameraMode.CAMERA))
                }
                val goal = if (uiState.cameraMode == CameraMode.LITE_AR) "tutorial.dynamicAr" else "tutorial.dynamicNonAr"
                guidanceHolder.value?.let { it.resetGuidance(goal); it.activate(goal) } // activate() no-ops on finished/skipped goals
            }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            // Lock: once AR is tracking, line the virtual table up over the real one and tap to
            // anchor it in the AR world; tap again to unlock and realign.
            if (uiState.cameraMode == CameraMode.AR_ACTIVE) {
                azRailToggle(
                    id = "lock",
                    isChecked = uiState.isArTableLocked,
                    toggleOnText = "unlock", toggleOffText = "lock",
                    fillColor = b10B,
                    onClick = {
                        onEvent(
                            if (uiState.isArTableLocked) MainScreenEvent.UnlockArTable
                            else MainScreenEvent.LockArTable
                        )
                    }
                )
            }

            // AR: starts the camera (felt capture, then AR tracking); "off" turns it straight off.
            val isCameraOn = uiState.cameraMode != CameraMode.OFF
            azRailToggle(
                id = "ar",
                isChecked = isCameraOn,
                toggleOnText = "off", toggleOffText = "ar",
                fillColor = b3R,
                onClick = {
                    onEvent(if (isCameraOn) MainScreenEvent.TurnCameraOff else MainScreenEvent.CycleCameraMode)
                }
            )

            // Glasses hidden until Meta wearable support is revisited.
            if (SHOW_GLASSES) azRailToggle(
                id = "meta_glasses",
                isChecked = uiState.cameraMode == CameraMode.META_GLASSES,
                toggleOnText = "phone", toggleOffText = "glasses",
                fillColor = b5O,
                onClick = {
                    if (uiState.cameraMode == CameraMode.META_GLASSES) {
                        onEvent(MainScreenEvent.TurnCameraOff)
                    } else {
                        onEvent(MainScreenEvent.SetExperienceMode(ExperienceMode.EXPERT)) // Ensure expert mode for glasses
                        onEvent(MainScreenEvent.SetCameraMode(CameraMode.META_GLASSES))
                    }
                }
            )

            if (inArSubMode) {
                if (uiState.pitchMatrix != null || uiState.topDownBitmap != null) {
                    azRailToggle(
                        id = "top_down_view",
                        isChecked = uiState.isTopDownViewActive,
                        toggleOnText = "back", toggleOffText = "view",
                        fillColor = b8K,
                        onClick = { 
                            if (uiState.isTopDownViewActive) onEvent(MainScreenEvent.ClearTopDownView)
                            else onEvent(MainScreenEvent.ToggleTopDownView)
                        }
                    )
                }
            }
        }
        azDivider()

        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(id = "spin", isChecked = uiState.isSpinControlVisible, toggleOnText = "Spin", toggleOffText = "Spin", fillColor = b4P, onClick = { onEvent(MainScreenEvent.ToggleSpinControl) })
            azRailToggle(id = "masse", isChecked = uiState.isMasseModeActive, toggleOnText = "Massé", toggleOffText = "Massé", fillColor = b5O, onClick = { onEvent(MainScreenEvent.ToggleMasseMode) })
            // Advisor hidden for the foreseeable future.
            if (SHOW_ADVISOR) azRailToggle(id = "advisor", isChecked = uiState.isAdvisorEnabled, toggleOnText = "Advisor", toggleOffText = "Advisor", fillColor = b13O, onClick = { onEvent(MainScreenEvent.ToggleAdvisor) })
        }

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(id = "bank", isChecked = uiState.isBankingMode, toggleOnText = "aim", toggleOffText = "bank", fillColor = b6G, onClick = { onEvent(MainScreenEvent.ToggleBankingMode) })
            azRailItemLowerCase(id = "add_obstacle", text = "add", fillColor = b7M, onClick = { onEvent(MainScreenEvent.AddObstacleBall) })
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "view_mode",
                isChecked = !uiState.isBeginnerViewLocked,
                toggleOnText = "dynamic", toggleOffText = "static",
                fillColor = if (!uiState.isBeginnerViewLocked) b6G else b7M,
                onClick = {
                    if (uiState.isBeginnerViewLocked) onEvent(MainScreenEvent.UnlockBeginnerView)
                    else onEvent(MainScreenEvent.LockBeginnerView)
                }
            )

            if (inArSubMode) {
                azRailToggle(
                    id = "target_type",
                    isChecked = uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES,
                    toggleOnText = "Stripes", toggleOffText = "Solids",
                    fillColor = if (uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES) b4P else b8K,
                    onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
                )
            }
        } else {
            val resetLabel = when {
                uiState.obstacleBalls.isNotEmpty() -> "clear"
                uiState.targetCvAnchor != null -> "undo"
                uiState.preResetState != null -> "undo"
                uiState.postResetState != null -> "redo"
                else -> "reset"
            }
            azRailItemLowerCase(id = "reset", text = resetLabel, fillColor = b8K, onClick = { onEvent(MainScreenEvent.Reset) })
        }
        azDivider()

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "size", text = "Table Size", fillColor = b13O, onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) })
            azMenuItem(id = "units", text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Metric" else "Imperial", fillColor = b14G, onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) })
            azDivider()
        }

        azMenuItem(id = "orientation", text = "Orientation", fillColor = b15M, onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) })
        // Training capture (CaptureRecorder): the setting the consent dialog points to.
        azMenuItem(id = "training_data", text = if (captureEnabled) "Training data: on" else "Training data: off", fillColor = b10B, onClick = onToggleCapture)

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "advanced", text = "Advanced", fillColor = b1Y, onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) })
        }

        // Replaces the old "Billing & License" item, which shipped a tester-
        // license console to every user in every build with no debug or
        // entitlement gate at all.
        azMenuItem(
            id = "support",
            text = "Support",
            fillColor = b1Y,
            onClick = { onEvent(MainScreenEvent.ToggleSupportSheet) },
        )

        azDivider()
        azMenuItem(id = "mode", text = "Mode: ${uiState.experienceMode?.name}", fillColor = b2B, onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) })
    }

    // Cache the controller for the manual "Tutorial" rail item (see guidanceHolder above).
    SideEffect { guidanceHolder.value = guidance }

    // Auto-start onboarding on first entry into a mode. Framework-persisted completion replaces the
    // old hasSeen* flags: a goal that has been finished once never re-activates.
    LaunchedEffect(guidance, uiState.experienceMode, uiState.isBeginnerViewLocked) {
        when (uiState.experienceMode) {
            ExperienceMode.BEGINNER -> {
                val goal = if (uiState.isBeginnerViewLocked) "tutorial.beginnerStatic" else "tutorial.beginnerDynamic"
                if (!guidance.isCompleted(goal) && !guidance.isDismissed(goal)) {
                    guidance.activate(goal)
                }
            }
            ExperienceMode.EXPERT -> {
                if (!guidance.isCompleted("tutorial.expert") && !guidance.isDismissed("tutorial.expert")) {
                    guidance.activate("tutorial.expert")
                }
            }
            else -> {}
        }
    }
}

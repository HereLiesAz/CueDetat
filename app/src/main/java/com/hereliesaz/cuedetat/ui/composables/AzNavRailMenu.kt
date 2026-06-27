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

private fun AzNavHostScope.azRailItemLowerCase(
    id: String,
    text: String,
    fillColor: Color,
    textColor: Color,
    route: String? = null,
    onClick: () -> Unit
) {
    azRailItem(id = id, text = text.lowercase(), fillColor = fillColor, textColor = textColor, route = route, onClick = onClick)
}

@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
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

    val guidance = AzHostActivityLayout(
        navController = navController,
        modifier = Modifier,
        currentDestination = currentDestination,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        // [SECTION 1] Configuration (DSL) - MANDATORY TOP POSITION
        azConfig(dockingSide = AzDockingSide.LEFT, packButtons = false, showFooter = true)
        azTheme(defaultShape = AzButtonShape.CIRCLE, activeColor = Color.White)
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
        // "Finish" that marks the active tutorial goal reached (target-independent; persists immediately).
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
                        .background(Color(0xFF2196F3), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 2.dp)
                ) {
                    Text(text = "Finish  ✓", color = Color.White)
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
                route = "main",
                isChecked = uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES,
                toggleOnText = "Stripes", toggleOffText = "Solids",
                fillColor = if (uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES) b4P else b8K,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
            )
        }

        // [SECTION 4] Mode-specific Rail Items
        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItemLowerCase(id = "shake", text = "Shake", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.Shake) })
            azRailItemLowerCase(id = "exit", text = "Exit", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ExitToSplash) })
            return@AzHostActivityLayout // Hater Mode doesn't show standard nav
        }

        azRailToggle(
            id = "help",
            route = "main",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = "Help",
            toggleOffText = "Help",
            fillColor = b1Y,
            textColor = Color.White,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )
        azMenuItem(
            id = "tutorial",
            route = "main",
            text = "Tutorial",
            fillColor = b2B,
            textColor = Color.White,
            onClick = {
                if (uiState.cameraMode == CameraMode.OFF) {
                    onEvent(MainScreenEvent.SetCameraMode(CameraMode.CAMERA))
                }
                val goal = if (uiState.cameraMode == CameraMode.LITE_AR) "tutorial.dynamicAr" else "tutorial.dynamicNonAr"
                guidanceHolder.value?.let { it.enable(); it.activate(goal) }
            }
        )

        // "Get Expert" tile only shows for non-entitled users in the play
        // flavor. In FOSS the entitlement is permanently active so this tile
        // never renders.
        if (!uiState.isExpertEntitled) {
            azRailItemLowerCase(
                id = "get_expert",
                text = "Get Expert",
                fillColor = b1Y,
                textColor = Color.White,
                onClick = {
                    onEvent(
                        MainScreenEvent.ShowPaywall(
                            com.hereliesaz.cuedetat.billing.PaywallTrigger.NAV_TILE
                        )
                    )
                }
            )
        }
        
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            val isArActive = uiState.cameraMode != CameraMode.OFF
            azRailToggle(
                id = "ar",
                route = "main",
                isChecked = isArActive,
                toggleOnText = "off", toggleOffText = "ar",
                fillColor = b3R, textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.CycleCameraMode) }
            )

            azRailToggle(
                id = "meta_glasses",
                route = "main",
                isChecked = uiState.cameraMode == CameraMode.META_GLASSES,
                toggleOnText = "phone", toggleOffText = "glasses",
                fillColor = b5O, textColor = Color.White,
                onClick = {
                    if (uiState.cameraMode == CameraMode.META_GLASSES) {
                        onEvent(MainScreenEvent.TurnCameraOff)
                    } else {
                        onEvent(MainScreenEvent.SetExperienceMode(ExperienceMode.EXPERT)) // Ensure expert mode for glasses
                        onEvent(MainScreenEvent.SetCameraMode(CameraMode.META_GLASSES))
                    }
                }
            )

            azRailItemLowerCase(id = "felt", text = "felt", fillColor = b11R, textColor = Color.White, onClick = {
                onEvent(MainScreenEvent.ToggleTableScanScreen)
            })

            azRailItemLowerCase(id = "holes", text = "holes", fillColor = b12P, textColor = Color.White, onClick = {
                onEvent(MainScreenEvent.StartManualHoleCapture)
            })

            if (inArSubMode) {
                if (uiState.pitchMatrix != null || uiState.topDownBitmap != null) {
                    azRailToggle(
                        id = "top_down_view",
                        route = "main",
                        isChecked = uiState.isTopDownViewActive,
                        toggleOnText = "back", toggleOffText = "view",
                        fillColor = b8K, textColor = Color.White,
                        onClick = { 
                            if (uiState.isTopDownViewActive) onEvent(MainScreenEvent.ClearTopDownView)
                            else onEvent(MainScreenEvent.ToggleTopDownView)
                        }
                    )
                }

                azRailItemLowerCase(id = "cancel_ar", text = "Cancel", fillColor = Color.DarkGray, textColor = Color.White, onClick = {
                    onEvent(MainScreenEvent.CancelArSetup) 
                })
            }
        }
        azDivider()

        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(id = "spin", route = "main", isChecked = uiState.isSpinControlVisible, toggleOnText = "Spin", toggleOffText = "Spin", fillColor = b4P, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleSpinControl) })
            azRailToggle(id = "masse", route = "main", isChecked = uiState.isMasseModeActive, toggleOnText = "Massé", toggleOffText = "Massé", fillColor = b5O, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleMasseMode) })
            azRailToggle(id = "advisor", route = "main", isChecked = uiState.isAdvisorEnabled, toggleOnText = "Advisor", toggleOffText = "Advisor", fillColor = b13O, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleAdvisor) })
        }

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(id = "bank", route = "main", isChecked = uiState.isBankingMode, toggleOnText = "aim", toggleOffText = "bank", fillColor = b6G, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleBankingMode) })
            azRailItemLowerCase(id = "add_obstacle", text = "add", fillColor = b7M, textColor = Color.White, onClick = { onEvent(MainScreenEvent.AddObstacleBall) })
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "view_mode",
                route = "main",
                isChecked = !uiState.isBeginnerViewLocked,
                toggleOnText = "dynamic", toggleOffText = "static",
                fillColor = if (!uiState.isBeginnerViewLocked) b6G else b7M,
                textColor = Color.White,
                onClick = {
                    if (uiState.isBeginnerViewLocked) onEvent(MainScreenEvent.UnlockBeginnerView)
                    else onEvent(MainScreenEvent.LockBeginnerView)
                }
            )

            if (inArSubMode) {
                azRailToggle(
                    id = "target_type",
                    route = "main",
                    isChecked = uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES,
                    toggleOnText = "Stripes", toggleOffText = "Solids",
                    fillColor = if (uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES) b4P else b8K,
                    textColor = Color.White,
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
            azRailItemLowerCase(id = "reset", text = resetLabel, fillColor = b8K, textColor = Color.White, onClick = { onEvent(MainScreenEvent.Reset) })
        }
        azDivider()

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "size", route = "main", text = "Table Size", fillColor = b13O, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) })
            azMenuItem(id = "units", route = "main", text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Metric" else "Imperial", fillColor = b14G, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) })
            azDivider()
        }

        azMenuItem(id = "orientation", route = "main", text = "Orientation", fillColor = b15M, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) })

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "advanced", route = "main", text = "Advanced", fillColor = b1Y, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) })
        }

        azMenuItem(
            id = "billing-debug",
            route = "main",
            text = "Billing & License",
            fillColor = b1Y,
            textColor = Color.White,
            onClick = { onEvent(MainScreenEvent.ToggleBillingDebugDialog) },
        )

        azDivider()
        azMenuItem(id = "mode", route = "main", text = "Mode: ${uiState.experienceMode?.name}", fillColor = b2B, textColor = Color.White, onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) })
    }

    // Cache the controller for the manual "Tutorial" rail item (see guidanceHolder above).
    SideEffect { guidanceHolder.value = guidance }

    // Auto-start onboarding on first entry into a mode. Framework-persisted completion replaces the
    // old hasSeen* flags: a goal that has been finished once never re-activates.
    LaunchedEffect(guidance, uiState.experienceMode, uiState.isBeginnerViewLocked) {
        when (uiState.experienceMode) {
            ExperienceMode.BEGINNER -> {
                val goal = if (uiState.isBeginnerViewLocked) "tutorial.beginnerStatic" else "tutorial.beginnerDynamic"
                if (!guidance.isCompleted(goal)) {
                    guidance.enable(); guidance.activate(goal)
                }
            }
            ExperienceMode.EXPERT -> {
                if (!guidance.isCompleted("tutorial.expert")) {
                    guidance.enable(); guidance.activate("tutorial.expert")
                }
            }
            else -> {}
        }
    }
}

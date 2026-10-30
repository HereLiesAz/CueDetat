package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
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
    debugLogEnabled: Boolean = false,
    onToggleDebugLog: () -> Unit = {},
    onShareDebugLog: () -> Unit = {},
    content: AzNavHostScope.() -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val b1Y = Color(0xFFFFEB3B); val b2B = Color(0xFF2196F3); val b3R = Color(0xFFF44336)
    val b4P = Color(0xFF9C27B0); val b5O = Color(0xFFFF9800); val b6G = Color(0xFF4CAF50)
    val b7M = Color(0xFFE91E63); val b8K = Color(0xFF212121)
    val b10B = Color(0xFF64B5F6); val b11R = Color(0xFFE57373)
    val b13O = Color(0xFFFFB74D); val b14G = Color(0xFF81C784); val b15M = Color(0xFFF06292)

    // Tutorial flows for the AzNavRail 11.50 guidance framework. Each flow reuses the verbatim step
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
    val confirmCloudSave = remember { mutableStateOf(false) }

    // The rail accent, and every AzNavRail surface drawn in it, follows the app theme (guide §2.B).
    val railAccent = MaterialTheme.colorScheme.primary
    // The rail DSL lambda is not @Composable, so resolve its strings here.
    val supportLabel = stringResource(R.string.rail_support)
    val rail_stripesText = stringResource(R.string.rail_stripes)
    val rail_solidsText = stringResource(R.string.rail_solids)
    val rail_shakeText = stringResource(R.string.rail_shake)
    val rail_exitText = stringResource(R.string.rail_exit)
    val rail_wtfText = stringResource(R.string.rail_wtf)
    val rail_tutorialText = stringResource(R.string.rail_tutorial)
    val rail_unlockText = stringResource(R.string.rail_unlock)
    val rail_lockText = stringResource(R.string.rail_lock)
    val rail_save_24hText = stringResource(R.string.rail_save_24h)
    val rail_restoreText = stringResource(R.string.rail_restore)
    val rail_forget_savedText = stringResource(R.string.rail_forget_saved)
    val rail_offText = stringResource(R.string.rail_off)
    val rail_arText = stringResource(R.string.rail_ar)
    val rail_phoneText = stringResource(R.string.rail_phone)
    val rail_glassesText = stringResource(R.string.rail_glasses)
    val rail_backText = stringResource(R.string.rail_back)
    val rail_viewText = stringResource(R.string.rail_view)
    val rail_spinText = stringResource(R.string.rail_spin)
    val rail_masseText = stringResource(R.string.rail_masse)
    val rail_advisorText = stringResource(R.string.rail_advisor)
    val rail_aimText = stringResource(R.string.rail_aim)
    val rail_bankText = stringResource(R.string.rail_bank)
    val rail_addText = stringResource(R.string.rail_add)
    val rail_cue_ballText = stringResource(R.string.rail_cue_ball)
    val rail_target_ballText = stringResource(R.string.rail_target_ball)
    val rail_dynamicText = stringResource(R.string.rail_dynamic)
    val rail_staticText = stringResource(R.string.rail_static)
    val rail_clearText = stringResource(R.string.rail_clear)
    val rail_undoText = stringResource(R.string.rail_undo)
    val rail_redoText = stringResource(R.string.rail_redo)
    val rail_resetText = stringResource(R.string.rail_reset)
    val rail_table_sizeText = stringResource(R.string.rail_table_size)
    val rail_metricText = stringResource(R.string.rail_metric)
    val rail_imperialText = stringResource(R.string.rail_imperial)
    // Shows the lock a tap is about to apply (taps cycle it; a 1 s debounce commits), so a tap on a
    // device already in that orientation still visibly does something.
    val rail_orientationText = stringResource(
        R.string.rail_orientation,
        (uiState.pendingOrientationLock ?: uiState.orientationLock).name.lowercase().replaceFirstChar { it.uppercase() },
    )
    val rail_training_data_onText = stringResource(R.string.rail_training_data_on)
    val rail_training_data_offText = stringResource(R.string.rail_training_data_off)
    val rail_advancedText = stringResource(R.string.rail_advanced)
    val rail_luminanceText = stringResource(R.string.rail_luminance)
    val rail_glow_stickText = stringResource(R.string.rail_glow_stick)
    val rail_snapping_onText = stringResource(R.string.rail_snapping_on)
    val rail_snapping_offText = stringResource(R.string.rail_snapping_off)
    val rail_scan_tableText = stringResource(R.string.rail_scan_table)
    val rail_forget_scanText = stringResource(R.string.rail_forget_scan)
    val rail_debug_log_onText = stringResource(R.string.rail_debug_log_on)
    val rail_debug_log_offText = stringResource(R.string.rail_debug_log_off)
    val rail_share_debug_logText = stringResource(R.string.rail_share_debug_log)
    // The mode a tap is about to apply (taps cycle it, then a 1 s debounce commits), not the old one:
    // switching starts the table over, so the user must see which mode they are landing on.
    val railModeName = (uiState.pendingExperienceMode ?: uiState.experienceMode)?.name.orEmpty()
        .lowercase().replaceFirstChar { it.uppercase() }
    val railModeFormat = stringResource(R.string.rail_mode, railModeName)

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

        // [SECTION 1b] Tutorial guidance (AzNavRail 11.50 status-driven framework).
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
        // "Finish" button (the label the step texts name) that marks the active tutorial goal reached (target-independent; persists immediately).
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
                    Text(text = stringResource(R.string.tutorial_finish), color = Color.Black)
                }
            }
        }

        // [SECTION 2] Global Onscreen & Background layers (DSL)
        if (uiState.areHelpersVisible) {
            onscreen(alignment = Alignment.TopStart) {
                Text(
                    text = stringResource(R.string.helpers_tap_icon_hint),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }

        if (uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_CUE) {
            onscreen(alignment = Alignment.BottomCenter) {
                Text(
                    text = stringResource(R.string.ball_selection_tap_cue),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        } else if (uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_TARGET) {
            onscreen(alignment = Alignment.BottomCenter) {
                Text(
                    text = stringResource(R.string.ball_selection_tap_target),
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
                toggleOnText = rail_stripesText, toggleOffText = rail_solidsText,
                fillColor = if (uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES) b4P else b8K,
                onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
            )
        }

        // [SECTION 4] Mode-specific Rail Items
        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItemLowerCase(id = "shake", text = rail_shakeText, fillColor = b1Y, onClick = { onEvent(MainScreenEvent.Shake) })
            azRailItemLowerCase(id = "exit", text = rail_exitText, fillColor = b2B, onClick = { onEvent(MainScreenEvent.ExitToSplash) })
            return@AzHostActivityLayout // Hater Mode doesn't show standard nav
        }

        azRailToggle(
            // "wtf", not "help": AzNavRail reserves its own auto-placed help item.
            id = "wtf",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = rail_wtfText,
            toggleOffText = rail_wtfText,
            fillColor = b1Y,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )
        azMenuItem(
            id = "tutorial",
            text = rail_tutorialText,
            fillColor = b2B,
            onClick = {
                // Replay the tutorial for the mode the user is in. Only the dynamic Beginner flows
                // teach with the camera, so only they turn it on.
                val dynamicBeginner = uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked
                val goal = when {
                    uiState.experienceMode == ExperienceMode.EXPERT -> "tutorial.expert"
                    uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "tutorial.beginnerStatic"
                    uiState.cameraMode == CameraMode.LITE_AR -> "tutorial.dynamicAr"
                    else -> "tutorial.dynamicNonAr"
                }
                if (dynamicBeginner && uiState.cameraMode == CameraMode.OFF) {
                    onEvent(MainScreenEvent.SetCameraMode(CameraMode.CAMERA))
                }
                guidanceHolder.value?.let { it.resetGuidance(goal); it.activate(goal) } // activate() no-ops on finished/skipped goals
            }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            // Lock: once AR is tracking, line the virtual table up over the real one and tap to
            // anchor it in the AR world; tap again to unlock and realign.
            // ARCore only: without it nothing drains the lock or cloud requests and the items would
            // sit there doing nothing.
            if (uiState.cameraMode == CameraMode.AR_ACTIVE &&
                uiState.depthCapability != com.hereliesaz.cuedetat.domain.DepthCapability.NONE) {
                azRailToggle(
                    id = "lock",
                    isChecked = uiState.isArTableLocked,
                    toggleOnText = rail_unlockText, toggleOffText = rail_lockText,
                    fillColor = b10B,
                    onClick = {
                        onEvent(
                            if (uiState.isArTableLocked) MainScreenEvent.UnlockArTable
                            else MainScreenEvent.LockArTable
                        )
                    }
                )
                // Cloud hosting is never automatic: explicit confirmation is required.
                if (uiState.isArTableLocked) {
                    azRailItemLowerCase(
                        id = "ar_cloud_save", text = rail_save_24hText,
                        fillColor = b6G, onClick = { confirmCloudSave.value = true },
                    )
                }
                azRailItemLowerCase(
                    id = "ar_cloud_restore", text = rail_restoreText,
                    fillColor = b10B, onClick = { onEvent(MainScreenEvent.RestoreCloudPlacement) },
                )
                azRailItemLowerCase(
                    id = "ar_cloud_forget", text = rail_forget_savedText,
                    fillColor = b11R, onClick = { onEvent(MainScreenEvent.ForgetCloudPlacement) },
                )
            }

            // Rescan: only while an AR camera mode is on, and not while the scan overlay is
            // already up (the tap would just close it). ToggleReducer resets the old scan.
            if (uiState.cameraMode == CameraMode.AR_SETUP || uiState.cameraMode == CameraMode.AR_ACTIVE) {
                if (!uiState.showTableScanScreen) {
                    azRailItemLowerCase(
                        id = "ar_scan_table", text = rail_scan_tableText,
                        fillColor = b10B, onClick = { onEvent(MainScreenEvent.ToggleTableScanScreen) },
                    )
                }
            }
            // Forget scan: drops the held table model (and the saved copy), only if one exists.
            if (uiState.tableScanModel != null) {
                azRailItemLowerCase(
                    id = "ar_scan_forget", text = rail_forget_scanText,
                    fillColor = b11R, onClick = { onEvent(MainScreenEvent.ClearTableScan) },
                )
            }

            // AR: starts the camera (felt capture, then AR tracking); "off" turns it straight off.
            val isCameraOn = uiState.cameraMode != CameraMode.OFF
            azRailToggle(
                id = "ar",
                isChecked = isCameraOn,
                toggleOnText = rail_offText, toggleOffText = rail_arText,
                fillColor = b3R,
                onClick = {
                    onEvent(if (isCameraOn) MainScreenEvent.TurnCameraOff else MainScreenEvent.CycleCameraMode)
                }
            )

            // Glasses hidden until Meta wearable support is revisited.
            if (SHOW_GLASSES) azRailToggle(
                id = "meta_glasses",
                isChecked = uiState.cameraMode == CameraMode.META_GLASSES,
                toggleOnText = rail_phoneText, toggleOffText = rail_glassesText,
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
                        toggleOnText = rail_backText, toggleOffText = rail_viewText,
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
            azRailToggle(id = "spin", isChecked = uiState.isSpinControlVisible, toggleOnText = rail_spinText, toggleOffText = rail_spinText, fillColor = b4P, onClick = { onEvent(MainScreenEvent.ToggleSpinControl) })
            azRailToggle(id = "masse", isChecked = uiState.isMasseModeActive, toggleOnText = rail_masseText, toggleOffText = rail_masseText, fillColor = b5O, onClick = { onEvent(MainScreenEvent.ToggleMasseMode) })
            // Advisor hidden for the foreseeable future.
            if (SHOW_ADVISOR) azRailToggle(id = "advisor", isChecked = uiState.isAdvisorEnabled, toggleOnText = rail_advisorText, toggleOffText = rail_advisorText, fillColor = b13O, onClick = { onEvent(MainScreenEvent.ToggleAdvisor) })
        }

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            // Tap one, then tap that ball on the screen: set both balls up, then drag to aim.
            // Coloured as the virtual balls are drawn (the cue ball's ring, the target's).
            azRailToggle(
                id = "pick_cue",
                isChecked = uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_CUE,
                toggleOnText = rail_cue_ballText, toggleOffText = rail_cue_ballText,
                fillColor = com.hereliesaz.cuedetat.view.config.ball.ActualCueBall().strokeColor,
                onClick = { onEvent(MainScreenEvent.BeginBallSelection(BallSelectionPhase.AWAITING_CUE)) }
            )
            azRailToggle(
                id = "pick_target",
                isChecked = uiState.ballSelectionPhase == BallSelectionPhase.AWAITING_TARGET,
                toggleOnText = rail_target_ballText, toggleOffText = rail_target_ballText,
                fillColor = com.hereliesaz.cuedetat.view.config.ball.TargetBall().strokeColor,
                onClick = { onEvent(MainScreenEvent.BeginBallSelection(BallSelectionPhase.AWAITING_TARGET)) }
            )
            azRailToggle(id = "bank", isChecked = uiState.isBankingMode, toggleOnText = rail_aimText, toggleOffText = rail_bankText, fillColor = b6G, onClick = { onEvent(MainScreenEvent.ToggleBankingMode) })
            azRailItemLowerCase(id = "add_obstacle", text = rail_addText, fillColor = b7M, onClick = { onEvent(MainScreenEvent.AddObstacleBall) })
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "view_mode",
                isChecked = !uiState.isBeginnerViewLocked,
                toggleOnText = rail_dynamicText, toggleOffText = rail_staticText,
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
                    toggleOnText = rail_stripesText, toggleOffText = rail_solidsText,
                    fillColor = if (uiState.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES) b4P else b8K,
                    onClick = { onEvent(MainScreenEvent.ToggleTargetType) }
                )
            }
        } else {
            val resetLabel = when {
                uiState.obstacleBalls.isNotEmpty() -> rail_clearText
                uiState.targetCvAnchor != null -> rail_undoText
                // Undo/redo only while nothing has changed since; after that Reset starts fresh.
                uiState.preResetState != null && !uiState.valuesChangedSinceReset -> rail_undoText
                uiState.postResetState != null && !uiState.valuesChangedSinceReset -> rail_redoText
                else -> rail_resetText
            }
            azRailItemLowerCase(id = "reset", text = resetLabel, fillColor = b8K, onClick = { onEvent(MainScreenEvent.Reset) })
        }
        azDivider()

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "size", text = rail_table_sizeText, fillColor = b13O, onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) })
            azMenuItem(id = "units", text = if (uiState.distanceUnit == DistanceUnit.METRIC) rail_metricText else rail_imperialText, fillColor = b14G, onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) })
            azDivider()
        }

        azMenuItem(id = "orientation", text = rail_orientationText, fillColor = b15M, onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) })
        azMenuItem(id = "snapping", text = if (uiState.isSnappingEnabled) rail_snapping_onText else rail_snapping_offText, fillColor = b14G, onClick = { onEvent(MainScreenEvent.ToggleSnapping) })
        // Training capture (CaptureRecorder): the setting the consent dialog points to.
        azMenuItem(id = "training_data", text = if (captureEnabled) rail_training_data_onText else rail_training_data_offText, fillColor = b10B, onClick = onToggleCapture)

        // Overlay brightness and the glow-stick lines, for a bright or dark feed. Their dialogs were
        // built and routed but nothing opened them.
        azMenuItem(id = "luminance", text = rail_luminanceText, fillColor = b1Y, onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) })
        azMenuItem(id = "glow_stick", text = rail_glow_stickText, fillColor = b4P, onClick = { onEvent(MainScreenEvent.ToggleGlowStickDialog) })

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(id = "advanced", text = rail_advancedText, fillColor = b1Y, onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) })
        }

        // Debug log (DebugLogRecorder): records this app's logcat so failures can be sent without adb.
        azMenuItem(id = "debug_log", text = if (debugLogEnabled) rail_debug_log_onText else rail_debug_log_offText, fillColor = b10B, onClick = onToggleDebugLog)
        if (debugLogEnabled) {
            azMenuItem(id = "share_debug_log", text = rail_share_debug_logText, fillColor = b10B, onClick = onShareDebugLog)
        }

        // Replaces the old "Billing & License" item, which shipped a tester-
        // license console to every user in every build with no debug or
        // entitlement gate at all. MainViewModel turns ToggleSupportSheet into
        // SingleEvent.ShowSupportSheet; MainActivity owns the sheet.
        azMenuItem(
            id = "support",
            text = supportLabel,
            fillColor = b1Y,
            onClick = { onEvent(MainScreenEvent.ToggleSupportSheet) },
        )

        azDivider()
        azMenuItem(id = "mode", text = railModeFormat, fillColor = b2B, onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) })
    }

    if (confirmCloudSave.value) {
        AlertDialog(
            onDismissRequest = { confirmCloudSave.value = false },
            title = { Text(stringResource(R.string.cloud_save_title)) },
            text = { Text(stringResource(R.string.cloud_save_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloudSave.value = false
                    onEvent(MainScreenEvent.SaveCloudPlacement)
                }) { Text(stringResource(R.string.cloud_save_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloudSave.value = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
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

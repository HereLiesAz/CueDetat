# Tutorial & AR Confidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the blocking Next-button tutorial with an action-gated transparent overlay, and implement `tableOverlayConfidence` so the AR_SETUP → AR_ACTIVE auto-advance actually triggers.

**Architecture:**
Track A (Tutorial): `TutorialReducer` gains a post-dispatch hook in `StateReducer` so it sees ALL events; each tutorial step defines a completing action that fires `NextTutorialStep` automatically; `TutorialOverlay` adds Back/Skip buttons and removes the blocking UX.
Track B (Confidence): `TableGeometryFitter` exposes fit quality; `TableScanViewModel` computes a per-frame confidence score and pushes it to `VisionRepository`; `CvReducer` smooths over a rolling window and auto-advances with normal or degraded-quality path.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, JUnit 4 (plain JVM unit tests — no Robolectric needed)

---

## File Map

**Track A**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/TutorialReducer.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt`
- Create: `app/src/test/java/com/hereliesaz/cuedetat/domain/TutorialReducerTest.kt`

**Track B**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt` (also touched by Track A)
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt`
- Modify: `app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt`
- Modify: `app/src/test/java/com/hereliesaz/cuedetat/domain/ArFlowReducerTest.kt`
- Modify: `docs/ALGORITHMS.md`

---

## TRACK A — Non-Blocking Action-Gated Tutorial

---

### Task A1: Add `TutorialBack` event to the sealed class

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`

- [ ] **Step 1: Add the event**

In `UiModel.kt`, find the tutorial event block (around line 269–272):
```kotlin
    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()
    data class UpdateHighlightAlpha(val alpha: Float) : MainScreenEvent()
```
Replace with:
```kotlin
    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()
    object TutorialBack : MainScreenEvent()
    data class UpdateHighlightAlpha(val alpha: Float) : MainScreenEvent()
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no new errors)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt
git commit -m "feat: add TutorialBack event to MainScreenEvent"
```

---

### Task A2: Rewrite TutorialReducer with action-gated logic

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/TutorialReducer.kt`

- [ ] **Step 1: Write the failing test first** (create the test file)

Create `app/src/test/java/com/hereliesaz/cuedetat/domain/TutorialReducerTest.kt`:
```kotlin
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.reducers.reduceTutorialAction
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement
import org.junit.Assert.*
import org.junit.Test

class TutorialReducerTest {

    private val base = CueDetatState()
    private val active = base.copy(showTutorialOverlay = true, currentTutorialStep = 0)

    // ── Lifecycle events ──────────────────────────────────────────────────────

    @Test
    fun `StartTutorial shows overlay at step 0 with NONE highlight`() {
        val result = reduceTutorialAction(base, MainScreenEvent.StartTutorial)
        assertTrue(result.showTutorialOverlay)
        assertEquals(0, result.currentTutorialStep)
        assertEquals(TutorialHighlightElement.NONE, result.tutorialHighlight)
    }

    @Test
    fun `EndTutorial hides overlay and clears highlight`() {
        val result = reduceTutorialAction(active, MainScreenEvent.EndTutorial)
        assertFalse(result.showTutorialOverlay)
        assertEquals(TutorialHighlightElement.NONE, result.tutorialHighlight)
    }

    @Test
    fun `NextTutorialStep from step 0 goes to step 1 with TARGET_BALL highlight`() {
        val result = reduceTutorialAction(active, MainScreenEvent.NextTutorialStep)
        assertEquals(1, result.currentTutorialStep)
        assertEquals(TutorialHighlightElement.TARGET_BALL, result.tutorialHighlight)
    }

    @Test
    fun `NextTutorialStep from step 1 goes to step 2 with GHOST_BALL highlight`() {
        val s = active.copy(currentTutorialStep = 1)
        val result = reduceTutorialAction(s, MainScreenEvent.NextTutorialStep)
        assertEquals(2, result.currentTutorialStep)
        assertEquals(TutorialHighlightElement.GHOST_BALL, result.tutorialHighlight)
    }

    @Test
    fun `NextTutorialStep from step 6 ends tutorial`() {
        val s = active.copy(currentTutorialStep = 6)
        val result = reduceTutorialAction(s, MainScreenEvent.NextTutorialStep)
        assertFalse(result.showTutorialOverlay)
    }

    // ── Back button ───────────────────────────────────────────────────────────

    @Test
    fun `TutorialBack on step 0 does nothing`() {
        val result = reduceTutorialAction(active.copy(currentTutorialStep = 0), MainScreenEvent.TutorialBack)
        assertEquals(0, result.currentTutorialStep)
        assertTrue(result.showTutorialOverlay)
    }

    @Test
    fun `TutorialBack on step 3 goes to step 2`() {
        val s = active.copy(currentTutorialStep = 3)
        val result = reduceTutorialAction(s, MainScreenEvent.TutorialBack)
        assertEquals(2, result.currentTutorialStep)
        assertEquals(TutorialHighlightElement.GHOST_BALL, result.tutorialHighlight)
    }

    // ── Action-gated steps ────────────────────────────────────────────────────

    @Test
    fun `step 0 completes on any LogicalDragApplied`() {
        val drag = MainScreenEvent.LogicalDragApplied(PointF(0f, 0f), PointF(5f, 5f), Offset.Zero)
        val result = reduceTutorialAction(active.copy(currentTutorialStep = 0), drag)
        assertEquals(1, result.currentTutorialStep)
    }

    @Test
    fun `step 1 completes on gesture starting near target ball`() {
        val center = PointF(100f, 100f)
        val s = active.copy(
            currentTutorialStep = 1,
            protractorUnit = ProtractorUnit(center, LOGICAL_BALL_RADIUS, 0f)
        )
        // Within 2× radius = within 50 logical units
        val gesture = MainScreenEvent.LogicalGestureStarted(PointF(120f, 100f), Offset.Zero)
        val result = reduceTutorialAction(s, gesture)
        assertEquals(2, result.currentTutorialStep)
    }

    @Test
    fun `step 1 does NOT complete on gesture far from target ball`() {
        val center = PointF(100f, 100f)
        val s = active.copy(
            currentTutorialStep = 1,
            protractorUnit = ProtractorUnit(center, LOGICAL_BALL_RADIUS, 0f)
        )
        val gesture = MainScreenEvent.LogicalGestureStarted(PointF(300f, 300f), Offset.Zero)
        val result = reduceTutorialAction(s, gesture)
        assertEquals(1, result.currentTutorialStep)
    }

    @Test
    fun `step 2 completes on TableRotationApplied`() {
        val s = active.copy(currentTutorialStep = 2)
        val result = reduceTutorialAction(s, MainScreenEvent.TableRotationApplied(15f))
        assertEquals(3, result.currentTutorialStep)
    }

    @Test
    fun `step 3 Expert-no-table completes on ToggleTableScanScreen`() {
        val s = active.copy(
            currentTutorialStep = 3,
            experienceMode = ExperienceMode.EXPERT,
            table = base.table.copy(isVisible = false)
        )
        val result = reduceTutorialAction(s, MainScreenEvent.ToggleTableScanScreen)
        assertEquals(4, result.currentTutorialStep)
    }

    @Test
    fun `step 3 non-Expert completes on LogicalDragApplied`() {
        val s = active.copy(
            currentTutorialStep = 3,
            experienceMode = ExperienceMode.BEGINNER
        )
        val drag = MainScreenEvent.LogicalDragApplied(PointF(0f, 0f), PointF(5f, 5f), Offset.Zero)
        val result = reduceTutorialAction(s, drag)
        assertEquals(4, result.currentTutorialStep)
    }

    @Test
    fun `step 4 completes on ZoomSliderChanged`() {
        val s = active.copy(currentTutorialStep = 4)
        val result = reduceTutorialAction(s, MainScreenEvent.ZoomSliderChanged(0.5f))
        assertEquals(5, result.currentTutorialStep)
    }

    @Test
    fun `step 5 completes on ToggleBankingMode`() {
        val s = active.copy(currentTutorialStep = 5)
        val result = reduceTutorialAction(s, MainScreenEvent.ToggleBankingMode)
        assertEquals(6, result.currentTutorialStep)
    }

    @Test
    fun `step 6 does NOT complete on any action event`() {
        val s = active.copy(currentTutorialStep = 6)
        val drag = MainScreenEvent.LogicalDragApplied(PointF(0f, 0f), PointF(5f, 5f), Offset.Zero)
        val result = reduceTutorialAction(s, drag)
        assertEquals(6, result.currentTutorialStep)
    }

    @Test
    fun `action event does not advance step when tutorial is inactive`() {
        val s = base.copy(showTutorialOverlay = false, currentTutorialStep = 2)
        val result = reduceTutorialAction(s, MainScreenEvent.TableRotationApplied(15f))
        assertEquals(2, result.currentTutorialStep)
    }
}
```

- [ ] **Step 2: Run to verify tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.TutorialReducerTest"`
Expected: FAIL — `TutorialBack` doesn't exist yet (caught by compile), or several tests fail

- [ ] **Step 3: Replace TutorialReducer.kt with the new implementation**

```kotlin
package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

private const val TUTORIAL_LAST_STEP = 6

/**
 * Reducer responsible for guiding the user through the interface.
 *
 * Handles two categories of event:
 *  1. Tutorial-specific events (StartTutorial, NextTutorialStep, etc.) — always processed.
 *  2. Any other event — checked against the current step's completing action when tutorial is active.
 *
 * Strictly gates Expert-level tasks (SCAN_TABLE) from non-Expert modes.
 */
internal fun reduceTutorialAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.StartTutorial -> state.copy(
            showTutorialOverlay = true,
            currentTutorialStep = 0,
            tutorialHighlight = TutorialHighlightElement.NONE
        )

        is MainScreenEvent.NextTutorialStep -> advanceTutorialStep(state)

        is MainScreenEvent.TutorialBack -> {
            if (state.currentTutorialStep == 0) state
            else {
                val prevStep = state.currentTutorialStep - 1
                state.copy(
                    currentTutorialStep = prevStep,
                    tutorialHighlight = highlightForStep(prevStep, state)
                )
            }
        }

        is MainScreenEvent.EndTutorial -> state.copy(
            showTutorialOverlay = false,
            tutorialHighlight = TutorialHighlightElement.NONE
        )

        is MainScreenEvent.UpdateHighlightAlpha -> state.copy(highlightAlpha = action.alpha)

        else -> {
            if (state.showTutorialOverlay && isTutorialStepCompleted(state, action)) {
                advanceTutorialStep(state)
            } else {
                state
            }
        }
    }
}

private fun advanceTutorialStep(state: CueDetatState): CueDetatState {
    val nextStep = state.currentTutorialStep + 1
    return if (nextStep > TUTORIAL_LAST_STEP) {
        state.copy(showTutorialOverlay = false, tutorialHighlight = TutorialHighlightElement.NONE)
    } else {
        state.copy(
            currentTutorialStep = nextStep,
            tutorialHighlight = highlightForStep(nextStep, state)
        )
    }
}

private fun highlightForStep(step: Int, state: CueDetatState): TutorialHighlightElement = when (step) {
    0 -> TutorialHighlightElement.NONE
    1 -> TutorialHighlightElement.TARGET_BALL
    2 -> TutorialHighlightElement.GHOST_BALL
    3 -> if (state.experienceMode == ExperienceMode.EXPERT && !state.table.isVisible)
        TutorialHighlightElement.SCAN_TABLE
    else
        TutorialHighlightElement.CUE_BALL
    4 -> TutorialHighlightElement.ZOOM_SLIDER
    else -> TutorialHighlightElement.NONE
}

private fun isTutorialStepCompleted(state: CueDetatState, action: MainScreenEvent): Boolean =
    when (state.currentTutorialStep) {
        0 -> action is MainScreenEvent.LogicalDragApplied
        1 -> action is MainScreenEvent.LogicalGestureStarted &&
                isNearTargetBall(action.logicalPoint, state)
        2 -> action is MainScreenEvent.TableRotationApplied
        3 -> if (state.experienceMode == ExperienceMode.EXPERT && !state.table.isVisible)
            action is MainScreenEvent.ToggleTableScanScreen
        else
            action is MainScreenEvent.LogicalDragApplied
        4 -> action is MainScreenEvent.ZoomSliderChanged
        5 -> action is MainScreenEvent.ToggleBankingMode
        else -> false
    }

private fun isNearTargetBall(point: PointF, state: CueDetatState): Boolean {
    val dx = point.x - state.protractorUnit.center.x
    val dy = point.y - state.protractorUnit.center.y
    return kotlin.math.hypot(dx.toDouble(), dy.toDouble()) <= LOGICAL_BALL_RADIUS * 2.0
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.TutorialReducerTest"`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/TutorialReducer.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/TutorialReducerTest.kt
git commit -m "feat: rewrite TutorialReducer with action-gated step completion and Back support"
```

---

### Task A3: Update StateReducer to add post-dispatch tutorial hook

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt`

- [ ] **Step 1: Add TutorialBack to the onboarding branch and add post-dispatch hook**

In `StateReducer.kt`, find the ONBOARDING block (around line 131–134):
```kotlin
        // --- ONBOARDING ---
        // Handled by [TutorialReducer].
        is MainScreenEvent.StartTutorial, is MainScreenEvent.NextTutorialStep,
        is MainScreenEvent.EndTutorial, is MainScreenEvent.UpdateHighlightAlpha ->
            reduceTutorialAction(currentState, action)
```
Replace with:
```kotlin
        // --- ONBOARDING ---
        // Handled by [TutorialReducer].
        is MainScreenEvent.StartTutorial, is MainScreenEvent.NextTutorialStep,
        is MainScreenEvent.EndTutorial, is MainScreenEvent.TutorialBack,
        is MainScreenEvent.UpdateHighlightAlpha ->
            reduceTutorialAction(currentState, action)
```

Then find the closing of `stateReducer` — the return statement ending in `}`. The current structure is:
```kotlin
fun stateReducer(
    currentState: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils,
    gestureReducer: GestureReducer
): CueDetatState {
    return when (action) {
        // ... all branches ...
        else -> currentState
    }
}
```

Change to:
```kotlin
fun stateReducer(
    currentState: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils,
    gestureReducer: GestureReducer
): CueDetatState {
    val primaryResult = when (action) {
        // ... all branches (unchanged) ...
        else -> currentState
    }
    // Post-dispatch: let TutorialReducer check whether this event completes a tutorial step.
    // Tutorial-specific events are excluded — they were already handled above.
    return if (primaryResult.showTutorialOverlay &&
        action !is MainScreenEvent.StartTutorial &&
        action !is MainScreenEvent.NextTutorialStep &&
        action !is MainScreenEvent.EndTutorial &&
        action !is MainScreenEvent.TutorialBack &&
        action !is MainScreenEvent.UpdateHighlightAlpha) {
        reduceTutorialAction(primaryResult, action)
    } else {
        primaryResult
    }
}
```

The key change is: rename `return when (action)` to `val primaryResult = when (action)`, and add the post-dispatch block at the end.

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all existing tests to verify nothing regressed**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
git commit -m "feat: add post-dispatch tutorial hook to StateReducer for action-gated steps"
```

---

### Task A4: Update TutorialOverlay — add Back button, rename Next→Skip

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt`

- [ ] **Step 1: Replace the button row**

In `TutorialOverlay.kt`, find the button Row (around lines 172–181):
```kotlin
                Row {
                    if (uiState.currentTutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text("Next")
                        }
                    }
                    TextButton(onClick = { onEvent(MainScreenEvent.EndTutorial) }) {
                        Text("Finish")
                    }
                }
```
Replace with:
```kotlin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onEvent(MainScreenEvent.TutorialBack) },
                        enabled = uiState.currentTutorialStep > 0
                    ) {
                        Text("Back")
                    }
                    if (uiState.currentTutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text("Skip")
                        }
                    } else {
                        TextButton(onClick = { onEvent(MainScreenEvent.EndTutorial) }) {
                            Text("Done")
                        }
                    }
                }
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt
git commit -m "feat: replace Next/Finish with Back/Skip/Done in TutorialOverlay"
```

---

## TRACK B — AR Confidence Calculation

---

### Task B1: Add `fitWithQuality` to TableGeometryFitter

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt`

- [ ] **Step 1: Write the failing tests first**

In `app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt`, add after the existing tests:

```kotlin
    // ── fitWithQuality tests ──────────────────────────────────────────────────

    @Test
    fun `fitPtWithQuality returns quality 1 for perfect rectangle`() {
        val result = TableGeometryFitter.fitPtWithQuality(shuffled())
        assertNotNull(result.assignments)
        assertEquals(1.0f, result.quality, 0.01f)
    }

    @Test
    fun `fitPtWithQuality returns quality below 1 for distorted rectangle`() {
        // Move TL inward by 10 units — diagonals will differ
        val distorted = idealPts.values.toMutableList().also {
            val tlIndex = it.indexOf(idealPts[PocketId.TL])
            it[tlIndex] = Pt(idealPts[PocketId.TL]!!.x + 10f, idealPts[PocketId.TL]!!.y + 10f)
        }
        val result = TableGeometryFitter.fitPtWithQuality(distorted)
        assertNotNull(result.assignments)
        assertTrue("Quality should be less than 1 for distorted layout", result.quality < 1.0f)
        assertTrue("Quality should be non-negative", result.quality >= 0f)
    }

    @Test
    fun `fitPtWithQuality returns quality 0 and null assignments for 5 points`() {
        val fivePts = shuffled().take(5)
        val result = TableGeometryFitter.fitPtWithQuality(fivePts)
        assertNull(result.assignments)
        assertEquals(0f, result.quality, 0.001f)
    }

    @Test
    fun `fitPt still works after fitPtWithQuality is added`() {
        val result = TableGeometryFitter.fitPt(shuffled())
        assertNotNull(result)
        assertEquals(6, result!!.size)
    }
```

- [ ] **Step 2: Run to verify new tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.TableGeometryFitterTest"`
Expected: Compilation fails — `fitPtWithQuality` does not exist yet

- [ ] **Step 3: Add FitResult data class and fitPtWithQuality to TableGeometryFitter.kt**

In `TableGeometryFitter.kt`, add inside the `object TableGeometryFitter` block, after the existing constants:

```kotlin
    /** Max acceptable diagonal difference (in logical units) at which fit quality reaches 0. */
    private const val MAX_DIAGONAL_DIFF_LOGICAL = 50f

    /** Return type for [fitPtWithQuality] and [fitWithQuality]. */
    internal data class FitResult(
        val assignments: List<Pair<PocketId, Pt>>?,
        val quality: Float  // 0..1; 1 = perfect rectangle, 0 = unfit or too distorted
    )
```

Add the new method `fitPtWithQuality` inside the object (replace the body of `fitPt` to delegate to it):

```kotlin
    /**
     * Like [fitPt] but also returns a quality score (0..1).
     * Quality = 1 - (diagonal_difference / MAX_DIAGONAL_DIFF_LOGICAL), clamped to [0,1].
     * Quality is 0 when fit fails or fewer than 6 points are provided.
     */
    internal fun fitPtWithQuality(points: List<Pt>): FitResult {
        if (points.size != 6) return FitResult(null, 0f)

        var bestScore = Float.MAX_VALUE
        var bestResult: List<Pair<PocketId, Pt>>? = null

        val indices = points.indices.toList()
        for (i in 0 until 6) for (j in i + 1 until 6) for (k in j + 1 until 6) for (l in k + 1 until 6) {
            val cornerIdx = listOf(i, j, k, l)
            val corners = cornerIdx.map { points[it] }
            val sides = indices.filter { it !in cornerIdx }.map { points[it] }
            val result = tryFitCorners(corners, sides) ?: continue
            val score = rectResidual(result)
            if (score < bestScore) { bestScore = score; bestResult = result }
        }

        if (bestResult == null) return FitResult(null, 0f)

        val diagonalDiff = kotlin.math.sqrt(bestScore.toDouble()).toFloat()
        val quality = (1f - diagonalDiff / MAX_DIAGONAL_DIFF_LOGICAL).coerceIn(0f, 1f)
        return FitResult(bestResult, quality)
    }

    /**
     * Public Android-facing variant of [fitPtWithQuality].
     * Returns a pair of (pocket assignments or null, quality 0..1).
     */
    fun fitWithQuality(points: List<PointF>): Pair<List<Pair<PocketId, PointF>>?, Float> {
        val pts = points.map { Pt(it.x, it.y) }
        val result = fitPtWithQuality(pts)
        return result.assignments?.map { (id, pt) -> id to PointF(pt.x, pt.y) } to result.quality
    }
```

Then replace the body of `fitPt` to delegate:
```kotlin
    internal fun fitPt(points: List<Pt>): List<Pair<PocketId, Pt>>? =
        fitPtWithQuality(points).assignments
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.TableGeometryFitterTest"`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt
git commit -m "feat: add fitWithQuality to TableGeometryFitter returning fit quality 0-1"
```

---

### Task B2: Add `setScanConfidence` to VisionRepository

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt`

- [ ] **Step 1: Add confidence state and setter**

In `VisionRepository.kt`, find the existing `MutableStateFlow` declarations near the top of the class body (around line 75):
```kotlin
    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()
```
After this block, add:
```kotlin
    // Confidence score pushed by TableScanViewModel during AR_SETUP scanning.
    private val _scanConfidence = MutableStateFlow(0f)

    /** Called by TableScanViewModel after each frame during AR_SETUP to report scan progress. */
    fun setScanConfidence(confidence: Float) {
        _scanConfidence.value = confidence
    }
```

- [ ] **Step 2: Use confidence when emitting VisionData**

In `VisionRepository.kt`, find the `finalVisionData` construction (around line 347):
```kotlin
                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                    cvMask = cvMask,
                    sourceImageWidth = inputImage.width,
                    sourceImageHeight = inputImage.height,
                    sourceImageRotation = rotationDegrees
                )
```
Replace with:
```kotlin
                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                    cvMask = cvMask,
                    sourceImageWidth = inputImage.width,
                    sourceImageHeight = inputImage.height,
                    sourceImageRotation = rotationDegrees,
                    tableOverlayConfidence = _scanConfidence.value
                )
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt
git commit -m "feat: add setScanConfidence to VisionRepository, thread confidence into VisionData"
```

---

### Task B3: Add confidence fields to CueDetatState

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`

- [ ] **Step 1: Add the two new @Transient fields**

In `UiModel.kt`, find the `@Transient val warningText` field (around line 146):
```kotlin
    @Transient val warningText: String? = null,
```
After this line, add:
```kotlin
    @Transient val arConfidenceHistory: List<Float> = emptyList(),
    @Transient val arLowConfidenceFrameCount: Int = 0,
```

- [ ] **Step 2: Verify compile and existing tests still pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt
git commit -m "feat: add arConfidenceHistory and arLowConfidenceFrameCount transient fields to state"
```

---

### Task B4: Update TableScanViewModel to inject VisionRepository and compute confidence

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt`

- [ ] **Step 1: Add VisionRepository import and inject it**

Add to imports at the top of `TableScanViewModel.kt`:
```kotlin
import com.hereliesaz.cuedetat.data.VisionRepository
```

Change the constructor from:
```kotlin
@HiltViewModel
class TableScanViewModel @Inject constructor(
    private val tableScanRepository: TableScanRepository,
    val pocketDetector: PocketDetector
) : ViewModel() {
```
To:
```kotlin
@HiltViewModel
class TableScanViewModel @Inject constructor(
    private val tableScanRepository: TableScanRepository,
    val pocketDetector: PocketDetector,
    private val visionRepository: VisionRepository
) : ViewModel() {
```

- [ ] **Step 2: Add computeScanConfidence private function**

Add this private function to `TableScanViewModel`, before `resetScan()`:
```kotlin
    /**
     * Computes a 0..1 confidence score from accumulated cluster data.
     * - Fewer than 6 stable clusters: score scales linearly up to 0.7.
     * - 6 stable clusters: score = 0.7 + fitQuality * 0.3 (range 0.7..1.0).
     * A cluster is "stable" when it has >= MIN_OBSERVATIONS_TO_FIT observations.
     */
    private fun computeScanConfidence(): Float {
        val stableMeans = clusters.values
            .filter { it.size >= MIN_OBSERVATIONS_TO_FIT }
            .map { obs ->
                PointF(
                    obs.sumOf { it.x.toDouble() }.toFloat() / obs.size,
                    obs.sumOf { it.y.toDouble() }.toFloat() / obs.size
                )
            }
        return when {
            stableMeans.size < 6 -> stableMeans.size / 6f * 0.7f
            else -> {
                val (_, fitQuality) = TableGeometryFitter.fitWithQuality(stableMeans)
                0.7f + fitQuality * 0.3f
            }
        }
    }
```

- [ ] **Step 3: Call setScanConfidence after each frame in onFrame()**

In `onFrame()`, find the end of the `viewModelScope.launch(Dispatchers.Default)` block:
```kotlin
            // Step 3: Merge into clusters (accumulated for manual geometry fitting if needed).
            logicalPts.forEach { logicalPt -> mergeIntoCluster(logicalPt) }
        }
    }
```
Replace with:
```kotlin
            // Step 3: Merge into clusters (accumulated for manual geometry fitting if needed).
            logicalPts.forEach { logicalPt -> mergeIntoCluster(logicalPt) }

            // Step 4: Publish scan confidence to VisionRepository for CvReducer smoothing.
            visionRepository.setScanConfidence(computeScanConfidence())
        }
    }
```

- [ ] **Step 4: Reset confidence in resetScan()**

In `resetScan()`, find:
```kotlin
    fun resetScan() {
        clusters.clear()
        _scanProgress.value = emptyMap()
        _scanComplete.value = false
        _capturedFeltHsv.value = null
    }
```
Replace with:
```kotlin
    fun resetScan() {
        clusters.clear()
        _scanProgress.value = emptyMap()
        _scanComplete.value = false
        _capturedFeltHsv.value = null
        visionRepository.setScanConfidence(0f)
    }
```

- [ ] **Step 5: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt
git commit -m "feat: inject VisionRepository into TableScanViewModel, compute and publish scan confidence per frame"
```

---

### Task B5: Update CvReducer with smoothing and degraded advance

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt`

- [ ] **Step 1: Write failing tests for the new logic**

In `ArFlowReducerTest.kt`, add these tests after the existing tests:

```kotlin
    @Test
    fun `CvDataUpdated smooths over rolling window before advancing`() {
        // A single frame at 0.85 should advance (window of 1 still meets threshold)
        val tps = TpsWarpData(srcPoints = listOf(PointF(0f, 0f)), dstPoints = listOf(PointF(1f, 1f)))
        val scan = TableScanModel(emptyList(), tps, com.hereliesaz.cuedetat.view.state.TableSize.EIGHT_FT, listOf(0f, 0f, 0f), null, null)
        val s = base.copy(cameraMode = CameraMode.AR_SETUP, lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f), tableScanModel = scan)
        val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(VisionData(tableOverlayConfidence = 0.85f)))
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
    }

    @Test
    fun `CvDataUpdated resets degraded counter when confidence drops below floor`() {
        val tps = TpsWarpData(srcPoints = listOf(PointF(0f, 0f)), dstPoints = listOf(PointF(1f, 1f)))
        val scan = TableScanModel(emptyList(), tps, com.hereliesaz.cuedetat.view.state.TableSize.EIGHT_FT, listOf(0f, 0f, 0f), null, null)
        val s = base.copy(
            cameraMode = CameraMode.AR_SETUP,
            lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f),
            tableScanModel = scan,
            arLowConfidenceFrameCount = 50  // already had 50 frames at floor
        )
        // Confidence drops below floor (0.5) → counter resets to 0
        val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(VisionData(tableOverlayConfidence = 0.2f)))
        assertEquals(0, result.arLowConfidenceFrameCount)
        assertEquals(CameraMode.AR_SETUP, result.cameraMode)
    }

    @Test
    fun `CvDataUpdated degraded-advances after 150 frames at floor confidence`() {
        val tps = TpsWarpData(srcPoints = listOf(PointF(0f, 0f)), dstPoints = listOf(PointF(1f, 1f)))
        val scan = TableScanModel(emptyList(), tps, com.hereliesaz.cuedetat.view.state.TableSize.EIGHT_FT, listOf(0f, 0f, 0f), null, null)
        val s = base.copy(
            cameraMode = CameraMode.AR_SETUP,
            lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f),
            tableScanModel = scan,
            arLowConfidenceFrameCount = 149  // one away from threshold
        )
        val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(VisionData(tableOverlayConfidence = 0.6f)))
        assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
        assertNotNull(result.warningText)
    }
```

- [ ] **Step 2: Run to verify new tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.ArFlowReducerTest"`
Expected: The three new tests FAIL (degraded counter fields not yet used)

- [ ] **Step 3: Update CvReducer.kt**

Replace the entire `reduceCvAction.kt` content:

```kotlin
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

private const val AR_CONFIDENCE_THRESHOLD = 0.8f
private const val AR_DEGRADED_FLOOR = 0.5f
private const val AR_DEGRADED_FRAME_COUNT = 150
private const val AR_CONFIDENCE_WINDOW = 20

internal fun reduceCvAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.AutoCalibrateCv -> {
            if (state.lockedHsvColor == null) return state
            state.copy(isAutoCalibrating = true)
        }

        is MainScreenEvent.CvDataUpdated -> {
            var nextState = state.copy(visionData = action.visionData)

            if (state.isAutoCalibrating) {
                val detectionCount = action.visionData.detectedBoundingBoxes.size
                if (detectionCount < 1) {
                    val newT1 = (state.cannyThreshold1 - 5f).coerceAtLeast(10f)
                    val newT2 = (state.cannyThreshold2 - 10f).coerceAtLeast(20f)
                    val newHough = (state.houghThreshold - 2).coerceAtLeast(15)
                    nextState = nextState.copy(
                        cannyThreshold1 = newT1,
                        cannyThreshold2 = newT2,
                        houghThreshold = newHough
                    )
                    if (newT1 <= 10f && newT2 <= 20f) {
                        nextState = nextState.copy(
                            isAutoCalibrating = false,
                            warningText = "Auto-calibration failed: Too dark."
                        )
                    }
                } else {
                    nextState = nextState.copy(isAutoCalibrating = false)
                }
            }

            // AR_SETUP confidence smoothing and auto-advance.
            if (nextState.cameraMode == CameraMode.AR_SETUP &&
                nextState.lockedHsvColor != null &&
                nextState.tableScanModel != null) {

                val rawConfidence = action.visionData.tableOverlayConfidence
                val newHistory = (nextState.arConfidenceHistory + rawConfidence)
                    .takeLast(AR_CONFIDENCE_WINDOW)
                val smoothedConfidence = newHistory.average().toFloat()
                nextState = nextState.copy(arConfidenceHistory = newHistory)

                when {
                    smoothedConfidence >= AR_CONFIDENCE_THRESHOLD -> {
                        nextState = nextState.copy(
                            cameraMode = CameraMode.AR_ACTIVE,
                            arConfidenceHistory = emptyList(),
                            arLowConfidenceFrameCount = 0
                        )
                    }
                    smoothedConfidence >= AR_DEGRADED_FLOOR -> {
                        val newCount = nextState.arLowConfidenceFrameCount + 1
                        if (newCount >= AR_DEGRADED_FRAME_COUNT) {
                            nextState = nextState.copy(
                                cameraMode = CameraMode.AR_ACTIVE,
                                warningText = "AR tracking quality is degraded. Realign for better results.",
                                arConfidenceHistory = emptyList(),
                                arLowConfidenceFrameCount = 0
                            )
                        } else {
                            nextState = nextState.copy(arLowConfidenceFrameCount = newCount)
                        }
                    }
                    else -> {
                        // Below floor: reset degraded counter so it must hold continuously.
                        nextState = nextState.copy(arLowConfidenceFrameCount = 0)
                    }
                }
            }

            nextState
        }

        is MainScreenEvent.LockColor -> state.copy(
            lockedHsvColor = action.hsvMean,
            lockedHsvStdDev = action.hsvStdDev
        )
        is MainScreenEvent.LockOrUnlockColor -> if (state.lockedHsvColor != null)
            state.copy(lockedHsvColor = null, lockedHsvStdDev = null)
        else state
        is MainScreenEvent.AddFeltSample -> {
            val newList = state.savedFeltSamples.toMutableList()
            newList.add(com.hereliesaz.cuedetat.domain.FeltSample(hsv = action.hsv))
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.DeleteFeltSamples -> {
            val newList = state.savedFeltSamples.filterNot { it.id in action.ids }
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.MoveFeltSample -> {
            val newList = state.savedFeltSamples.toMutableList()
            if (action.fromIndex in newList.indices && action.toIndex in newList.indices) {
                val item = newList.removeAt(action.fromIndex)
                newList.add(action.toIndex, item)
            }
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.ClearSamplePoint -> state.copy(colorSamplePoint = null)
        else -> state
    }
}
```

- [ ] **Step 4: Run all tests to verify**

Run: `./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.ArFlowReducerTest"`
Expected: All tests PASS (including existing ones + 3 new ones)

- [ ] **Step 5: Run full test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/ArFlowReducerTest.kt
git commit -m "feat: implement AR confidence smoothing and degraded auto-advance in CvReducer"
```

---

### Task B6: Update ALGORITHMS.md

**Files:**
- Modify: `docs/ALGORITHMS.md`

- [ ] **Step 1: Find and replace the AR confidence section**

In `docs/ALGORITHMS.md`, find the section describing `tableOverlayConfidence`. It currently describes confidence as if it's computed but it isn't. Replace or augment it with:

```markdown
### AR Overlay Confidence (`tableOverlayConfidence`)

**Computation (implemented as of 2026-03-28):**

`VisionData.tableOverlayConfidence` (0–1) is set by `TableScanViewModel` during `AR_SETUP` and
forwarded through `VisionRepository` into `VisionData`.

**Per-frame formula (`TableScanViewModel.computeScanConfidence`):**
- Count "stable clusters": pocket clusters with ≥ `MIN_OBSERVATIONS_TO_FIT` (3) accumulated observations.
- If stable cluster count < 6: `confidence = stableCount / 6 × 0.7` (max 0.583 — below advance threshold)
- If stable cluster count == 6: `confidence = 0.7 + fitQuality × 0.3`
  - `fitQuality` from `TableGeometryFitter.fitWithQuality()`: `(1 - diagonalDiff / 50.0).coerceIn(0,1)`
  - `diagonalDiff` = `|diagonal1 - diagonal2|` in logical units (how "square" the detected rectangle is)

**Temporal smoothing (`CvReducer`):**
- Rolling window of last 20 frames: `smoothedConfidence = history.average()`

**Auto-advance logic (`CvReducer`):**
- `smoothedConfidence ≥ 0.8` → advance to `AR_ACTIVE` immediately (normal path)
- `smoothedConfidence ∈ [0.5, 0.8)` continuously for 150 frames (~5s at 30fps) → advance to `AR_ACTIVE`
  with `warningText` set (degraded path)
- `smoothedConfidence < 0.5` → reset degraded frame counter (must hold floor confidence continuously)

**Constants (all in `CvReducer.kt`):**
- `AR_CONFIDENCE_THRESHOLD = 0.8f`
- `AR_DEGRADED_FLOOR = 0.5f`
- `AR_DEGRADED_FRAME_COUNT = 150`
- `AR_CONFIDENCE_WINDOW = 20`
- `MAX_DIAGONAL_DIFF_LOGICAL = 50f` (in `TableGeometryFitter.kt`)
```

- [ ] **Step 2: Commit**

```bash
git add docs/ALGORITHMS.md
git commit -m "docs: update ALGORITHMS.md with accurate AR confidence formula and constants"
```

---

## Final Verification

- [ ] **Run full test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests PASS with no regressions

- [ ] **Run a full debug build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Update TODO.md**

In `docs/TODO.md`, mark items `§1.1` and `§1.2` as completed.

- [ ] **Final commit**

```bash
git add docs/TODO.md
git commit -m "chore: mark tutorial redesign and AR confidence as complete in TODO"
```

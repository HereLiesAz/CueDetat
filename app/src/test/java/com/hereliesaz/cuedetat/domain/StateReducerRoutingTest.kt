package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.data.WearableState
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the root router in [stateReducer] against silently dropping events.
 *
 * History: StartArTracking, ArSurfaceTapped and WearableStateUpdated each had a
 * working branch in a sub-reducer but no branch in [stateReducer], so they fell
 * into its `else -> currentState` and did nothing at runtime while sub-reducer
 * tests stayed green.
 *
 * The source-level test enumerates every `MainScreenEvent.X` that appears as an
 * `is` branch in any sub-reducer file and asserts the router names it too. It
 * reads source text rather than calling the reducers, so its expectation is not
 * derived from the router it checks.
 */
class StateReducerRoutingTest {

    private val utils = ReducerUtils()
    private val gestures = GestureReducer()

    private fun domainDir(): File {
        val rel = "src/main/java/com/hereliesaz/cuedetat/domain"
        return listOf(File(rel), File("app/$rel")).firstOrNull { it.isDirectory }
            ?: error("domain source dir not found from ${File(".").absolutePath}")
    }

    private val eventRef = Regex("""is\s+MainScreenEvent\.([A-Za-z0-9_]+)""")

    @Test
    fun `every event a sub-reducer handles is routed by stateReducer`() {
        val dir = domainDir()
        val router = File(dir, "StateReducer.kt").readText()
        val routed = eventRef.findAll(router).map { it.groupValues[1] }.toSet()

        // SnapReducer is invoked from MainViewModel, not from stateReducer, and
        // pattern-matches no events; every other file under reducers/ is routed.
        val handled = File(dir, "reducers").listFiles { f -> f.extension == "kt" }!!
            .flatMap { f -> eventRef.findAll(f.readText()).map { it.groupValues[1] to f.name } }

        val dropped = handled.filter { (event, _) -> event !in routed }
            .map { (event, file) -> "$event (handled in $file)" }
            .distinct()

        assertTrue("Router silently drops: $dropped", dropped.isEmpty())
        assertTrue("sanity: scan found sub-reducer events", handled.size > 50)
    }

    @Test
    fun `StartArTracking reaches ToggleReducer through the router`() {
        val s = CueDetatState().copy(cameraMode = CameraMode.AR_SETUP, showTableScanScreen = true)
        val out = stateReducer(s, MainScreenEvent.StartArTracking, utils, gestures)
        assertEquals(CameraMode.AR_ACTIVE, out.cameraMode)
        assertFalse(out.showTableScanScreen)
    }

    @Test
    fun `WearableStateUpdated reaches SystemReducer through the router`() {
        val ws = WearableState(isConnected = true, heartRate = 88f)
        val out = stateReducer(CueDetatState(), MainScreenEvent.WearableStateUpdated(ws), utils, gestures)
        assertEquals(ws, out.wearableState)
    }
}

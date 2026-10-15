package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.di.AppModule
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the persisted-state restore crash.
 *
 * Gson instantiates via Unsafe, bypassing the Kotlin constructor's default
 * values and null-checks. Before the InstanceCreator fix, any non-null field
 * absent from an older persisted JSON (a newly-added field, or any @Transient
 * field Gson never writes) deserialized to raw null, and the first .copy() on
 * the restored state threw NPE via checkNotNullParameter (R8-lowered to
 * Object.getClass()).
 */
class CueDetatStateRestoreTest {

    private val gson = AppModule.provideGson()

    @Test
    fun oldJsonMissingNonNullFieldsRestoresWithDefaults() {
        // A minimal, stale persisted snapshot: only a couple of primitive fields,
        // none of the non-null enum/list/@Transient fields present.
        val oldJson = """{"viewWidth":1080,"viewHeight":1920}"""

        val restored = gson.fromJson(oldJson, CueDetatState::class.java)

        // Fields present in the JSON are honored...
        assertEquals(1080, restored.viewWidth)
        assertEquals(1920, restored.viewHeight)
        // ...and every absent non-null field falls back to its Kotlin default
        // instead of being left as null.
        assertEquals(DistanceUnit.IMPERIAL, restored.distanceUnit)
        assertEquals(TargetType.SOLIDS, restored.targetType)
        assertEquals(InteractionMode.NONE, restored.interactionMode)
        assertEquals(BallSelectionPhase.NONE, restored.ballSelectionPhase)
        assertNotNull(restored.table)
        assertNotNull(restored.protractorUnit)
        assertNotNull(restored.obstacleBalls)
        assertNotNull(restored.masseImpactPoints)
    }

    @Test
    fun copyOnRestoredOldStateDoesNotThrow() {
        val oldJson = """{"viewWidth":1080,"viewHeight":1920}"""
        val restored = gson.fromJson(oldJson, CueDetatState::class.java)

        // This is the exact operation that crashed in the field: the first reducer
        // .copy() runs the real constructor and its non-null checks.
        val copied = restored.copy(viewWidth = 720)

        assertEquals(720, copied.viewWidth)
        assertEquals(DistanceUnit.IMPERIAL, copied.distanceUnit)
    }

    @Test
    fun missingTableSizeRestoresDefaultAndRetainsVisibility() {
        val restored = gson.fromJson(
            """{"table":{"isVisible":true},"viewWidth":1080}""",
            CueDetatState::class.java
        )

        assertEquals(8, restored.table.size.feet)
        assertTrue(restored.table.isVisible)
        assertEquals(4, restored.table.corners.size)
        assertEquals(6, restored.table.pockets.size)
        assertEquals(8, restored.copy(viewWidth = 720).table.copy(isVisible = false).size.feet)
    }

    @Test
    fun invalidOrNullTableDoesNotCrashStateRestore() {
        val invalid = gson.fromJson(
            """{"table":{"size":"REMOVED_SIZE","isVisible":true}}""",
            CueDetatState::class.java
        )
        val nullTable = gson.fromJson("""{"table":null}""", CueDetatState::class.java)

        assertEquals(8, invalid.table.size.feet)
        assertTrue(invalid.table.isVisible)
        assertEquals(8, nullTable.table.size.feet)
        assertEquals(8, nullTable.copy(viewWidth = 1080).table.size.feet)
    }

    @Test
    fun persistedSizeSurvivesAndDerivedGeometryIsRebuilt() {
        // Legacy snapshots contain derived geometry, which must be ignored and recomputed.
        val legacyJson = """{"table":{"size":"NINE_FT","isVisible":true,"logicalWidth":0,"corners":null,"pockets":null}}"""
        val restored = gson.fromJson(legacyJson, CueDetatState::class.java)

        assertEquals(9, restored.table.size.feet)
        assertTrue(restored.table.isVisible)
        assertEquals(4, restored.table.corners.size)
        assertEquals(6, restored.table.pockets.size)
        assertTrue(restored.table.logicalWidth > 0f)
        assertTrue(restored.table.logicalHeight > restored.table.logicalWidth)

        val tableJson = com.google.gson.JsonParser.parseString(gson.toJson(restored))
            .asJsonObject.getAsJsonObject("table")
        assertEquals(2, tableJson.size())
        assertEquals("NINE_FT", tableJson.get("size").asString)
        assertTrue(tableJson.get("isVisible").asBoolean)
    }
}

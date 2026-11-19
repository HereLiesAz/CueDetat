package com.hereliesaz.cuedetat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** What a crash report says, and how the breadcrumbs behave. The Android parts (the handler, the exit record) need a device. */
class CrashReportTest {

    @Before
    fun clean() = CrashReport.resetBreadcrumbs()

    private fun report(error: Throwable, crumbs: List<String> = emptyList()) = CrashReport.format(
        timeMillis = 1_763_513_400_000L, // 2025-11-19 00:50:00 UTC
        app = "1.20.0 (110150035)",
        device = "samsung SM-A166B, Android 15 (API 35), arm64-v8a",
        thread = "GLThread 1234",
        error = error,
        breadcrumbs = crumbs,
    )

    @Test
    fun `a report names the app, the device, the thread and the time`() {
        val text = report(IllegalStateException("no session"))
        assertTrue(text, "app:    1.20.0 (110150035)" in text)
        assertTrue(text, "device: samsung SM-A166B, Android 15 (API 35), arm64-v8a" in text)
        assertTrue(text, "thread: GLThread 1234" in text)
        assertTrue(text, "time:   2025-11-19 00:50:00 UTC" in text)
    }

    @Test
    fun `it carries the whole stack trace including the cause chain`() {
        val cause = UnsatisfiedLinkError("libarcore_sdk_c.so not found")
        val text = report(RuntimeException("wrapped", cause))
        assertTrue(text, "java.lang.RuntimeException: wrapped" in text)
        assertTrue(text, "Caused by: java.lang.UnsatisfiedLinkError: libarcore_sdk_c.so not found" in text)
        assertTrue("frames of this test are in the trace", "CrashReportTest" in text)
    }

    @Test
    fun `the breadcrumbs come out in order, and an empty list says so`() {
        val text = report(
            Exception("x"),
            listOf("12:00:00.000 AR: creating the ARCore session", "12:00:00.050 AR: session created"),
        )
        assertTrue(text.indexOf("creating the ARCore session") < text.indexOf("session created"))
        assertTrue("(nothing recorded)" in report(Exception("x")))
    }

    @Test
    fun `breadcrumbs keep only the most recent ones`() {
        repeat(CrashReport.MAX_BREADCRUMBS + 25) { CrashReport.breadcrumb("step $it") }
        val kept = CrashReport.breadcrumbs()
        assertEquals(CrashReport.MAX_BREADCRUMBS, kept.size)
        assertTrue(kept.first().endsWith("step 25"))
        assertTrue(kept.last().endsWith("step ${CrashReport.MAX_BREADCRUMBS + 24}"))
    }

    @Test
    fun `a repeated breadcrumb is noted once, so a loop does not push the rest out`() {
        repeat(500) { CrashReport.breadcrumb("event TapBall") }
        CrashReport.breadcrumb("AR: session created")
        assertEquals(2, CrashReport.breadcrumbs().size)
        // But the same note after something else is a new fact.
        CrashReport.breadcrumb("event TapBall")
        assertEquals(3, CrashReport.breadcrumbs().size)
    }

    @Test
    fun `the exit record names the reason and says when`() {
        val text = CrashReport.formatExit(
            1_763_513_400_000L,
            "CRASH_NATIVE (a native crash: ARCore, OpenCV or the system's own libraries)",
            status = 11, description = "crash", importance = 100,
        )
        assertTrue(text, "reason:      CRASH_NATIVE" in text)
        assertTrue(text, "status:      11" in text)
        assertTrue(text, "2025-11-19 00:50:00 UTC" in text)
        assertFalse("(none)" in text)
        assertTrue("(none)" in CrashReport.formatExit(0L, "ANR", 0, null, 400))
    }
}

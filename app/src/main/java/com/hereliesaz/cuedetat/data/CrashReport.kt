package com.hereliesaz.cuedetat.data

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A crash the user can send without a computer.
 *
 * Android shows a force-close and keeps the reason to itself; `adb logcat` is the usual way to see it
 * and not everyone has one. Two records, both in the debug log's folder (so the same share sheet and
 * `FileProvider` path carry them, see [DebugLogRecorder.shareIntent]):
 *
 *  - **A Java crash**: [install] sets the process's default uncaught-exception handler, which writes
 *    the stack trace, the device, the app version and the last [MAX_BREADCRUMBS] things the app was
 *    doing, then hands the crash on to the handler that was there (so the system's own handling, and
 *    Play's crash reporting, are untouched). The write is guarded: a failure to record never replaces the
 *    crash with another.
 *  - **A native crash or a kill** (a segfault inside ARCore or OpenCV leaves no Java stack): on the
 *    next launch, on Android 11 and later, the system's record of how the last process ended
 *    ([ActivityManager.getHistoricalProcessExitReasons]) is appended, so "native crash", "killed for
 *    memory" or "not responding" is at least named. The native backtrace itself is a protobuf tombstone
 *    that is not written here.
 *
 * The breadcrumbs ([breadcrumb]) are what makes a native crash locatable: the last few events the app
 * handled, so the report says "...AR toggled, module loaded, session created, then it died".
 */
object CrashReport {

    private const val DIR = "debug_log" // must match res/xml/debug_log_paths.xml
    private const val FILE = "last_crash.txt"
    private const val PREFS = "crash_report"
    private const val KEY_LAST_EXIT = "last_exit_timestamp"
    const val MAX_BREADCRUMBS = 60

    private val crumbs = ArrayDeque<String>()
    private var lastCrumb: String? = null

    /** The report file (it may not exist). */
    fun file(context: Context): File = File(File(context.cacheDir, DIR).apply { mkdirs() }, FILE)

    /** Whether there is a crash report to send. */
    fun exists(context: Context): Boolean = file(context).let { it.exists() && it.length() > 0 }

    /** Deletes the report. */
    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    /**
     * Notes what the app is doing now. Cheap, thread-safe, and ignores a repeat of the previous note,
     * so a loop does not push everything else out of the last [MAX_BREADCRUMBS].
     */
    fun breadcrumb(note: String) {
        synchronized(crumbs) {
            if (note == lastCrumb) return
            lastCrumb = note
            crumbs.addLast("${clock(System.currentTimeMillis())} $note")
            while (crumbs.size > MAX_BREADCRUMBS) crumbs.removeFirst()
        }
    }

    internal fun breadcrumbs(): List<String> = synchronized(crumbs) { crumbs.toList() }

    internal fun resetBreadcrumbs() = synchronized(crumbs) { crumbs.clear(); lastCrumb = null }

    /**
     * Installs the crash handler and, on Android 11 and later, records how the previous process
     * ended. Call first thing in `Application.onCreate`.
     */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                write(file(app), format(
                    timeMillis = System.currentTimeMillis(),
                    app = appLine(app),
                    device = deviceLine(),
                    thread = thread.name,
                    error = error,
                    breadcrumbs = breadcrumbs(),
                ))
            } catch (_: Throwable) {
                // Recording must never replace the crash with another one.
            }
            previous?.uncaughtException(thread, error)
        }
        runCatching { recordPreviousExit(app) }
    }

    /** What the system says about the last time this app's process died, if that was not a clean exit. */
    private fun recordPreviousExit(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val last = manager.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (last.timestamp <= prefs.getLong(KEY_LAST_EXIT, 0L)) return
        prefs.edit().putLong(KEY_LAST_EXIT, last.timestamp).apply()
        val reason = exitReasonName(last.reason) ?: return
        append(file(context), formatExit(last.timestamp, reason, last.status, last.description, last.importance))
    }

    private fun exitReasonName(reason: Int): String? = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH (Java)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE (a native crash: ARCore, OpenCV or the system's own libraries)"
        ApplicationExitInfo.REASON_ANR -> "ANR (not responding)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY (killed for memory)"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED (killed by a signal)"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
        else -> null // user-requested, self-exit, permission change: not a failure
    }

    private fun appLine(context: Context): String = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    }.getOrDefault("unknown")

    private fun deviceLine(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.SUPPORTED_ABIS.joinToString()}"

    private fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private fun append(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.appendText(text)
    }

    private fun clock(millis: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))

    private fun stamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))

    /** The text of a Java crash report. Pure: the caller supplies every fact. */
    internal fun format(
        timeMillis: Long,
        app: String,
        device: String,
        thread: String,
        error: Throwable,
        breadcrumbs: List<String>,
    ): String = buildString {
        appendLine("Cue D'etat crash report")
        appendLine("time:   ${stamp(timeMillis)}")
        appendLine("app:    $app")
        appendLine("device: $device")
        appendLine("thread: $thread")
        appendLine()
        appendLine("exception:")
        appendLine(StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString().trimEnd())
        appendLine()
        appendLine("what the app was doing, oldest first (UTC):")
        if (breadcrumbs.isEmpty()) appendLine("  (nothing recorded)") else breadcrumbs.forEach { appendLine("  $it") }
        appendLine()
    }

    /** The text for how the previous process ended. Pure. */
    internal fun formatExit(timeMillis: Long, reason: String, status: Int, description: String?, importance: Int): String =
        buildString {
            appendLine("previous process exit (system record):")
            appendLine("  time:        ${stamp(timeMillis)}")
            appendLine("  reason:      $reason")
            appendLine("  status:      $status")
            appendLine("  description: ${description ?: "(none)"}")
            appendLine("  importance:  $importance (100 = was in the foreground)")
            appendLine()
        }
}

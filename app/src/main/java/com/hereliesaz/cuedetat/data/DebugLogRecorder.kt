package com.hereliesaz.cuedetat.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "Debug log" setting: while on, the app's own logcat is written continuously to a small
 * rotating file in its cache, so a failure (e.g. Expert AR refusing to load, tag `ExpertAR`) is
 * captured without a computer or adb. "Share debug log" hands the file to any app.
 *
 * Android lets an app read only its own process's log lines, so nothing from other apps is
 * recorded. The setting survives restarts; recording resumes on the next launch.
 */
@Singleton
class DebugLogRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("debug_log", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var process: Process? = null

    private val dir: File get() = File(context.cacheDir, DIR).apply { mkdirs() }
    private val logFile: File get() = File(dir, FILE)

    /** Starts recording if the setting is on. Call once at startup. */
    @Synchronized
    fun resumeIfEnabled() {
        if (_enabled.value) start()
    }

    @Synchronized
    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, on).apply()
        _enabled.value = on
        if (on) start() else stop()
    }

    private fun start() {
        if (process?.isAlive == true) return
        process = runCatching {
            // -r: rotate at ROTATE_KB, -n: keep ROTATE_COUNT old files. threadtime has timestamps,
            // PIDs and tags — what a stack trace needs to be read in context.
            ProcessBuilder(
                "logcat", "-v", "threadtime",
                "-f", logFile.absolutePath,
                "-r", ROTATE_KB.toString(), "-n", ROTATE_COUNT.toString(),
            ).redirectErrorStream(true).start()
        }.onFailure { Log.e(TAG, "Could not start debug log", it) }.getOrNull()
        Log.i(TAG, "Debug log recording to ${logFile.absolutePath}")
    }

    private fun stop() {
        process?.destroy()
        process = null
    }

    /**
     * A share-sheet intent carrying the log (current file plus rotated ones, oldest first, joined
     * into one), or null when nothing has been recorded yet.
     */
    @Synchronized
    fun shareIntent(): Intent? {
        val parts = (ROTATE_COUNT downTo 1).map { File(dir, "$FILE.$it") } + logFile
        val existing = parts.filter { it.exists() && it.length() > 0 }
        if (existing.isEmpty()) return null
        val out = File(dir, SHARE_FILE)
        out.outputStream().use { sink -> existing.forEach { f -> f.inputStream().use { it.copyTo(sink) } } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.debuglog", out)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cue D'etat debug log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share debug log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private companion object {
        const val TAG = "DebugLog"
        const val KEY_ENABLED = "enabled"
        /** Must match res/xml/debug_log_paths.xml. */
        const val DIR = "debug_log"
        const val FILE = "logcat.txt"
        const val SHARE_FILE = "cuedetat-debug-log.txt"
        const val ROTATE_KB = 1024
        const val ROTATE_COUNT = 2
    }
}

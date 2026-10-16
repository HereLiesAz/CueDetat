package com.hereliesaz.cuedetat.utils

import androidx.core.util.AtomicFile
import java.io.File

/**
 * Replaces this file's contents with [text] atomically: the new contents go to a scratch
 * file that replaces the original only once fully written and synced ([AtomicFile]). A crash
 * or kill mid-write leaves the previous contents intact instead of a truncated file.
 *
 * @throws java.io.IOException if the write fails; the original file is left untouched.
 */
fun File.writeTextAtomically(text: String) {
    val atomic = AtomicFile(this)
    val out = atomic.startWrite()
    try {
        out.write(text.toByteArray(Charsets.UTF_8))
        atomic.finishWrite(out)
    } catch (e: Throwable) {
        atomic.failWrite(out)
        throw e
    }
}

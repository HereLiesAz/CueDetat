// app/src/main/java/com/hereliesaz/cuedetatlite/util/SingleEvent.kt
package com.hereliesaz.cuedetatlite.utils

/**
 * Used as a wrapper for data that is exposed via a LiveData or StateFlow that represents an event.
 * This prevents an event from being consumed more than once, which is a common issue on
 * configuration changes (like screen rotation).
 */
class SingleEvent<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}

package com.hereliesaz.cuedetat.domain

/**
 * What to tell the user when AR tracking or the saved-placement restore doesn't work. Keyed by
 * the ARCore enum constant's *name*, so this stays pure Kotlin and a constant a newer ARCore adds
 * falls through to a generic line instead of failing to compile.
 */
object ArTrackingMessages {

    /** Why camera tracking stopped (`TrackingFailureReason`), as advice. Null reason: no advice. */
    fun trackingLost(reasonName: String?): String = when (reasonName) {
        "INSUFFICIENT_LIGHT" -> "Tracking lost: too dark. Add light, or the table won't hold."
        "EXCESSIVE_MOTION" -> "Tracking lost: moving too fast. Slow down."
        "INSUFFICIENT_FEATURES" -> "Tracking lost: not enough to see. Look at the table's edges and floor, not only the felt."
        "CAMERA_UNAVAILABLE" -> "Tracking lost: another app has the camera."
        else -> "Tracking lost. Hold still and look around the table."
    }

    /** `CloudAnchorState` names worth trying a resolve again for. */
    val retryableResolveStates: Set<String> = setOf(
        "ERROR_RESOLVING_LOCALIZATION_NO_MATCH",
        "ERROR_SERVICE_UNAVAILABLE",
        "ERROR_INTERNAL",
    )

    /** The final word when a restore has failed for good. */
    fun resolveFailed(stateName: String): String = when (stateName) {
        "ERROR_CLOUD_ID_NOT_FOUND" ->
            "The saved placement has expired or was removed. Lock the table and save again."
        "ERROR_RESOLVING_LOCALIZATION_NO_MATCH" ->
            "Couldn't recognise the table from here. Scan it from the sides you saved it from, then try again."
        "ERROR_NOT_AUTHORIZED" ->
            "Cloud AR isn't authorised for this app. Check the API key."
        "ERROR_SERVICE_UNAVAILABLE", "ERROR_INTERNAL", "ERROR_HOSTING_SERVICE_UNAVAILABLE" ->
            "Google's AR service didn't answer. Check the connection and try again."
        "ERROR_RESOURCE_EXHAUSTED" ->
            "Cloud AR quota reached. Try again later."
        "ERROR_RESOLVING_SDK_VERSION_TOO_OLD" -> "Update the app: the saved placement is newer than it understands."
        "ERROR_RESOLVING_SDK_VERSION_TOO_NEW" -> "The saved placement is from an older version. Lock and save again."
        else -> "Could not recognise the saved placement ($stateName)."
    }

    /** While a restore keeps trying. */
    const val RESTORE_STILL_LOOKING =
        "Not recognised yet. Keep scanning the table slowly from different sides; still looking."
}

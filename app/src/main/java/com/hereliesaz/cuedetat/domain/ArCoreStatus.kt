package com.hereliesaz.cuedetat.domain

/**
 * What a device can do about ARCore, i.e. the "Google Play Services for AR" app. Pure Kotlin: the
 * ARCore SDK's own `Availability` and `InstallStatus` stay inside `feature_expert_ar/`.
 */
enum class ArCoreStatus {
    /** Installed and current: a Session can be created. */
    READY,

    /** The device supports ARCore but Google Play Services for AR is not installed. Installable. */
    NOT_INSTALLED,

    /** Installed, but older than this SDK needs. Updatable. */
    TOO_OLD,

    /** The device cannot run ARCore at all. */
    UNSUPPORTED,

    /** Not determined: the AR module is not loaded, or the query failed or is still pending. */
    UNKNOWN;

    /** True when asking Google Play to install or update the app could make [READY] possible. */
    val needsInstall: Boolean get() = this == NOT_INSTALLED || this == TOO_OLD
}

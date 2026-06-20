// FILE: app/src/main/java/com/hereliesaz/cuedetat/delivery/ArFeatureDelivery.kt

package com.hereliesaz.cuedetat.delivery

/**
 * Flavor-specific delivery of the `:feature_expert_ar` dynamic feature module
 * (the Expert-only ARCore table-scan flow + ARCore itself).
 *
 *  - **foss**: the module's sources are compiled directly into the APK (the foss
 *    flavor adds the module's java srcDir and ARCore), so the code is always
 *    present and there is nothing to install.
 *  - **play**: the base AAB ships *without* the AR code or ARCore. The split is
 *    delivered on demand via Play Feature Delivery the first time an entitled
 *    user enters Expert AR.
 *
 * Only the install concern lives here; the base resolves the implementation
 * class reflectively (see ArControllerFacade) so the Play-only SplitInstall APIs
 * never leak into the foss build.
 */
interface ArFeatureDelivery {

    /** True when the AR code is already loadable in this process. Always true for foss. */
    val isInstalled: Boolean get() = true

    /**
     * Ensures the AR split is installed (and its classes loadable), requesting it
     * on demand if necessary. Suspends until install completes (play) or returns
     * immediately (foss). Returns true when the code is available afterwards.
     */
    suspend fun ensureInstalled(): Boolean = true
}

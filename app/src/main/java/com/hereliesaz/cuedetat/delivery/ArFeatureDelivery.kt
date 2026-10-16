// FILE: app/src/main/java/com/hereliesaz/cuedetat/delivery/ArFeatureDelivery.kt

package com.hereliesaz.cuedetat.delivery

/**
 * Delivery seam for the Expert-AR code (the ARCore table-scan flow). There is one build and
 * no product flavors: feature_expert_ar's sources are compiled into the app
 * (app/build.gradle.kts adds its folder to the main source set), and the only binding is the
 * no-op [BundledArFeatureDelivery]. The interface remains so an on-demand delivery could be
 * reintroduced without touching ArControllerFacade.
 */
interface ArFeatureDelivery {

    /** True when the AR code is already loadable in this process. Always true in the bundled build. */
    val isInstalled: Boolean get() = true

    /**
     * Ensures the AR code is loadable. Returns true when it is available afterwards; the
     * bundled build returns true immediately.
     */
    suspend fun ensureInstalled(): Boolean = true
}

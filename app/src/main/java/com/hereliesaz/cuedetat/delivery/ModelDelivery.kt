// FILE: app/src/main/java/com/hereliesaz/cuedetat/delivery/ModelDelivery.kt

package com.hereliesaz.cuedetat.delivery

import android.content.Context

/**
 * Delivery seam for the TFLite "master" model. There is one build and no product flavors:
 * the model ships in feature_mlmodel's assets folder, which app/build.gradle.kts adds to the
 * main source set, and the only binding is the no-op [BundledModelDelivery].
 * MergedTFLiteDetector depends only on this interface, so an on-demand delivery could be
 * reintroduced without touching it.
 */
interface ModelDelivery {

    /**
     * True when the model assets are already present in this process and can be
     * opened from [assetContext] without any further install step. Always true in the
     * bundled build.
     */
    val isModelInstalled: Boolean get() = true

    /**
     * Ensures the model assets are available. Returns true when they are afterwards; the
     * bundled build returns true immediately.
     */
    suspend fun ensureInstalled(): Boolean = true

    /**
     * Returns the [Context] whose AssetManager can open the model: [base] in the bundled
     * build.
     */
    fun assetContext(base: Context): Context = base
}

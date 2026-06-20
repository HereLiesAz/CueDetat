// FILE: app/src/main/java/com/hereliesaz/cuedetat/delivery/ModelDelivery.kt

package com.hereliesaz.cuedetat.delivery

import android.content.Context

/**
 * Flavor-specific delivery of the TFLite "master" model that lives in the
 * `:feature_mlmodel` dynamic feature module.
 *
 *  - **foss**: the model is bundled directly in the APK (the foss flavor adds
 *    the feature module's assets to its own source set), so it is always
 *    immediately available and there is nothing to install.
 *  - **play**: the base AAB ships *without* the model. It is delivered on
 *    demand via Play Feature Delivery the first time detection is needed.
 *
 * The base [MergedTFLiteDetector] depends only on this interface so the
 * Play-only SplitInstall APIs never leak into the foss build.
 */
interface ModelDelivery {

    /**
     * True when the model assets are already present in this process and can be
     * opened from [assetContext] without any further install step. Always true
     * for foss; for play it reflects whether the on-demand split is installed.
     */
    val isModelInstalled: Boolean get() = true

    /**
     * Ensures the model assets are installed, requesting the on-demand split if
     * necessary. Suspends until install completes (play) or returns immediately
     * (foss). Returns true when the model is available afterwards.
     */
    suspend fun ensureInstalled(): Boolean = true

    /**
     * Returns the [Context] whose AssetManager can open the model. For foss this
     * is just [base]; for play, after an on-demand install, it is a
     * SplitCompat-refreshed context that can see the freshly installed split.
     */
    fun assetContext(base: Context): Context = base
}

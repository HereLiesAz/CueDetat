// FILE: app/src/main/java/com/hereliesaz/cuedetat/delivery/ModelDelivery.kt

package com.hereliesaz.cuedetat.delivery

import android.content.Context

/**
 * Model-asset boundary retained for detector API stability.
 *
 * The sole Android variant bundles the master TFLite package directly, so the
 * default implementation is immediately ready and needs no runtime delivery.
 */
interface ModelDelivery {
    val isModelInstalled: Boolean get() = true
    suspend fun ensureInstalled(): Boolean = true
    fun assetContext(base: Context): Context = base
}

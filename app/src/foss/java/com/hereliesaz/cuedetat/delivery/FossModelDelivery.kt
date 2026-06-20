// FILE: app/src/foss/java/com/hereliesaz/cuedetat/delivery/FossModelDelivery.kt

package com.hereliesaz.cuedetat.delivery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS-flavor [ModelDelivery]: a no-op. The foss APK bundles the TFLite master
 * model directly (its source set includes the feature module's assets), so the
 * model is always present and every default in [ModelDelivery] applies.
 */
@Singleton
class FossModelDelivery @Inject constructor() : ModelDelivery

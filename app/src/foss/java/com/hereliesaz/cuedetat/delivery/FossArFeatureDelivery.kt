// FILE: app/src/foss/java/com/hereliesaz/cuedetat/delivery/FossArFeatureDelivery.kt

package com.hereliesaz.cuedetat.delivery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS-flavor [ArFeatureDelivery]: a no-op. The foss APK compiles the
 * `:feature_expert_ar` sources directly in (its source set includes the module's
 * java srcDir and ARCore), so `ArControllerImpl` is always loadable and every
 * default in [ArFeatureDelivery] applies.
 */
@Singleton
class FossArFeatureDelivery @Inject constructor() : ArFeatureDelivery

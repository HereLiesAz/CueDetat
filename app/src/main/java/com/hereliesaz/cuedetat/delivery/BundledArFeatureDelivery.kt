package com.hereliesaz.cuedetat.delivery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ArFeatureDelivery] for the single build: a no-op. The Expert-AR sources and ARCore are
 * compiled into the app (feature_expert_ar's java folder is part of the main source set), so
 * `ArControllerImpl` is always loadable and every default in [ArFeatureDelivery] applies.
 */
@Singleton
class BundledArFeatureDelivery @Inject constructor() : ArFeatureDelivery

package com.hereliesaz.cuedetat.delivery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ModelDelivery] for the single build: a no-op. The TFLite master model is compiled into the
 * app (app/build.gradle.kts adds feature_mlmodel's assets to the main source set), so it is
 * always present and every default in [ModelDelivery] applies.
 */
@Singleton
class BundledModelDelivery @Inject constructor() : ModelDelivery

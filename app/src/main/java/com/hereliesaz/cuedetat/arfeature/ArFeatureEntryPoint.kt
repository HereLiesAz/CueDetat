package com.hereliesaz.cuedetat.arfeature

import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.ui.composables.tablescan.PocketDetector
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point exposing the base singletons that the on-demand
 * `:feature_expert_ar` module needs. Hilt does not extend into dynamic feature
 * modules, so `ArControllerImpl` (in the module) pulls these via
 * `EntryPointAccessors.fromApplication(context, ArFeatureEntryPoint::class.java)`
 * instead of constructor injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ArFeatureEntryPoint {
    fun visionRepository(): VisionRepository
    fun tableScanRepository(): TableScanRepository
    fun pocketDetector(): PocketDetector
}

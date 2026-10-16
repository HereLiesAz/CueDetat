package com.hereliesaz.cuedetat.arfeature

import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.ui.composables.tablescan.PocketDetector
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point exposing the singletons `ArControllerImpl` needs. That class is
 * compiled into :app from `feature_expert_ar/` but is constructed by hand by
 * ArControllerFacade (not by Hilt), so it pulls these via
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

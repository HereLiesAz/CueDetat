// FILE: app/src/main/java/com/hereliesaz/cuedetat/di/ArFeatureModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.arfeature.BaseArController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the in-base [ArController]. Step 2 of the Expert-AR extraction swaps this
 * for a provider that resolves the implementation from the on-demand
 * `:feature_expert_ar` dynamic feature module (entitlement-gated SplitInstall).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ArFeatureModule {

    @Binds
    @Singleton
    abstract fun bindArController(impl: BaseArController): ArController
}

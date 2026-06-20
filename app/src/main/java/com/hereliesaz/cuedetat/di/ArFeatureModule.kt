// FILE: app/src/main/java/com/hereliesaz/cuedetat/di/ArFeatureModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.arfeature.ArControllerFacade
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the base [ArController] to the [ArControllerFacade], which loads the real
 * implementation from the on-demand `:feature_expert_ar` dynamic feature module
 * via reflection once an entitled user requests it (see ArControllerFacade).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ArFeatureModule {

    @Binds
    @Singleton
    abstract fun bindArController(impl: ArControllerFacade): ArController
}
